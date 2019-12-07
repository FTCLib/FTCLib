/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.ftccommon.configuration;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ReadXMLFileHandler;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.RobocolDatagram;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link ConfigureFromTemplateActivity} allows one to add a configuration to your
 * robot by instantiating from a list of templates.
 */
public class ConfigureFromTemplateActivity extends EditActivity implements RecvLoopRunnable.RecvLoopCallback
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final RequestCode requestCode = RequestCode.CONFIG_FROM_TEMPLATE;
    public static final String      TAG = "ConfigFromTemplate";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    protected NetworkConnectionHandler  networkConnectionHandler    = NetworkConnectionHandler.getInstance();
    protected List<RobotConfigFile>     configurationList           = new CopyOnWriteArrayList<RobotConfigFile>();
    protected List<RobotConfigFile>     templateList                = new CopyOnWriteArrayList<RobotConfigFile>();
    protected USBScanManager            usbScanManager;
    protected ViewGroup                 feedbackAnchor;
    protected Map<String,String>        remoteTemplates             = new ConcurrentHashMap<String,String>();
    protected final Deque<StringProcessor> receivedConfigProcessors = new LinkedList<StringProcessor>();

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override public void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_from_template);

        EditParameters parameters = EditParameters.fromIntent(this, getIntent());
        deserialize(parameters);

        if (remoteConfigure)
            {
            networkConnectionHandler.pushReceiveLoopCallback(this);
            }

        try
            {
            usbScanManager = new USBScanManager(context, remoteConfigure);
            this.usbScanManager.startExecutorService();
            this.usbScanManager.startDeviceScanIfNecessary();
            }
        catch (RobotCoreException e)
            {
            appUtil.showToast(UILocation.ONLY_LOCAL, getString(R.string.templateConfigureFailedToOpenUSBScanManager));
            RobotLog.ee(TAG, e, "Failed to open usb scan manager: " + e.toString());
            }

        this.feedbackAnchor = (ViewGroup)findViewById(R.id.feedbackAnchor);
        }

    @Override
    protected void onStart()
        {
        super.onStart();
        this.robotConfigFileManager.updateActiveConfigHeader(this.currentCfgFile);

        if (!remoteConfigure)
            {
            configurationList = robotConfigFileManager.getXMLFiles();
            templateList = robotConfigFileManager.getXMLTemplates();
            warnIfNoTemplates();
            }
        else
            {
            // Ask the RC to send us (the DS) the list of extant configs and templates. Do the configurations
            // first before the templates so that they'll arrive before the user has anything to click on.
            networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_CONFIGURATIONS));
            networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_CONFIGURATION_TEMPLATES));
            }
        populate();
        }

    // RC has informed DS of the list of configurations
    protected CallbackResult handleCommandRequestConfigurationsResp(String extra) throws RobotCoreException
        {
        configurationList = robotConfigFileManager.deserializeXMLConfigList(extra);
        return CallbackResult.HANDLED;
        }

    // RC has informed DS of the list of configuration templates
    protected CallbackResult handleCommandRequestTemplatesResp(String extra) throws RobotCoreException
        {
        templateList = robotConfigFileManager.deserializeXMLConfigList(extra);
        warnIfNoTemplates();
        populate();
        return CallbackResult.HANDLED;
        }

    @Override
    protected void onDestroy()
        {
        super.onDestroy();
        this.usbScanManager.stopExecutorService();
        this.usbScanManager = null;
        if (remoteConfigure)
            {
            networkConnectionHandler.removeReceiveLoopCallback(this);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Template list management
    //----------------------------------------------------------------------------------------------

    protected void warnIfNoTemplates()
        {
        if (templateList.size() == 0)
            {
            feedbackAnchor.setVisibility(View.INVISIBLE);
            final String msg0 = getString(R.string.noTemplatesFoundTitle);
            final String msg1 = getString(R.string.noTemplatesFoundMessage);
            runOnUiThread(new Runnable()
                {
                @Override
                public void run()
                    {
                    utility.setFeedbackText(msg0, msg1, R.id.feedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1);
                    }
                });
            }
        else
            {
            runOnUiThread(new Runnable()
                {
                @Override
                public void run()
                    {
                    feedbackAnchor.removeAllViews();
                    feedbackAnchor.setVisibility(View.GONE);
                    }
                });
            }
        }

    protected void populate()
        {
        runOnUiThread(new Runnable()
            {
            @Override
            public void run()
                {
                ViewGroup parent = (ViewGroup) findViewById(R.id.templateList);
                parent.removeAllViews();

                final Collator coll = Collator.getInstance();
                coll.setStrength(Collator.PRIMARY); // use case-insensitive compare
                Collections.sort(templateList, new Comparator<RobotConfigFile>()
                    {
                    @Override public int compare(RobotConfigFile lhs, RobotConfigFile rhs)
                        {
                        return coll.compare(lhs.getName(), rhs.getName());
                        }
                    });

                for (RobotConfigFile template : templateList)
                    {
                    View child = LayoutInflater.from(context).inflate(R.layout.template_info, null);
                    parent.addView(child);

                    TextView name = (TextView) child.findViewById(R.id.templateNameText);
                    name.setText(template.getName());
                    name.setTag(template);
                    }
                }
            });
        }

    public void onConfigureButtonPressed(View v)
        {
        RobotConfigFile templateMeta = getTemplateMeta(v);
        getTemplateAndThen(templateMeta, new TemplateProcessor()
            {
            @Override public void processTemplate(RobotConfigFile templateMeta, XmlPullParser xmlPullParser)
                {
                configureFromTemplate(templateMeta, xmlPullParser);
                }
            });
        }

    void configureFromTemplate(RobotConfigFile templateMeta, XmlPullParser xmlPullParser)
        {
        try {
            RobotConfigMap robotConfigMap = instantiateTemplate(templateMeta, xmlPullParser);
            awaitScannedDevices();
            //
            Class clazz = FtcConfigurationActivity.class;
            EditParameters parameters = new EditParameters(this);
            parameters.setRobotConfigMap(robotConfigMap);
            parameters.setExtantRobotConfigurations(configurationList);
            parameters.setScannedDevices(scannedDevices);
            Intent intent = new Intent(context, clazz);
            parameters.putIntent(intent);
            //
            // Start with an unnamed config, but don't update the header here as that's just distracting
            // as it just shows briefly before FtcConfigurationActivity takes over.
            robotConfigFileManager.setActiveConfig(RobotConfigFile.noConfig(robotConfigFileManager));
            startActivityForResult(intent, FtcConfigurationActivity.requestCode.value);
            }
        catch (RobotCoreException e)
            {
            }
        }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
        {
        if (requestCode==FtcConfigurationActivity.requestCode.value)
            {
            // FtcConfigurationActivity can change the current config. Update our local copy.
            this.currentCfgFile = robotConfigFileManager.getActiveConfigAndUpdateUI();
            }
        }

    public void onInfoButtonPressed(View v)
        {
        RobotConfigFile templateMeta = getTemplateMeta(v);
        getTemplateAndThen(templateMeta, new TemplateProcessor()
            {
            @Override public void processTemplate(RobotConfigFile templateMeta, XmlPullParser xmlPullParser)
                {
                showInfo(templateMeta, xmlPullParser);
                }
            });
        }

    protected void showInfo(RobotConfigFile template, XmlPullParser xmlPullParser)
        {
        String description   = indent(3, robotConfigFileManager.getRobotConfigDescription(xmlPullParser));
        final String title   = getString(R.string.templateConfigureConfigurationInstructionsTitle);
        final String message = String.format(getString(R.string.templateConfigurationInstructions), template.getName(), description);

        runOnUiThread(new Runnable()
            {
            @Override
            public void run()
                {
                utility.setFeedbackText(title, message.trim(), R.id.feedbackAnchor, R.layout.feedback, R.id.feedbackText0, R.id.feedbackText1, R.id.feedbackOKButton);
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Remote templates
    //----------------------------------------------------------------------------------------------

    protected interface TemplateProcessor
        {
        void processTemplate(RobotConfigFile templateMeta, XmlPullParser xmlPullParser);
        }

    protected interface StringProcessor
        {
        void processString(String string);
        }

    protected void getTemplateAndThen(final RobotConfigFile templateMeta, final TemplateProcessor processor)
        {
        if (remoteConfigure)
            {
            // Look in cache if we have it, otherwise ask for it from RC
            String template = remoteTemplates.get(templateMeta.getName());
            if (template != null)
                {
                XmlPullParser xmlPullParser = xmlPullParserFromString(template);
                processor.processTemplate(templateMeta, xmlPullParser);
                }
            else
                {
                synchronized (receivedConfigProcessors)
                    {
                    receivedConfigProcessors.addLast(new StringProcessor()
                        {
                        @Override public void processString(String template)
                            {
                            // Remember it in the cache for next time
                            remoteTemplates.put(templateMeta.getName(), template);
                            // Process it now
                            XmlPullParser xmlPullParser = xmlPullParserFromString(template);
                            processor.processTemplate(templateMeta, xmlPullParser);
                            }
                        });
                    networkConnectionHandler.sendCommand(new Command(CommandList.CMD_REQUEST_PARTICULAR_CONFIGURATION, templateMeta.toString()));
                    }
                }
            }
        else
            {
            processor.processTemplate(templateMeta, templateMeta.getXml());
            }
        }

    protected CallbackResult handleCommandRequestParticularConfigurationResp(String config) throws RobotCoreException
        {
        StringProcessor processor = null;
        synchronized (receivedConfigProcessors)
            {
            processor = receivedConfigProcessors.pollFirst();
            }
        if (processor != null)
            {
            processor.processString(config);
            }
        return CallbackResult.HANDLED;
        }

    protected XmlPullParser xmlPullParserFromString(String string)
        {
        return ReadXMLFileHandler.xmlPullParserFromReader(new StringReader(string));
        }

    protected RobotConfigFile getTemplateMeta(View v)
        {
        ViewGroup viewGroup = (ViewGroup) v.getParent();
        TextView name = (TextView)viewGroup.findViewById(R.id.templateNameText);
        return (RobotConfigFile) name.getTag();
        }

    //----------------------------------------------------------------------------------------------
    // Template management
    //----------------------------------------------------------------------------------------------

    protected ScannedDevices awaitScannedDevices()
        {
        try {
            scannedDevices = usbScanManager.awaitScannedDevices();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return scannedDevices;
        }

    RobotConfigMap instantiateTemplate(RobotConfigFile templateMeta, XmlPullParser xmlPullParser) throws RobotCoreException
        {
        awaitScannedDevices();
        //
        ReadXMLFileHandler readXMLFileHandler = new ReadXMLFileHandler();
        List<ControllerConfiguration> controllerList = readXMLFileHandler.parse(xmlPullParser);
        RobotConfigMap robotConfigMap = new RobotConfigMap(controllerList);
        robotConfigMap.bindUnboundControllers(scannedDevices);
        //
        return robotConfigMap;
        }

    private String indent(int count, String target)
        {
        String indent = "";
        for (int i = 0; i < count; i++) indent += " ";
        return indent + target.replace("\n", "\n" + indent);
        }

    //----------------------------------------------------------------------------------------------
    // Network listener
    //----------------------------------------------------------------------------------------------

    @Override
    public CallbackResult commandEvent(Command command)
        {
        CallbackResult result = CallbackResult.NOT_HANDLED;
        try
            {
            String name = command.getName();
            String extra = command.getExtra();

            if (name.equals(CommandList.CMD_SCAN_RESP))
                {
                result = handleCommandScanResp(extra);
                }
            else if (name.equals(CommandList.CMD_REQUEST_CONFIGURATIONS_RESP))
                {
                result = handleCommandRequestConfigurationsResp(extra);
                }
            else if (name.equals(CommandList.CMD_REQUEST_CONFIGURATION_TEMPLATES_RESP))
                {
                result = handleCommandRequestTemplatesResp(extra);
                }
            else if (name.equals(CommandList.CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP))
                {
                result = handleCommandRequestParticularConfigurationResp(extra);
                }
            else if (name.equals(CommandList.CMD_NOTIFY_ACTIVE_CONFIGURATION))
                {
                result = handleCommandNotifyActiveConfig(extra);
                }
            }
        catch (RobotCoreException e)
            {
            RobotLog.logStacktrace(e);
            }
        return result;
        }

    private CallbackResult handleCommandScanResp(String extra) throws RobotCoreException
        {
        Assert.assertTrue(remoteConfigure);
        usbScanManager.handleCommandScanResponse(extra);
        return CallbackResult.HANDLED_CONTINUE;  // someone else in the chain might want the same result
        }

    @Override
    public CallbackResult packetReceived(RobocolDatagram packet)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult peerDiscoveryEvent(RobocolDatagram packet)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult heartbeatEvent(RobocolDatagram packet, long tReceived)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult telemetryEvent(RobocolDatagram packet)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult gamepadEvent(RobocolDatagram packet)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult emptyEvent(RobocolDatagram packet)
        {
        return CallbackResult.NOT_HANDLED;
        }

    @Override
    public CallbackResult reportGlobalError(String error, boolean recoverable)
        {
        return CallbackResult.NOT_HANDLED;
        }
    }
