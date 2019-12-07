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
package com.qualcomm.hardware.lynx;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import com.qualcomm.hardware.R;
import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.hardware.lynx.commands.LynxInterface;
import com.qualcomm.hardware.lynx.commands.LynxInterfaceCommand;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.hardware.lynx.commands.LynxRespondable;
import com.qualcomm.hardware.lynx.commands.LynxResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxDekaInterfaceCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxFtdiResetControlCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxPhoneChargeControlCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxPhoneChargeQueryCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxPhoneChargeQueryResponse;
import com.qualcomm.hardware.lynx.commands.core.LynxReadVersionStringCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxReadVersionStringResponse;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.hardware.lynx.commands.standard.LynxDiscoveryCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxFailSafeCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxGetModuleLEDColorCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxGetModuleStatusCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxGetModuleStatusResponse;
import com.qualcomm.hardware.lynx.commands.standard.LynxKeepAliveCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxNack;
import com.qualcomm.hardware.lynx.commands.standard.LynxQueryInterfaceCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxQueryInterfaceResponse;
import com.qualcomm.hardware.lynx.commands.standard.LynxSetDebugLogLevelCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxSetModuleLEDColorCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxSetModuleLEDPatternCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxSetNewModuleAddressCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxStandardCommand;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareDeviceHealth;
import com.qualcomm.robotcore.hardware.VisuallyIdentifiableHardwareDevice;
import com.qualcomm.robotcore.hardware.RobotConfigNameable;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.internal.network.WifiDirectInviteDialogMonitor;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.Misc;
import org.firstinspires.ftc.robotcore.internal.usb.LynxModuleSerialNumber;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link LynxModule} represents the connection between the host and a particular
 * Lynx controller module. Multiple Lynx controller modules may be chained together over RS485
 * and share a common USB connection.
 *
 * @see LynxUsbDeviceImpl
 */
