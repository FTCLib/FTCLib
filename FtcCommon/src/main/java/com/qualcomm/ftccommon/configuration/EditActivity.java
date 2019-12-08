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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.BuiltInConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationUtility;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;
import com.qualcomm.robotcore.hardware.configuration.Utility;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link EditActivity} is a base class that provides support for various configuration
 * editing activities.
 */
@SuppressWarnings("WeakerAccess")
public abstract class EditActivity extends ThemedActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "EditActivity";
	@Override public String getTag() { return TAG; }

    protected Context                context;
    protected AppUtil                appUtil = AppUtil.getInstance();
    protected boolean                remoteConfigure = AppUtil.getInstance().isDriverStation();
    protected Utility                utility;
    protected ConfigurationUtility   configurationUtility;
    protected RobotConfigFileManager robotConfigFileManager;
    protected RobotConfigFile        currentCfgFile;
    protected @IdRes int             idAddButton = R.id.addButton;     // id of the add button
    protected @IdRes int             idFixButton = R.id.fixButton;     // id of the fix button
    protected @IdRes int             idSwapButton = R.id.swapButton;
    protected ControllerConfiguration controllerConfiguration;
    protected RobotConfigMap         robotConfigMap = new RobotConfigMap();
    protected boolean                haveRobotConfigMapParameter = false;
    protected @NonNull ScannedDevices scannedDevices = new ScannedDevices();
    protected List<RobotConfigFile>  extantRobotConfigurations = new LinkedList<RobotConfigFile>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public EditActivity()
        {
        }

    @Override protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        this.context = this;
        PreferenceManager.setDefaultValues(this, R.xml.app_settings, false);
        this.utility = new Utility(this);
        this.configurationUtility = new ConfigurationUtility();
        this.robotConfigFileManager = new RobotConfigFileManager(this);
        this.currentCfgFile = robotConfigFileManager.getActiveConfig();
        }

    @Override
    protected void onStart()
        {
        super.onStart();
        this.robotConfigFileManager.updateActiveConfigHeader(this.currentCfgFile);
        }

    protected void deserialize(EditParameters parameters)
        {
        this.scannedDevices = parameters.getScannedDevices();
        this.extantRobotConfigurations = parameters.getExtantRobotConfigurations();
        this.controllerConfiguration = (parameters.getConfiguration() instanceof ControllerConfiguration) ? (ControllerConfiguration)parameters.getConfiguration() : null;
        // Caller can optionally provide an explicit config file; this avoids races with setting the contextual one
        if (parameters.getCurrentCfgFile() != null) this.currentCfgFile = parameters.getCurrentCfgFile();
        deserializeConfigMap(parameters);
        }

    protected void deserializeConfigMap(EditParameters parameters)
        {
        this.robotConfigMap = new RobotConfigMap(parameters.getRobotConfigMap());   // copy for isolation
        this.haveRobotConfigMapParameter = parameters.haveRobotConfigMapParameter();

        // Re-establish object identities
        if (this.robotConfigMap != null && this.controllerConfiguration != null)
            {
            if (this.robotConfigMap.contains(this.controllerConfiguration.getSerialNumber()))
                {
                this.controllerConfiguration = this.robotConfigMap.get(this.controllerConfiguration.getSerialNumber());
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    protected RobotConfigMap getRobotConfigMap()
        {
        return this.robotConfigMap==null ? new RobotConfigMap() : this.robotConfigMap;
        }

    //----------------------------------------------------------------------------------------------
    // Launching and finishing
    //----------------------------------------------------------------------------------------------

    protected void handleLaunchEdit(RequestCode requestCode, Class launchClass, List<DeviceConfiguration> currentItems)
        {
        handleLaunchEdit(requestCode, launchClass, new EditParameters<DeviceConfiguration>(this, DeviceConfiguration.class, currentItems));
        }
    protected void handleLaunchEdit(RequestCode requestCode, Class launchClass, DeviceConfiguration config)
        {
        handleLaunchEdit(requestCode, launchClass, new EditParameters(this, config));
        }

    protected void handleLaunchEdit(RequestCode requestCode, Class launchClass, EditParameters parameters)
        {
        handleLaunchEdit(requestCode, launchClass, parameters.toBundle());
        }
    private void handleLaunchEdit(RequestCode requestCode, Class launchClass, Bundle bundle)
        {
        Intent editIntent = new Intent(context, launchClass);
        editIntent.putExtras(bundle);
        setResult(RESULT_OK, editIntent);
        RobotLog.v("%s: starting activity %s code=%d", this.getClass().getSimpleName(), editIntent.getComponent().getShortClassName(), requestCode.value);
        startActivityForResult(editIntent, requestCode.value);
        }

    public static String formatSerialNumber(Context context, ControllerConfiguration controllerConfiguration)
        {
        String result = controllerConfiguration.getSerialNumber().toString();
        if (controllerConfiguration.getSerialNumber().isFake())
            {
            return result;
            }
        else
            {
            if (!controllerConfiguration.isKnownToBeAttached())
                {
                result = result + context.getString(R.string.serialNumberNotAttached);
                }
            return result;
            }
        }

    protected void finishCancel()
        {
        RobotLog.v("%s: cancelling", this.getClass().getSimpleName());
        setResult(RESULT_CANCELED, new Intent());
        finish();
        }
    protected void finishOk(EditParameters parameters)
        {
        RobotLog.v("%s: OK", this.getClass().getSimpleName());
        Intent returnIntent = new Intent();
        parameters.putIntent(returnIntent);
        finishOk(returnIntent);
        }
    protected void finishOk()
        {
        finishOk(new Intent());
        }
    protected void finishOk(Intent intent)
        {
        setResult(RESULT_OK, intent);
        finish();
        }

    @Override
    public void onBackPressed()
        {
        logBackPressed();
        finishOk(); // up until the 'file' level, back is 'Ok', not 'Cancel'
        }

    protected void logActivityResult(int requestCodeValue, int resultCode, Intent data)
        {
        RobotLog.v("%s: activity result: code=%d result=%d", this.getClass().getSimpleName(), requestCodeValue, resultCode);
        }
    protected void logBackPressed()
        {
        RobotLog.v("%s: backPressed received", this.getClass().getSimpleName());
        }

    //----------------------------------------------------------------------------------------------
    // Text watchers
    //----------------------------------------------------------------------------------------------

    protected class SetNameTextWatcher implements TextWatcher
        {
        private final DeviceConfiguration deviceConfiguration;

        protected SetNameTextWatcher(DeviceConfiguration deviceConfiguration)
            {
            this.deviceConfiguration = deviceConfiguration;
            }
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }
        public void afterTextChanged(Editable editable)
            {
            String text = editable.toString();
            deviceConfiguration.setName(text);
            }
        }

    //----------------------------------------------------------------------------------------------
    // List Management: display names + type
    //----------------------------------------------------------------------------------------------

    protected static class DisplayNameAndInteger implements Comparable<DisplayNameAndInteger>
        {
        public final String  displayName;
        public final int     value;

        public DisplayNameAndInteger(String displayName, int value)
            {
            this.displayName = displayName;
            this.value = value;
            }

        @Override public String toString()
            {
            return this.displayName;
            }

        @Override public int compareTo(DisplayNameAndInteger another)
            {
            return this.displayName.compareTo(another.displayName);
            }
        }

    protected static class DisplayNameAndRequestCode implements Comparable<DisplayNameAndRequestCode>
        {
        public final String      displayName;
        public final RequestCode requestCode;

        public DisplayNameAndRequestCode(String combined)
        // Example combined: "PWM Devices|EDIT_PWM_PORT"
            {
            String[] parts = combined.split("\\|");
            this.displayName = parts[0];
            this.requestCode = RequestCode.fromString(parts[1]);
            }

        public DisplayNameAndRequestCode(String displayName, RequestCode requestCode)
            {
            this.displayName = displayName;
            this.requestCode = requestCode;
            }

        public static DisplayNameAndRequestCode[] fromArray(String[] strings)
            {
            DisplayNameAndRequestCode[] result = new DisplayNameAndRequestCode[strings.length];
            for (int i = 0; i < result.length; i++)
                {
                result[i] = new DisplayNameAndRequestCode(strings[i]);
                }
            return result;
            }

        @Override public String toString()
            {
            return this.displayName;
            }

        @Override public int compareTo(DisplayNameAndRequestCode another)
            {
            return this.displayName.compareTo(another.displayName);
            }
        }

    //----------------------------------------------------------------------------------------------
    // ConfigurationType management
    //----------------------------------------------------------------------------------------------

    /**
     * This gets called if we're changing a device, so if the nametext was "NO DEVICE ATTACHED",
     * it should now be empty for editing. Otherwise, it should be the name of the device.
     *
     * @param nameText - the name field that may need to be "cleared"
     * @param device   - the module
     */
    protected void clearNameIfNecessary(EditText nameText, DeviceConfiguration device)
        {
        if (!device.isEnabled())
            {
            nameText.setText("");
            device.setName("");
            }
        else
            {
            nameText.setText(device.getName());
            }
        }

    public String disabledDeviceName()
        {
        return getString(R.string.noDeviceAttached);
        }

    public String nameOf(DeviceConfiguration config)
        {
        return nameOf(config.getName());
        }

    public String nameOf(String name)
        {
        if (name.equals(DeviceConfiguration.DISABLED_DEVICE_NAME))
            {
            name = getString(R.string.noDeviceAttached);  // localize this constant string
            }
        return name;
        }

    public String displayNameOfConfigurationType(ConfigurationType.DisplayNameFlavor flavor, ConfigurationType type)
        {
        return type.getDisplayName(flavor);
        }

    // Localization technique from http://www.katr.com/article_android_spinner01.php
    protected class ConfigurationTypeAndDisplayName
        {
        public final ConfigurationType.DisplayNameFlavor flavor;
        public final ConfigurationType configurationType;
        public final String            displayName;

        public ConfigurationTypeAndDisplayName(ConfigurationType.DisplayNameFlavor flavor, ConfigurationType configurationType)
            {
            this.flavor            = flavor;
            this.configurationType = configurationType;
            this.displayName       = displayNameOfConfigurationType(this.flavor, configurationType);
            }

        @Override public String toString()
            {
            return this.displayName;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Spinner support for those classes that use spinners
    //----------------------------------------------------------------------------------------------

    protected void localizeConfigTypeSpinner(ConfigurationType.DisplayNameFlavor flavor, Spinner spinner)
    // Localize the strings in the spinner. What's there now is the string form of the BuiltInConfigurationType
        {
        ArrayAdapter<String> existingAdapter = (ArrayAdapter<String>) spinner.getAdapter();
        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < existingAdapter.getCount(); i++)
            {
            strings.add(existingAdapter.getItem(i));
            }
        localizeConfigTypeSpinnerStrings(flavor, spinner, strings);
        }

    protected void localizeConfigTypeSpinnerStrings(ConfigurationType.DisplayNameFlavor flavor, Spinner spinner, List<String> strings)
    // Localize the strings in the spinner, where a list of string forms of BuiltInConfigurationType are passed
        {
        List<ConfigurationType> types = new LinkedList<ConfigurationType>();
        for (String string : strings)
            {
            types.add(BuiltInConfigurationType.fromString(string)); // NB: string is the enum string constant, not the XML tag
            }
        localizeConfigTypeSpinnerTypes(flavor, spinner, types);
        }

    protected void localizeConfigTypeSpinnerTypes(ConfigurationType.DisplayNameFlavor flavor, Spinner spinner, List<ConfigurationType> types)
    // Localize the strings in the spinner
        {
        ConfigurationTypeAndDisplayName[] pairs = new ConfigurationTypeAndDisplayName[types.size()];
        for (int i = 0; i < types.size(); i++)
            {
            ConfigurationType type = types.get(i);
            pairs[i] = new ConfigurationTypeAndDisplayName(flavor, type);
            }

        ConfigurationTypeArrayAdapter newAdapter = new ConfigurationTypeArrayAdapter(this, pairs);
        spinner.setAdapter(newAdapter);
        }

    protected int findPosition(Spinner spinner, ConfigurationType type)
    // Find the position of this indicated type in the localized spinner
        {
        ArrayAdapter<ConfigurationTypeAndDisplayName> adapter = (ArrayAdapter<ConfigurationTypeAndDisplayName>)spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++)
            {
            if (adapter.getItem(i).configurationType == type)
                {
                return i;
                }
            }
        return -1;
        }

    protected int findPosition(Spinner spinner, ConfigurationType typeA, ConfigurationType typeB)
    // Find the position of this indicated types in the localized spinner
        {
        int result = findPosition(spinner, typeA);
        if (result < 0) result = findPosition(spinner, typeB);
        return result;
        }

    protected void handleSpinner(View view, int spinnerId, DeviceConfiguration deviceConfiguration)
        {
        handleSpinner(view, spinnerId, deviceConfiguration, false);
        }
    protected void handleSpinner(View view, int spinnerId, DeviceConfiguration deviceConfiguration, boolean forceFind)
        {
        Spinner choiceSpinner = (Spinner)view.findViewById(spinnerId);
        if (forceFind || deviceConfiguration.isEnabled())
            {
            int spinnerPosition = findPosition(choiceSpinner, deviceConfiguration.getSpinnerChoiceType(), getDefaultEnabledSelection());
            choiceSpinner.setSelection(spinnerPosition);
            }
        else
            {
            choiceSpinner.setSelection(findPosition(choiceSpinner, BuiltInConfigurationType.NOTHING));
            }
        choiceSpinner.setOnItemSelectedListener(spinnerListener);
        }
    protected ConfigurationType getDefaultEnabledSelection()
        {
        return BuiltInConfigurationType.NOTHING;
        }

    /** The listener that controls the behavior when an item in the spinner is selected. */
    protected AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener()
        {
        @Override
        public void onItemSelected(AdapterView<?> parent, View spinnerItem, int pos, long l)
            {
            ConfigurationTypeAndDisplayName selected = (ConfigurationTypeAndDisplayName) parent.getItemAtPosition(pos);
            View itemView = itemViewFromSpinnerItem(spinnerItem);
            if (selected.configurationType == BuiltInConfigurationType.NOTHING)
                {
                clearDevice(itemView);
                }
            else
                {
                changeDevice(itemView, selected.configurationType);
                }
            }

        protected View itemViewFromSpinnerItem(View spinnerItem) {
            // TODO: this is fragile.
            //view is SpinnerItem
            //view.getParent is Spinner
            //view.getparent.getparent is the RelativeLayout around the Spinner
            //view.getparent.getparent.getparent is the item view around the whole item, usually a TableRow
            ViewParent spinner              = spinnerItem.getParent();      // RobotLog.v("spinner class =%s", spinner.getClass().getSimpleName());
            ViewParent spinnerParent        = spinner.getParent();          // RobotLog.v("relativeLayout class =%s", spinnerParent.getClass().getSimpleName());
            ViewParent spinnerParentParent  = spinnerParent.getParent();    // RobotLog.v("linearLayout class =%s", spinnerParentParent.getClass().getSimpleName());

            View itemView = (View) spinnerParentParent;
            return itemView;
            }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        };

    protected void clearDevice(View itemView)
        {
        // subclasses using spinners to provide something useful
        }
    protected void changeDevice(View itemView, ConfigurationType type)
        {
        // subclasses using spinners to provide something useful
        }

    //----------------------------------------------------------------------------------------------
    // Networking
    //----------------------------------------------------------------------------------------------

    protected void sendOrInject(Command cmd)
        {
        if (remoteConfigure)
            {
            NetworkConnectionHandler.getInstance().sendCommand(cmd);
            }
        else
            {
            NetworkConnectionHandler.getInstance().injectReceivedCommand(cmd);
            }
        }

    /** When doing remote config and we get notice that the config has changed, we need to
     * update our header string contents and attendant red vs grey etc coloring */
    protected CallbackResult handleCommandNotifyActiveConfig(String extra)
        {
        RobotLog.vv(TAG, "%s.handleCommandRequestActiveConfigResp(%s)", this.getClass().getSimpleName(), extra);
        final RobotConfigFile configFile = robotConfigFileManager.getConfigFromString(extra);

        // Remember it locally as a cache, which will make config editing better, and update our UI
        robotConfigFileManager.setActiveConfigAndUpdateUI(configFile);

        // We might be hiding others in the call chain who also want to hear about this. A typical
        // situation might be remote config editing screen (FtcFileLoadActivity) in front of the
        // main driver station screen: once the config edit screen dismisses, the file load screen
        // should be seen to be updated too.
        return CallbackResult.HANDLED_CONTINUE;
        }
    }