@SuppressWarnings("WeakerAccess")
public class LynxModule extends LynxCommExceptionHandler implements LynxModuleIntf, RobotArmingStateNotifier, RobotArmingStateNotifier.Callback, Blinker, VisuallyIdentifiableHardwareDevice
    {
    //----------------------------------------------------------------------------------------------
    // Constants
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "LynxModule";
    @Override protected String getTag() { return TAG; }

    public static BlinkerPolicy blinkerPolicy = new CountModuleAddressBlinkerPolicy();

    protected static final int msInitialContact = 500;      // not an exact number; probably can be reduced
    protected static final int msKeepAliveTimeout = 2500;   // per the Lynx spec

    //----------------------------------------------------------------------------------------------
    // Command Meta State
    //----------------------------------------------------------------------------------------------

    /** A {@link Class} for one of the Lynx messages together with a cached constructor thereto */
    protected static class MessageClassAndCtor
        {
        public Class<? extends LynxMessage>        clazz;
        public Constructor<? extends LynxMessage>  ctor;

        public void assignCtor() throws NoSuchMethodException
            {
            try {
                this.ctor = this.clazz.getConstructor(LynxModule.class);
                }
            catch (NoSuchMethodException ignored)
                {
                try {
                    this.ctor = this.clazz.getConstructor(LynxModuleIntf.class);
                    }
                catch (NoSuchMethodException e)
                    {
                    this.ctor = null;
                    }
                }
            }
        }

    protected static Map<Integer,MessageClassAndCtor>                       standardMessages = new HashMap<Integer, MessageClassAndCtor>();    // command number -> class
    protected static Map<Class<? extends LynxCommand>,MessageClassAndCtor>  responseClasses = new HashMap<Class<? extends LynxCommand>, MessageClassAndCtor>();

    static {
        addStandardMessage(LynxAck.class);
        addStandardMessage(LynxNack.class);
        addStandardMessage(LynxKeepAliveCommand.class);
        addStandardMessage(LynxGetModuleStatusCommand.class);
        addStandardMessage(LynxFailSafeCommand.class);
        addStandardMessage(LynxSetNewModuleAddressCommand.class);
        addStandardMessage(LynxQueryInterfaceCommand.class);
        addStandardMessage(LynxSetNewModuleAddressCommand.class);
        addStandardMessage(LynxSetModuleLEDColorCommand.class);
        addStandardMessage(LynxGetModuleLEDColorCommand.class);

        correlateStandardResponse(LynxGetModuleStatusCommand.class);
        correlateStandardResponse(LynxQueryInterfaceCommand.class);
        correlateStandardResponse(LynxGetModuleLEDColorCommand.class);
        }

    protected static void addStandardMessage(Class<? extends LynxMessage> clazz)
        {
        try {
            Integer commandNumber = (Integer)LynxMessage.invokeStaticNullaryMethod(clazz, "getStandardCommandNumber");
            Assert.assertTrue((commandNumber & LynxResponse.RESPONSE_BIT)==0);
            //
            MessageClassAndCtor pair = new MessageClassAndCtor();
            pair.clazz = clazz;
            pair.assignCtor();
            standardMessages.put(commandNumber, pair);
            }
        catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e)
            {
            RobotLog.ee(TAG, "error registering %s", clazz.getSimpleName());
            }
        }

    protected static void correlateStandardResponse(Class<? extends LynxCommand> commandClass)
        {
        try {
            Class<? extends LynxResponse> responseClass = LynxCommand.getResponseClass(commandClass);
            correlateResponse(commandClass, responseClass);
            }
        catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e)
            {
            RobotLog.ee(TAG, "error registering response to %s", commandClass.getSimpleName());
            }
        }

    public static void correlateResponse(Class<? extends LynxCommand> commandClass, Class<? extends LynxResponse> responseClass) throws NoSuchMethodException
        {
        MessageClassAndCtor pair = new MessageClassAndCtor();
        pair.clazz = responseClass;
        pair.assignCtor();
        responseClasses.put(commandClass, pair);
        }

    //----------------------------------------------------------------------------------------------
    // Instance State
    //----------------------------------------------------------------------------------------------

    protected LynxUsbDevice         lynxUsbDevice;
    protected List<LynxController>  controllers;
    protected int                   moduleAddress;
    protected SerialNumber          moduleSerialNumber;
    protected AtomicInteger         nextMessageNumber;
    protected boolean               isParent;
    protected boolean               isSystemSynthetic;  // if true, then system made this up, not user
    protected boolean               isUserModule;       // if false, then this is for some system-admin purpose
    protected boolean               isEngaged;
    protected final Object          engagementLock = this; // 'this' for historical reasons; optimization might be possible
    protected boolean               isOpen;
    protected final Object          startStopLock;

    /** maps message number to command we've issued with said number */
    protected final ConcurrentHashMap<Integer,LynxRespondable>    unfinishedCommands;

    /** for all the commands we know about (standard + QueryInterface), maps command number to
     *  class which implements same. Only commands known to be supported by the module are populated */
    protected final ConcurrentHashMap<Integer,MessageClassAndCtor>          commandClasses;
    protected final Set<Class<? extends LynxCommand>>                       supportedCommands;

    protected final ConcurrentHashMap<String, LynxInterface>                interfacesQueried;

    /** This lock prevents concurrency problems that would arrive from
     *  interleaving messages of the (asynchronous) i2c protocol. In particular it
     *  makes sure that once we issue a read, we can actually read that data before
     *  we get back in there and, say, issue a write on another bus. */
    protected final Object                                    i2cLock;

    /** State for maintaining stack of blinker patterns */
    protected ArrayList<Step>                                 currentSteps;
    protected Deque<ArrayList<Step>>                          previousSteps;
    protected boolean                                         isVisuallyIdentifying;

    protected ScheduledExecutorService                        executor;
    protected Future<?>                                       pingFuture;
    protected Future<?>                                       attentionRequiredFuture;
    protected final Object                                    futureLock;
    protected boolean                                         ftdiResetWatchdogActive; // our actual current status
    protected boolean                                         ftdiResetWatchdogActiveWhenEngaged; // status when we were last engaged

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public LynxModule(LynxUsbDevice lynxUsbDevice, int moduleAddress, boolean isParent)
        {
        this.lynxUsbDevice      = lynxUsbDevice;
        this.controllers        = new CopyOnWriteArrayList<LynxController>();
        this.moduleAddress      = moduleAddress;
        this.moduleSerialNumber = new LynxModuleSerialNumber(lynxUsbDevice.getSerialNumber(), moduleAddress);
        this.isParent           = isParent;
        this.isSystemSynthetic  = false;
        this.isEngaged          = true;
        this.isUserModule       = true;
        this.isOpen             = true;
        this.startStopLock      = new Object();

        this.nextMessageNumber  = new AtomicInteger(0); // TODO: start with random number? Either that, or send a 'new msg# seq' start of some sort (less important now that we reset before connecting)
        this.commandClasses     = new ConcurrentHashMap<Integer, MessageClassAndCtor>(standardMessages);
        this.supportedCommands  = new HashSet<>();
        for (MessageClassAndCtor pair : this.commandClasses.values())
            {
            if (ClassUtil.inheritsFrom(pair.clazz, LynxCommand.class))
                {
                //noinspection unchecked
                supportedCommands.add((Class<? extends LynxCommand>)pair.clazz);
                }
            }
        this.interfacesQueried  = new ConcurrentHashMap<String, LynxInterface>();
        this.unfinishedCommands = new ConcurrentHashMap<Integer, LynxRespondable>();
        this.i2cLock            = new Object();
        this.currentSteps       = new ArrayList<Step>();
        this.previousSteps      = new ArrayDeque<ArrayList<Step>>();
        this.isVisuallyIdentifying = false;
        this.executor           = null;
        this.pingFuture         = null;
        this.attentionRequiredFuture = null;
        this.futureLock         = new Object();
        this.ftdiResetWatchdogActive = false;
        this.ftdiResetWatchdogActiveWhenEngaged = false;

        startExecutor();

        this.lynxUsbDevice.registerCallback(this, false);
        }

    @Override public String toString()
        {
        return Misc.formatForUser("LynxModule(mod#=%d)", this.moduleAddress);
        }

    @Override public void close()
        {
        synchronized (startStopLock)
            {
            RobotLog.vv(TAG, "close(#%d)", moduleAddress);
            stopFtdiResetWatchdog();
            this.isOpen = false;
            stopAttentionRequired();
            stopPingTimer(true);
            stopExecutor();
            }
        }

    //----------------------------------------------------------------------------------------------
    // Accessors
    //----------------------------------------------------------------------------------------------

    public boolean isUserModule()
        {
        return this.isUserModule;
        }

    public void setUserModule(boolean isUserModule)
        {
        this.isUserModule = isUserModule;
        }

    public boolean isSystemSynthetic()
        {
        return this.isSystemSynthetic;
        }

    public void setSystemSynthetic(boolean systemSynthetic)
        {
        this.isSystemSynthetic = systemSynthetic;
        }

    public void noteController(LynxController controller)
        {
        this.controllers.add(controller);
        }

    public int getModuleAddress()
        {
        return this.moduleAddress;
        }

    public void setNewModuleAddress(final int newModuleAddress)
        {
        if (newModuleAddress != getModuleAddress())
            {
            this.lynxUsbDevice.changeModuleAddress(this, newModuleAddress, new Runnable()
                {
                @Override public void run()
                    {
                    try {
                        LynxSetNewModuleAddressCommand command = new LynxSetNewModuleAddressCommand(LynxModule.this, (byte)newModuleAddress);
                        command.acquireNetworkLock();   // make the lock span the updating of our module address variable too
                        try {
                            command.send();
                            LynxModule.this.moduleAddress = newModuleAddress;
                            }
                        finally
                            {
                            command.releaseNetworkLock();
                            }
                        }
                    catch (LynxNackException|InterruptedException e)
                        {
                        handleException(e);
                        }
                    }
                });
            }
        }

    protected byte getNewMessageNumber()
        {
        for (;;)
            {
            byte result = (byte)this.nextMessageNumber.getAndIncrement();

            // message numbers may never be zero, and we don't want to reuse a number
            // that's currently already in use
            if (result != 0 && !this.unfinishedCommands.containsKey((int)result))
                {
                return result;
                }
            }
        }

    public void noteAttentionRequired()
        {
        if (isUserModule())
            {
            synchronized (this.futureLock)
                {
                if (this.isOpen)
                    {
                    if (this.attentionRequiredFuture != null)
                        {
                        this.attentionRequiredFuture.cancel(false);
                        }

                    // Clear the attention required signal
                    this.attentionRequiredFuture = this.executor.submit(new Runnable()
                        {
                        @Override public void run()
                            {
                            sendGetModuleStatusAndProcessResponse(true);
                            }
                        });
                    }
                }

            // Forget any cached data as the attention might have invalidated things
            forgetLastKnown();
            }
        }

    protected void stopAttentionRequired()
        {
        synchronized (futureLock)
            {
            if (this.attentionRequiredFuture != null)
                {
                this.attentionRequiredFuture.cancel(true);
                ThreadPool.awaitFuture(this.attentionRequiredFuture, 250, TimeUnit.MILLISECONDS);
                this.attentionRequiredFuture = null;
                }
            }
        }

    protected void sendGetModuleStatusAndProcessResponse(boolean clearStatus)
        {
        LynxGetModuleStatusCommand command = new LynxGetModuleStatusCommand(LynxModule.this, clearStatus);
        try {
            LynxGetModuleStatusResponse response = command.sendReceive();
            if (response.testAnyBits(~LynxGetModuleStatusResponse.bitFailSafe))
                {
                // We *explicitly* enter fail safe so often (in the default opmode) that this log is too much
                RobotLog.vv(TAG, "received status: %s", response.toString());
                }
            if (response.isKeepAliveTimeout())
                {
                // The keep alive timeout made the module forget what we were using. Now that
                // we're back, re-establish what the user expects to be blinking.
                resendCurrentPattern();
                }
            }
        catch (LynxNackException|RuntimeException|InterruptedException e)
            {
            handleException(e);
            }
        }

    protected void forgetLastKnown()
        {
        for (LynxController controller : this.controllers)
            {
            controller.forgetLastKnown();
            }
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public Manufacturer getManufacturer()
        {
        return Manufacturer.Lynx;
        }

    @Override
    public String getDeviceName()
        {
        return String.format("%s (%s)", AppUtil.getDefContext().getString(R.string.lynxModuleDisplayName), getFirmwareVersionString());
        }

    @Override
    public String getFirmwareVersionString()
        {
        String result = getNullableFirmwareVersionString();
        if (result == null)
            {
            result = AppUtil.getDefContext().getString(com.qualcomm.robotcore.R.string.lynxUnavailableFWVersionString);
            }
        return result;
        }

    public String getNullableFirmwareVersionString()
        {
        try {
            LynxReadVersionStringCommand command = new LynxReadVersionStringCommand(this);
            LynxReadVersionStringResponse response = command.sendReceive();
            return response.getNullableVersionString();
            }
        catch (LynxNackException|InterruptedException e)
            {
            handleException(e);
            }
        return null;
        }

    @Override
    public String getConnectionInfo()
        {
        return String.format("%s; module %d", this.lynxUsbDevice.getConnectionInfo(), this.getModuleAddress());
        }

    @Override
    public int getVersion()
        {
        return 1;
        }

    @Override
    public void resetDeviceConfigurationForOpMode()
        {
        }

    // Separate here for deadlock paranoia. Used only where we know we need it. Could
    // perhaps use more generally
    public void removeAsConfigured()
        {
        this.lynxUsbDevice.removeConfiguredModule(this);
        }

    /**
     * Returns any global warnings from this device. Currently, such warnings are culled from
     * the health status of the controllers attached to this module. If no warnings are currently
     * relevant, then an empty list is returned.
     */
    public List<String> getGlobalWarnings()
        {
        List<String> result = new ArrayList<String>();
        for (LynxController controller : controllers)
            {
            String candidate = getHealthStatusWarningMessage(controller);
            if (!candidate.isEmpty())
                {
                result.add(candidate);
                }
            }
        return result;
        }

    /** Returns a status warning message indicative of the health of the indicated device, or an
     * empty string if no such message is currently applicable. */
    public static @NonNull String getHealthStatusWarningMessage(HardwareDeviceHealth hardwareDeviceHealth)
        {
        switch (hardwareDeviceHealth.getHealthStatus())
            {
            case UNHEALTHY:
                String name = null;
                if (hardwareDeviceHealth instanceof RobotConfigNameable)
                    {
                    name = ((RobotConfigNameable)hardwareDeviceHealth).getUserConfiguredName();
                    if (name != null)
                        {
                        name = AppUtil.getDefContext().getString(R.string.quotes, name);
                        }
                    }
                if (name == null && hardwareDeviceHealth instanceof HardwareDevice)
                    {
                    HardwareDevice hardwareDevice = (HardwareDevice)hardwareDeviceHealth;
                    String typeDescription = hardwareDevice.getDeviceName();
                    String connectionInfo = hardwareDevice.getConnectionInfo();
                    name = AppUtil.getDefContext().getString(R.string.hwDeviceDescriptionAndConnection, typeDescription, connectionInfo);
                    }
                if (name == null)
                    {
                    name = AppUtil.getDefContext().getString(R.string.hwPoorlyNamedDevice);
                    }
                return AppUtil.getDefContext().getString(R.string.unhealthyDevice, name);
            default:
                return "";
            }
        }

    //----------------------------------------------------------------------------------------------
    // RobotArmingStateNotifier
    //----------------------------------------------------------------------------------------------

    public SerialNumber getModuleSerialNumber()
        {
        return moduleSerialNumber;
        }

    @Override public SerialNumber getSerialNumber()
        {
        return this.lynxUsbDevice.getSerialNumber();
        }

    @Override
    public ARMINGSTATE getArmingState()
        {
        return this.lynxUsbDevice.getArmingState();
        }

    @Override
    public void registerCallback(Callback callback, boolean doInitialCallback)
        {
        this.lynxUsbDevice.registerCallback(callback, doInitialCallback);
        }

    @Override
    public void unregisterCallback(Callback callback)
        {
        this.lynxUsbDevice.unregisterCallback(callback);
        }

    @Override
    public void onModuleStateChange(RobotArmingStateNotifier module, ARMINGSTATE state)
        {
        switch (state)
            {
            case DISARMED:
                // when disarmed, clean up so that we're good to go for next armed transition
                // this.resetI2cLock(); // no longer needed
                break;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Engagable
    //----------------------------------------------------------------------------------------------

    @Override public void engage()
        {
        synchronized (engagementLock)
            {
            if (!isEngaged)
                {
                RobotLog.vv(TAG, "engaging lynx module #%d", getModuleAddress());
                for (LynxController controller : this.controllers)
                    {
                    controller.engage();
                    }
                isEngaged = true;
                if (ftdiResetWatchdogActiveWhenEngaged)
                    {
                    startFtdiResetWatchdog();
                    }
                }
            }
        }

    @Override public void disengage()
        {
        synchronized (engagementLock)
            {
            if (isEngaged)
                {
                RobotLog.vv(TAG, "disengaging lynx module #%d", getModuleAddress());
                stopFtdiResetWatchdog(true); // paranoia: do *before* commencing disengagement to avoid interactions with transmission infrastructure
                isEngaged = false;
                //
                nackUnfinishedCommands();
                for (LynxController controller : this.controllers)
                    {
                    controller.disengage();
                    }
                nackUnfinishedCommands(); // paranoia
                }
            }
        }

    @Override public boolean isEngaged()
        {
        synchronized (engagementLock)
            {
            return isEngaged;
            }
        }

    //----------------------------------------------------------------------------------------------
    // VisuallyIdentifiableHardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public void visuallyIdentify(boolean shouldIdentify)
        {
        synchronized (this)
            {
            if (isVisuallyIdentifying != shouldIdentify)
                {
                if (!isVisuallyIdentifying)
                    {
                    internalPushPattern(blinkerPolicy.getVisuallyIdentifyPattern(this));
                    }
                else
                    {
                    popPattern();
                    }
                isVisuallyIdentifying = shouldIdentify;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Blinker interface
    //----------------------------------------------------------------------------------------------

    @Override public int getBlinkerPatternMaxLength()
        {
        return LynxSetModuleLEDPatternCommand.maxStepCount;
        }

    @Override public void setConstant(@ColorInt int color)
        {
        Step step = new Step(color, 1, TimeUnit.SECONDS);
        List<Step> steps = new ArrayList<Step>();
        steps.add(step);
        setPattern(steps);
        }

    @Override public void stopBlinking()
        {
        setConstant(Color.BLACK);
        }

    @Override public synchronized void setPattern(Collection<Step> steps)
        {
        this.currentSteps = steps == null ? new ArrayList<Step>() : new ArrayList<Step>(steps);
        sendLEDPatternSteps(this.currentSteps);
        }

    @Override public synchronized Collection<Step> getPattern()
        {
        // Return a copy so caller can't mess with us
        return new ArrayList<Step>(this.currentSteps);
        }

    protected void resendCurrentPattern()
        {
        RobotLog.vv(TAG,"resendCurrentPattern()");
        sendLEDPatternSteps(this.currentSteps);
        }

    @Override public synchronized void pushPattern(Collection<Step> steps)
        {
        visuallyIdentify(false);
        internalPushPattern(steps);
        }

    protected void internalPushPattern(Collection<Step> steps)
        {
        this.previousSteps.push(this.currentSteps);
        setPattern(steps);
        }

    @Override public synchronized boolean patternStackNotEmpty()
        {
        return this.previousSteps.size() > 0;
        }

    @Override public synchronized boolean popPattern()
        {
        try {
            setPattern(previousSteps.pop());
            return true;
            }
        catch (NoSuchElementException e)
            {
            setPattern(null);
            }
        return false;
        }

    void sendLEDPatternSteps(Collection<Step> steps)
        {
        RobotLog.vv(TAG, "sendLEDPatternSteps(): #steps=%d", steps.size());

        // Hack: in the current (as of 2016.12.12) version of the firmware, if you send an LED pattern
        // as the first message when coming back from KA time out, it will be overwritten. As a work
        // around, we ALWAYS send a KA before you send the LED pattern to ensure that it isn't timed out.
        // Unfortunately, that work-around doesn't seem to work. We leave it in for the moment
        // nevertheless. // TODO REVIEW
        ping();

        // Now actually send the pattern
        LynxSetModuleLEDPatternCommand.Steps commandSteps = new LynxSetModuleLEDPatternCommand.Steps();
        for (Step step : steps)
            {
            commandSteps.add(step);
            }
        LynxSetModuleLEDPatternCommand patternCommand = new LynxSetModuleLEDPatternCommand(this, commandSteps);

        try {
            patternCommand.sendReceive();
            }
        catch (InterruptedException|LynxNackException|RuntimeException e)
            {
            handleException(e);
            }
        }

    /**
     * {@link BlinkerPolicy} embodies the various blinker patterns exhibited by {@link LynxModule}s
     * The policy can be changed, globally, by setting the variable {@link #blinkerPolicy}.
     */
    public interface BlinkerPolicy
        {
        List<Step> getIdlePattern(LynxModule lynxModule);
        List<Step> getVisuallyIdentifyPattern(LynxModule lynxModule);
        }

    public static class BreathingBlinkerPolicy implements BlinkerPolicy
        {
        // 'Breathes' in and out in cyan
        @Override public List<Step> getIdlePattern(LynxModule lynxModule)
            {
            float[] hsv = new float[] { 0, 0, 0 };
            Color.colorToHSV(Color.CYAN, hsv);
            final float hue = hsv[0];
            final float saturation = 1;

            final int iStepLast = LynxSetModuleLEDPatternCommand.maxStepCount/2;
            final int msIncrement = 2000 / (2 * iStepLast);
            final List<Step> steps = new ArrayList<>();

            Consumer<Integer> makeStep = new Consumer<Integer>()
                {
                @Override public void accept(Integer iStep)
                    {
                    float min = 0.05f;
                    float max = 1.0f;
                    float value = (float)iStep / (float)iStepLast * (max-min) + min;
                    value = 1-(float)Math.sqrt(1-value); // darken brighter bits more than darker bits
                    @ColorInt int color = Color.HSVToColor(new float[] { hue, saturation, value });
                    steps.add(new Step(color, msIncrement, TimeUnit.MILLISECONDS));
                    }
                };

            for (int iStep = 0; iStep <= iStepLast; iStep++)
                {
                makeStep.accept(iStep);
                }
            for (int iStep = iStepLast-1; iStep > 0; iStep--)
                {
                makeStep.accept(iStep);
                }

            return steps;
            }


        // Blinks back and forth between cyan and magenta
        @Override public List<Step> getVisuallyIdentifyPattern(LynxModule lynxModule)
            {
            List<Step> steps = new ArrayList<>();
            int msIncrement = 150;
            steps.add(new Blinker.Step(Color.CYAN,    msIncrement,    TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.BLACK,  msIncrement /2, TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.MAGENTA, msIncrement,    TimeUnit.MILLISECONDS));
            steps.add(new Blinker.Step(Color.BLACK,  msIncrement /2, TimeUnit.MILLISECONDS));
            return steps;
            }
        }

    public static class CountModuleAddressBlinkerPolicy extends BreathingBlinkerPolicy
        {
        @Override public List<Step> getIdlePattern(LynxModule lynxModule)
            {
            // We set the LED to be a solid green, interrupted occasionally by a brief off duration.
            int msLivenessLong = 5000;
            int msLivenessShort = 500;
            List<Step> steps = new ArrayList<Step>();
            steps.add(new Step(Color.GREEN, msLivenessLong - msLivenessShort, TimeUnit.MILLISECONDS));
            steps.add(new Step(Color.BLACK, msLivenessShort, TimeUnit.MILLISECONDS));

            // Then we blink the module address in blue
            int slotsRemaining = LynxSetModuleLEDPatternCommand.maxStepCount-steps.size();
            int blinkCount = Math.min(lynxModule.getModuleAddress(), slotsRemaining/2);
            for (int i = 0; i < blinkCount; i++)
                {
                steps.add(new Step(Color.BLUE, msLivenessShort, TimeUnit.MILLISECONDS));
                steps.add(new Step(Color.BLACK, msLivenessShort, TimeUnit.MILLISECONDS));
                }

            return steps;
            }
        }

    //----------------------------------------------------------------------------------------------
    // QueryInterface
    //----------------------------------------------------------------------------------------------

    public boolean isParent()
        {
        return this.isParent;
        }

    /** Do all the stuff we need to do when we've become aware that this module is in fact
     * attached to its USB device. */
    public void pingAndQueryKnownInterfacesAndEtc(long randSeed) throws RobotCoreException, InterruptedException
        {
        RobotLog.vv(TAG, "pingAndQueryKnownInterfaces mod=%d", this.getModuleAddress());
        // We always ping first, just in case he's stuck right now in discovery mode.
        // Note that, in doing so, we also need to make sure we ping the parent first
        // before we ping any children; that is the responsibility of our caller.
        pingInitialContact();
        queryInterface(LynxDekaInterfaceCommand.theInterface);
        startFtdiResetWatchdog();
        if (isUserModule())
            {
            initializeDebugLogging();
            initializeLEDS();

            // Figure out if we're the right guy for the embedded UI blinker
            if (this.isParent())
                {
                if (LynxConstants.isEmbeddedSerialNumber(this.getSerialNumber()))
                    {
                    RobotLog.vv(TAG, "setAsPrimaryLynxModuleForUI(mod=%d)", this.getModuleAddress());
                    WifiDirectInviteDialogMonitor.setUILynxModule(this);
                    }
                }
            }
        }

    protected void initializeLEDS()
        {
        setPattern(blinkerPolicy.getIdlePattern(this));
        }

    protected void initializeDebugLogging() throws RobotCoreException, InterruptedException
        {
        setDebug(DebugGroup.MODULELED, DebugVerbosity.HIGH);
        }

    // Ping until we get an ack. The biggest cause of failures we've seen is that somehow
    // the framing gets off and initial acks are thus lost. We just keep trying, but only
    // for a while. Usually this will return really quickly, but if the module is in fact
    // not there, or it's there but having suffered from a failed firmware update attempt,
    // it will not ever respond.
    protected void pingInitialContact() throws RobotCoreException, InterruptedException
        {
        ElapsedTime duration = new ElapsedTime();
        while (duration.milliseconds() < msInitialContact)
            {
            try {
                ping(true);
                return;
                }
            catch (RobotCoreException|LynxNackException|RuntimeException e)
                {
                RobotLog.vv(TAG, "retrying ping mod=%d", this.getModuleAddress());
                }
            }
        throw new RobotCoreException("initial ping contact failed: mod=%d", this.getModuleAddress());
        }

    public void validateCommand(LynxMessage lynxMessage) throws LynxUnsupportedCommandException
        {
        synchronized (this.interfacesQueried)
            {
            // We can't reasonably validate anything if we're not armed and so talking to a real module
            if (lynxUsbDevice.getArmingState() != ARMINGSTATE.ARMED)
                {
                return;
                }

            // If it's a standard command, then we're good
            int commandNumber = lynxMessage.getCommandNumber();
            if (LynxStandardCommand.isStandardCommandNumber(commandNumber))
                {
                return;
                }

            // Otherwise, it's an interface command
            if (!commandClasses.containsKey(commandNumber))
                {
                throw new LynxUnsupportedCommandException(this, lynxMessage);
                }
            }
        }

    /** Answers as to whether the command is actively supported by the module, at least in SOME
     * interface, or as a standard command */
    public boolean isCommandSupported(Class<? extends LynxCommand> clazz)
        {
        synchronized (this.interfacesQueried)
            {
            /** Nothing but discovery is supported on fake modules. See {@link LynxUsbDeviceImpl#discoverModules} */
            return moduleAddress==0
                    ? clazz==LynxDiscoveryCommand.class
                    : supportedCommands.contains(clazz);
            }
        }

    /** Issues a query interface for the indicated interface and processes the results. This
     * method is idempotent, and copes with a module changing its mind about command numbering.
     * @return whether or not the interface is supported */
    protected boolean queryInterface(LynxInterface theInterface) throws InterruptedException
        {
        boolean supported = false;
        synchronized (this.interfacesQueried)
            {
            LynxQueryInterfaceCommand queryInterfaceCommand = new LynxQueryInterfaceCommand(this, theInterface.getInterfaceName());
            this.interfacesQueried.put(theInterface.getInterfaceName(), theInterface);
            try {
                // Query the module
                LynxQueryInterfaceResponse response = queryInterfaceCommand.sendReceive();
                theInterface.setWasNacked(response.isNackReceived());
                theInterface.setBaseCommandNumber(response.getCommandNumberFirst());
                RobotLog.vv(TAG, "mod#=%d queryInterface(%s)=%d commands starting at %d", getModuleAddress(), theInterface.getInterfaceName(), response.getNumberOfCommands(), response.getCommandNumberFirst());

                // Clean out our old notions of the commands in this interface
                final List<Class<? extends LynxInterfaceCommand>> interfaceCommandClasses = theInterface.getCommandClasses();
                for (Map.Entry<Integer,MessageClassAndCtor> pair : this.commandClasses.entrySet())
                    {
                    if (interfaceCommandClasses.contains(pair.getValue().clazz))
                        {
                        commandClasses.remove(pair.getKey());
                        supportedCommands.remove(pair.getValue().clazz);
                        }
                    }

                // Add the current notion of the commands in this interface
                int iCommand = 0;
                for (Class<? extends LynxInterfaceCommand> interfaceCommandClass : interfaceCommandClasses)
                    {
                    // If the module returned fewer commands for this interface than what we know
                    // about, then we'll assume it's working with an older version of the interface.
                    // This policy allows commands to be added to the end of the interface while still
                    // using the same interface name.
                    if (iCommand >= response.getNumberOfCommands())
                        {
                        RobotLog.vv(TAG, "mod#=%d intf=%s: expected %d commands; found %d", getModuleAddress(), theInterface.getInterfaceName(), interfaceCommandClasses.size(), response.getNumberOfCommands());
                        break;
                        }

                    if (interfaceCommandClass==null)
                        {
                        // empty, placeholder entry in table
                        }
                    else
                        {
                        try {
                            int commandNumber = iCommand + response.getCommandNumberFirst();
                            MessageClassAndCtor pair = new MessageClassAndCtor();
                            pair.clazz = interfaceCommandClass;
                            pair.assignCtor();
                            commandClasses.put(commandNumber, pair);
                            supportedCommands.add(interfaceCommandClass);
                            }
                        catch (NoSuchMethodException|RuntimeException e)
                            {
                            RobotLog.ee(TAG, e, "exception registering %s", interfaceCommandClass.getSimpleName());
                            }
                        }
                    iCommand++;
                    }

                supported = true;
                }
            catch (LynxNackException e)
                {
                // The interface is not supported. Leave the nack in the command so that
                // getInterfaceBaseCommandNumber will find it later.
                RobotLog.vv(TAG, "mod#=%d queryInterface(): interface %s is not supported", getModuleAddress(), theInterface.getInterfaceName());
                }
            catch (RuntimeException e)
                {
                RobotLog.ee(TAG, e, "exception during queryInterface(%s)", theInterface.getInterfaceName());
                }
            }
        return supported;
        }

    /** Returns the first command number to use for the given interface. Throws if not supported */
    public int getInterfaceBaseCommandNumber(String interfaceName)
        {
        synchronized (this.interfacesQueried)
            {
            LynxInterface anInterface = this.interfacesQueried.get(interfaceName);
            if (anInterface == null)
                return LynxInterface.ERRONEOUS_COMMAND_NUMBER;  // we never queried. why? shutdown?

            if (!anInterface.wasNacked())
                {
                return anInterface.getBaseCommandNumber();
                }
            else
                throw new IllegalArgumentException(String.format("interface \"%s\" not supported", interfaceName));
            }
        }

    //----------------------------------------------------------------------------------------------
    // Pinging
    //----------------------------------------------------------------------------------------------

    protected void ping()
        {
        try {
            ping(false);
            }
        catch (RobotCoreException|InterruptedException|LynxNackException|RuntimeException e)
            {
            handleException(e);
            }
        }

    protected void ping(boolean initialPing) throws RobotCoreException, InterruptedException, LynxNackException
        {
        // RobotLog.vv(TAG, "pinging mod#=%d initial=%s", getModuleAddress(), initialPing);
        LynxKeepAliveCommand command = new LynxKeepAliveCommand(this, initialPing);
        command.send();
        }

    protected int getMsModulePingInterval()
        {
        return msKeepAliveTimeout - 550;
        }

    @Override public void resetPingTimer(@NonNull LynxMessage message)
        {
        startPingTimer();
        }

    protected void startPingTimer()
        {
        synchronized (this.futureLock)
            {
            stopPingTimer(false);
            if (this.isOpen)
                {
                try {
                    this.pingFuture = this.executor.schedule(new Runnable()
                        {
                        @Override public void run()
                            {
                            ping();
                            }
                        }, getMsModulePingInterval(), TimeUnit.MILLISECONDS);
                    }
                catch (RejectedExecutionException e)
                    {
                    RobotLog.vv(TAG, "mod#=%d: scheduling of ping rejected: ignored", getModuleAddress());
                    this.pingFuture = null;
                    }
                }
            }
        }

    protected void stopPingTimer(boolean wait)
        {
        synchronized (this.futureLock)
            {
            if (this.pingFuture != null)
                {
                this.pingFuture.cancel(false);
                if (wait)
                    {
                    if (!ThreadPool.awaitFuture(this.pingFuture, 250, TimeUnit.MILLISECONDS))
                        {
                        RobotLog.vv(TAG, "mod#=%d: unable to await ping future cancellation", getModuleAddress());
                        }
                    }
                this.pingFuture = null;
                }
            }
        }

    protected void startFtdiResetWatchdog()
        {
        synchronized (engagementLock)
            {
            if (!ftdiResetWatchdogActive)
                {
                ftdiResetWatchdogActive = true;
                setFtdiResetWatchdog(true);
                }
            if (isEngaged) ftdiResetWatchdogActiveWhenEngaged = ftdiResetWatchdogActive;
            }
        }

    protected void stopFtdiResetWatchdog()
        {
        stopFtdiResetWatchdog(false);
        }

    protected void stopFtdiResetWatchdog(boolean disengaging)
        {
        synchronized (engagementLock)
            {
            if (ftdiResetWatchdogActive)
                {
                ftdiResetWatchdogActive = false;
                setFtdiResetWatchdog(false);
                }
            if (isEngaged && !disengaging) ftdiResetWatchdogActiveWhenEngaged = ftdiResetWatchdogActive;
            }
        }

    protected void setFtdiResetWatchdog(boolean enabled)
        {
        if (isCommandSupported(LynxFtdiResetControlCommand.class))
            {
            boolean wasInterrupted = Thread.interrupted(); // also clears interrupted flag

            RobotLog.vv(TAG, "sending LynxFtdiResetControlCommand(%s) wasInterrupted=%s", enabled, wasInterrupted);
            try {
                LynxFtdiResetControlCommand command = new LynxFtdiResetControlCommand(this, enabled);
                command.sendReceive();
                }
            catch (InterruptedException|LynxNackException|RuntimeException e)
                {
                handleException(e);
                }

            if (wasInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    protected void startExecutor()
        {
        if (this.executor == null)
            {
            this.executor = ThreadPool.newScheduledExecutor(1, "lynx module executor");
            }
        }

    protected void stopExecutor()
        {
        if (this.executor != null)
            {
            this.executor.shutdownNow();
            try {
                ThreadPool.awaitTermination(executor, 2, TimeUnit.SECONDS, "lynx module executor");
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();;
                }
            }
        }

    //----------------------------------------------------------------------------------------------
    // Misc other commands
    //----------------------------------------------------------------------------------------------

    public void failSafe() throws RobotCoreException, InterruptedException, LynxNackException
        {
        LynxFailSafeCommand command = new LynxFailSafeCommand(this);
        command.send();
        //
        forgetLastKnown();
        }

    public void enablePhoneCharging(boolean enable) throws RobotCoreException, InterruptedException, LynxNackException
        {
        LynxPhoneChargeControlCommand command = new LynxPhoneChargeControlCommand(this, enable);
        command.send();
        }

    public boolean isPhoneChargingEnabled() throws RobotCoreException, InterruptedException, LynxNackException
        {
        LynxPhoneChargeQueryCommand command = new LynxPhoneChargeQueryCommand(this);
        LynxPhoneChargeQueryResponse response = command.sendReceive();
        return response.isChargeEnabled();
        }

    //----------------------------------------------------------------------------------------------
    // Debug control
    //----------------------------------------------------------------------------------------------

    public enum DebugGroup
        {
        NONE(0),
            MAIN(1), TOHOST(2), FROMHOST(3), ADC(4), PWMSERVO(5), MODULELED(6),
            DIGITALIO(7), I2C(8), MOTOR0(9), MOTOR1(10), MOTOR2(11), MOTOR3(12);

        public final byte bVal;

        DebugGroup(int b)
            {
            this.bVal = (byte)b;
            }
        public static DebugGroup fromInt(int b)
            {
            for (DebugGroup debugGroup : DebugGroup.values())
                {
                if (debugGroup.bVal == (byte)b)
                    return debugGroup;
                }
            return NONE;
            }
        }

    public enum DebugVerbosity
        {
        OFF(0), LOW(1), MEDIUM(2), HIGH(3);

        public final byte bVal;

        DebugVerbosity(int b)
            {
            this.bVal = (byte)b;
            }
        public static DebugVerbosity fromInt(int b)
            {
            for (DebugVerbosity verbosity : DebugVerbosity.values())
                {
                if (verbosity.bVal == (byte)b)
                    return verbosity;
                }
            return OFF;
            }
        }

    public void setDebug(DebugGroup group, DebugVerbosity verbosity) throws InterruptedException
        {
        try {
            LynxSetDebugLogLevelCommand command = new LynxSetDebugLogLevelCommand(this, group, verbosity);
            command.send();
            }
        catch (LynxNackException|RuntimeException e)
            {
            handleException(e);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Transmission
    //----------------------------------------------------------------------------------------------

    public <T> T acquireI2cLockWhile(Supplier<T> supplier) throws InterruptedException, RobotCoreException, LynxNackException
        {
        synchronized (i2cLock)
            {
            return supplier.get();
            }
        }

    public void acquireNetworkTransmissionLock(@NonNull LynxMessage message) throws InterruptedException
        {
        this.lynxUsbDevice.acquireNetworkTransmissionLock(message);
        }

    public void releaseNetworkTransmissionLock(@NonNull LynxMessage message) throws InterruptedException
        {
        this.lynxUsbDevice.releaseNetworkTransmissionLock(message);
        }

    /**
     * Sends a command to the module, scheduling retransmissions as necessary.
     */
    public void sendCommand(LynxMessage command) throws InterruptedException, LynxUnsupportedCommandException
        {
        command.setMessageNumber(getNewMessageNumber());
        int msgnumCur = command.getMessageNumber();

        // Serialize this guy and remember it
        LynxDatagram datagram = new LynxDatagram(command); // throws LynxUnsupportedCommandNumberException
        command.setSerialization(datagram);

        // Remember this guy as someone who needs acknowledgement
        boolean moduleWillReply = command.isAckable() || command.isResponseExpected();
        this.unfinishedCommands.put(msgnumCur, (LynxRespondable)command);

        // Send it on out!
        this.lynxUsbDevice.transmit(command);

        // If the module isn't going to send something back to us in response, then it's finished
        if (!moduleWillReply)
            {
            finishedWithMessage(command);
            }
         }

    @Override public void retransmit(LynxMessage message) throws InterruptedException
        {
        RobotLog.vv(TAG, "retransmitting: mod=%d cmd=0x%02x msg#=%d ref#=%d ", this.getModuleAddress(), message.getCommandNumber(), message.getMessageNumber(), message.getReferenceNumber());
        this.lynxUsbDevice.transmit(message);
        }

    public void finishedWithMessage(LynxMessage message) // must be idempotent
        {
        if (LynxUsbDeviceImpl.DEBUG_LOG_DATAGRAMS_FINISH) RobotLog.vv(TAG, "finishing mod=%d msg#=%d",
                message.getModuleAddress(),
                message.getMessageNumber());
        //
        int messageNumber = message.getMessageNumber();
        this.unfinishedCommands.remove(messageNumber);
        message.forgetSerialization();
        }

    public void pretendFinishExtantCommands() throws InterruptedException
        {
        for (LynxRespondable ackable : this.unfinishedCommands.values())
            {
            ackable.pretendFinish();
            }
        }

    public void onIncomingDatagramReceived(LynxDatagram datagram)
    // We've received a datagram from our module.
        {
        // Reify the incoming command. First, what kind of command is that guy?
        try {
            MessageClassAndCtor pair = this.commandClasses.get(datagram.getCommandNumber());
            if (pair != null)
                {
                // Is it the command itself, or a response to a command of that flavor?
                if (datagram.isResponse())
                    {
                    pair = responseClasses.get(pair.clazz);
                    }
                if (pair != null)
                    {
                    // Instantiate the command or response so we can deserialize
                    LynxMessage incomingMessage = pair.ctor.newInstance(this);

                    // Deserialize
                    incomingMessage.setSerialization(datagram);
                    incomingMessage.loadFromSerialization();

                    if (LynxUsbDeviceImpl.DEBUG_LOG_MESSAGES) RobotLog.vv(TAG, "rec'd: mod=%d cmd=0x%02x(%s) msg#=%d ref#=%d", datagram.getSourceModuleAddress(), datagram.getPacketId(), incomingMessage.getClass().getSimpleName(), incomingMessage.getMessageNumber(), incomingMessage.getReferenceNumber());

                    // Acks&nacks are processed differently than responses
                    if (incomingMessage.isAck() || incomingMessage.isNack())
                        {
                        LynxRespondable ackdCommand = this.unfinishedCommands.get(datagram.getReferenceNumber());
                        if (ackdCommand != null)
                            {
                            // Process the ack or the nack
                            if (incomingMessage.isNack())
                                {
                                ackdCommand.onNackReceived((LynxNack)incomingMessage);
                                }
                            else
                                {
                                ackdCommand.onAckReceived((LynxAck)incomingMessage);
                                }

                            // If we get an ack or a nack, then we WON'T get a response
                            finishedWithMessage(ackdCommand);
                            }
                        else
                            {
                            RobotLog.ee(TAG, "unable to find originating LynxRespondable for mod=%d msg#=%d ref#=%d", datagram.getSourceModuleAddress(), datagram.getMessageNumber(), datagram.getReferenceNumber());
                            }
                        }
                    else
                        {
                        LynxRespondable originatingCommand = this.unfinishedCommands.get(datagram.getReferenceNumber());
                        if (originatingCommand != null)
                            {
                            Assert.assertTrue(incomingMessage.isResponse());

                            // Process the response
                            originatingCommand.onResponseReceived((LynxResponse) incomingMessage);

                            // After a response is received, we're always done with a command
                            finishedWithMessage(originatingCommand);
                            }
                        else
                            {
                            RobotLog.ee(TAG, "unable to find originating command for packetid=0x%04x msg#=%d ref#=%d", datagram.getPacketId(), datagram.getMessageNumber(), datagram.getReferenceNumber());
                            }
                        }
                    }
                }
            else
                {
                RobotLog.ee(TAG, "no command class known for command=0x%02x", datagram.getCommandNumber());
                }
            }
        catch (InstantiationException|IllegalAccessException|InvocationTargetException|RuntimeException e)
            {
            RobotLog.ee(TAG, e, "internal error in LynxModule.noteIncomingDatagramReceived()");
            }
        }

    public void abandonUnfinishedCommands()
        {
        this.unfinishedCommands.clear();
        }

    protected void nackUnfinishedCommands()
        {
        while (!unfinishedCommands.isEmpty())
            {
            for (LynxRespondable respondable : unfinishedCommands.values())
                {
                RobotLog.vv(RobotLog.TAG, "force-nacking unfinished command=%s mod=%d msg#=%d", respondable.getClass().getSimpleName(), respondable.getModuleAddress(), respondable.getMessageNumber());
                LynxNack nack = new LynxNack(this, respondable.isResponseExpected() ? LynxNack.ReasonCode.ABANDONED_WAITING_FOR_RESPONSE : LynxNack.ReasonCode.ABANDONED_WAITING_FOR_ACK);
                respondable.onNackReceived(nack);
                finishedWithMessage(respondable);
                }
            }
        }
    }
