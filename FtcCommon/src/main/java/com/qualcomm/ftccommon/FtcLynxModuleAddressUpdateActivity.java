/*
Copyright (c) 2017 Robert Atkinson

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
package com.qualcomm.ftccommon;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.qualcomm.ftccommon.configuration.EditActivity;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link FtcLynxModuleAddressUpdateActivity} provides a means by which users can update
 * the (persistently stored) address of a Lynx Module
 */
@SuppressWarnings("WeakerAccess")
public class FtcLynxModuleAddressUpdateActivity extends EditActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcLynxModuleAddressUpdateActivity";
    @Override public String getTag() { return TAG; }

    protected NetworkConnectionHandler              networkConnectionHandler = NetworkConnectionHandler.getInstance();
    protected RecvLoopRunnable.RecvLoopCallback     recvLoopCallback         = new ReceiveLoopCallback();

    protected int                                   msResponseWait           = 10000;   // finding addresses can be slow
    protected BlockingQueue<CommandList.USBAccessibleLynxModulesResp> availableLynxModules = new ArrayBlockingQueue<CommandList.USBAccessibleLynxModulesResp>(1);

    protected List<USBAccessibleLynxModule>         currentModules              = new ArrayList<USBAccessibleLynxModule>();
    protected DisplayedModuleList                   displayedModuleList         = new DisplayedModuleList();

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftc_lynx_address_update);

        networkConnectionHandler.pushReceiveLoopCallback(recvLoopCallback);
        }

    @Override
    protected void onStart()
        {
        super.onStart();

        AppUtil.getInstance().showWaitCursor(getString(R.string.dialogMessagePleaseWait),
            new Runnable()
                {
                @Override public void run()
                    {
                    // this can take a long time, so we run it here in the background
                    currentModules = getUSBAccessibleLynxModules();
                    }
                },
            new Runnable()
                {
                @Override public void run()
                    {
                    displayedModuleList.initialize(currentModules);

                    TextView instructions = (TextView) findViewById(R.id.lynxAddressListInstructions);
                    if (currentModules.isEmpty())
                        {
                        instructions.setText(getString(R.string.lynx_address_instructions_no_devices));
                        }
                    else
                        {
                        instructions.setText(getString(R.string.lynx_address_instructions_update));
                        }
                    }
            });
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        networkConnectionHandler.removeReceiveLoopCallback(recvLoopCallback);
        }

    //----------------------------------------------------------------------------------------------
    // Change management
    //----------------------------------------------------------------------------------------------

    protected class DisplayedModuleList
        {
        protected int                  lastModuleAddressChoice = LynxConstants.MAX_MODULE_ADDRESS_CHOICE;
        protected AddressConfiguration currentAddressConfiguration = new AddressConfiguration();
        protected ViewGroup            moduleList;

        public void initialize(List<USBAccessibleLynxModule> modules)
            {
            moduleList = (ViewGroup) findViewById(R.id.moduleList);
            moduleList.removeAllViews();

            // Keep addresses of things that can't be changed out of the action
            // Note that addresses always includes the 0 for 'no change'
            List<Integer> addresses = new ArrayList<>();
            for (int i = 0; i <= lastModuleAddressChoice; i++)
                {
                addresses.add(i);
                }
            for (USBAccessibleLynxModule module : modules)
                {
                if (!module.isModuleAddressChangeable())
                    {
                    addresses.remove(Integer.valueOf(module.getModuleAddress()));
                    }
                }

            // Keep addresses of things that can't be changed out of the action
            for (USBAccessibleLynxModule module : modules)
                {
                Assert.assertTrue(module.getModuleAddress() != 0);  // these should be pruned by sender (if any)

                // We always need a free module address. Make sure we leave one available
                if (size() + 1 >= addresses.size()-1) break;

                this.add(module, addresses);
                }

            currentAddressConfiguration = new AddressConfiguration(modules);
            }

        protected int size()
            {
            return moduleList.getChildCount();
            }

        protected void add(USBAccessibleLynxModule module, List<Integer> addresses)
            {
            View child = LayoutInflater.from(context).inflate(R.layout.lynx_module_configure_address, null);
            moduleList.addView(child);

            DisplayedModule displayedModule = new DisplayedModule(child);
            displayedModule.initialize(module, addresses);
            }

        protected DisplayedModule from(SerialNumber serialNumber)
            {
            ViewGroup parent = (ViewGroup) findViewById(R.id.moduleList);
            for (int i = 0; i < parent.getChildCount(); i++)
                {
                DisplayedModule displayedModule = new DisplayedModule(parent.getChildAt(i));
                if (displayedModule.getSerialNumber().equals(serialNumber))
                    {
                    return displayedModule;
                    }
                }
            return null;
            }

        public void changeAddress(SerialNumber serialNumber, int newAddress)
            {
            RobotLog.vv(TAG, "changeAddress(%s) from:%d to:%d", serialNumber, currentAddressConfiguration.getCurrentAddress(serialNumber), newAddress);
            if (currentAddressConfiguration.getCurrentAddress(serialNumber) != newAddress)
                {
                // If the new address is already in use within the configuration, then we need to
                // change that use to something else
                SerialNumber existing = currentAddressConfiguration.findByCurrentAddress(newAddress);
                currentAddressConfiguration.putCurrentAddress(serialNumber, newAddress);
                if (existing != null)
                    {
                    int newAddressForExisting = findUnusedAddress();
                    RobotLog.vv(TAG, "conflict with %s: that goes to %d", existing, newAddressForExisting );
                    Assert.assertTrue(newAddressForExisting != 0);
                    currentAddressConfiguration.putCurrentAddress(existing, newAddressForExisting);
                    from(existing).setNewAddress(newAddressForExisting);
                    }
                }
            }

        protected int findUnusedAddress()
            {
            for (int i = 1; i <= lastModuleAddressChoice; i++)
                {
                if (!currentAddressConfiguration.containsCurrentAddress(i))
                    {
                    return i;
                    }
                }
            return 0;
            }
        }

    protected class DisplayedModule
        {
        View view;
        Spinner spinner;

        public DisplayedModule(View view)
            {
            this.view = view;
            this.spinner = (Spinner) view.findViewById(R.id.spinnerChooseAddress);
            }

        public SerialNumber getSerialNumber()
            {
            TextView serial = (TextView) view.findViewById(R.id.moduleSerialText);
            return (SerialNumber)serial.getTag();
            }

        public void initialize(USBAccessibleLynxModule module, List<Integer> addresses)
            {
            TextView serial = (TextView) view.findViewById(R.id.moduleSerialText);
            serial.setText(module.getSerialNumber().toString());
            serial.setTag(module.getSerialNumber()); // so we can find it later

            final TextView address = (TextView) view.findViewById(R.id.moduleAddressText);
            address.setText(getString(R.string.lynx_address_format_module_address, module.getModuleAddress()));

            boolean changeable = module.isModuleAddressChangeable();
            spinner.setEnabled(changeable);
            initializeSpinnerList(spinner, addresses, changeable);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                @Override public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
                    {
                    AddressAndDisplayName item = (AddressAndDisplayName)adapterView.getItemAtPosition(position);
                    int newAddress = item.address;
                    if (newAddress == getStartingAddress())
                        {
                        selectNoChange();
                        }
                    else if (newAddress == 0)
                        {
                        newAddress = getStartingAddress();
                        }
                    displayedModuleList.changeAddress(getSerialNumber(), newAddress);
                    }
                @Override public void onNothingSelected(AdapterView<?> adapterView)
                    {
                    }
                });
            }

        public void setNewAddress(int newAddress)
            {
            RobotLog.vv(TAG, "setNewAddress(%s)=%d", getSerialNumber(), newAddress);
            if (newAddress == getStartingAddress())
                {
                selectNoChange();
                }
            else
                {
                for (int i = 0; i < spinner.getAdapter().getCount(); i++)
                    {
                    if (getItem(i).address == newAddress)
                        {
                        spinner.setSelection(i);
                        return;
                        }
                    }
                }
            }

        protected void selectNoChange()
            {
            RobotLog.vv(TAG, "selectNoChange(%s)", getSerialNumber());
            spinner.setSelection(0);
            }

        protected AddressAndDisplayName getItem(int position)
            {
            return (AddressAndDisplayName) spinner.getAdapter().getItem(position);
            }

        public int getCurrentAddress()
            {
            return displayedModuleList.currentAddressConfiguration.getCurrentAddress(getSerialNumber());
            }

        public int getStartingAddress()
            {
            return displayedModuleList.currentAddressConfiguration.getStartingAddress(getSerialNumber());
            }

        protected void initializeSpinnerList(Spinner spinner, List<Integer> addresses, boolean changeable)
            {
            AddressAndDisplayName[] pairs = new AddressAndDisplayName[addresses.size()];
            for (int i = 0; i < addresses.size(); i++)
                {
                pairs[i] = new AddressAndDisplayName(addresses.get(i), changeable);
                }
            Arrays.sort(pairs);
            ArrayAdapter<AddressAndDisplayName> newAdapter = new ArrayAdapter<AddressAndDisplayName>(FtcLynxModuleAddressUpdateActivity.this, R.layout.lynx_module_configure_address_spin_item, pairs);
            spinner.setAdapter(newAdapter);
            }
        }

    protected class AddressAndDisplayName implements Comparable<AddressAndDisplayName>
        {
        public final String displayName;
        public final int    address;

        public AddressAndDisplayName(int address, boolean changeable)
            {
            this.address = address;
            this.displayName = address==0
                    ? getString(changeable ? R.string.lynx_address_format_no_change : R.string.lynx_address_format_not_changeable)
                    : getString(R.string.lynx_address_format_new_module_address, address);
            }

        @Override public String toString()
            {
            return this.displayName;
            }

        @Override public int compareTo(@NonNull AddressAndDisplayName another)
            {
            return this.address - another.address;
            }
        }

    protected boolean isDirty()
        {
        for (USBAccessibleLynxModule module : currentModules)
            {
            DisplayedModule displayedModule = displayedModuleList.from(module.getSerialNumber());
            if (displayedModule.getStartingAddress() != displayedModule.getCurrentAddress())
                {
                return true;
                }
            }
        return false;
        }

    //----------------------------------------------------------------------------------------------
    // Updating
    //----------------------------------------------------------------------------------------------

    DialogInterface.OnClickListener doNothingAndCloseListener = new DialogInterface.OnClickListener()
        {
        public void onClick(DialogInterface dialog, int button)
            {
            // Do nothing. Dialog will dismiss itself upon return.
            }
        };

    public void onDoneButtonPressed(View view)
        {
        RobotLog.vv(TAG, "onDoneButtonPressed()");
        ArrayList<CommandList.LynxAddressChangeRequest.AddressChange> modulesToChange = new ArrayList<CommandList.LynxAddressChangeRequest.AddressChange>();
        for (USBAccessibleLynxModule module : currentModules)
            {
            DisplayedModule displayedModule = displayedModuleList.from(module.getSerialNumber());
            if (displayedModule.getStartingAddress() != displayedModule.getCurrentAddress())
                {
                CommandList.LynxAddressChangeRequest.AddressChange addressChange = new CommandList.LynxAddressChangeRequest.AddressChange();
                addressChange.serialNumber = displayedModule.getSerialNumber();
                addressChange.oldAddress = displayedModule.getStartingAddress();
                addressChange.newAddress = displayedModule.getCurrentAddress();
                modulesToChange.add(addressChange);
                }
            }

        if (currentModules.size() > 0)
            {
            if (modulesToChange.size() > 0)
                {
                CommandList.LynxAddressChangeRequest request = new CommandList.LynxAddressChangeRequest();
                request.modulesToChange = modulesToChange;
                sendOrInject(new Command(CommandList.CMD_LYNX_ADDRESS_CHANGE, request.serialize()));
                }
            else
                {
                AppUtil.getInstance().showToast(UILocation.BOTH, getString(R.string.toastLynxAddressChangeNothingToDo));
                }
            }

        finishOk();
        }

    public void onCancelButtonPressed(View view)
        {
        RobotLog.vv(TAG, "onCancelButtonPressed()");
        doBackOrCancel();
        }

    @Override public void onBackPressed()
        {
        RobotLog.vv(TAG, "onBackPressed()");
        doBackOrCancel();
        }

    protected void doBackOrCancel()
        {
        if (this.isDirty())
            {
            DialogInterface.OnClickListener exitWithoutSavingButtonListener = new DialogInterface.OnClickListener()
                {
                @Override public void onClick(DialogInterface dialog, int which)
                    {
                    finishCancel();
                    }
                };

            AlertDialog.Builder builder = utility.buildBuilder(getString(R.string.saveChangesTitle), getString(R.string.saveChangesMessageScreen));
            builder.setPositiveButton(R.string.buttonExitWithoutSaving, exitWithoutSavingButtonListener);
            builder.setNegativeButton(R.string.buttonNameCancel, doNothingAndCloseListener);
            builder.show();
            }
        else
            {
            finishCancel();
            }
        }

    protected class AddressConfiguration
        {
        protected Map<SerialNumber, Integer> starting = new ConcurrentHashMap<SerialNumber,Integer>();
        protected Map<SerialNumber, Integer> current  = new ConcurrentHashMap<SerialNumber,Integer>();

        public AddressConfiguration()
            {
            }

        public AddressConfiguration(List<USBAccessibleLynxModule> modules)
            {
            for (USBAccessibleLynxModule module : modules)
                {
                starting.put(module.getSerialNumber(), module.getModuleAddress());
                current.put(module.getSerialNumber(), module.getModuleAddress());
                }
            }

        public int getStartingAddress(SerialNumber serialNumber)
            {
            return starting.get(serialNumber);
            }

        public boolean containsCurrentAddress(int address)
            {
            return current.values().contains(address);
            }

        public void putCurrentAddress(SerialNumber serialNumber, int address)
            {
            current.put(serialNumber, address);
            }

        public int getCurrentAddress(SerialNumber serialNumber)
            {
            return current.get(serialNumber);
            }

        public @Nullable SerialNumber findByCurrentAddress(int address)
            {
            for (Map.Entry<SerialNumber,Integer> pair : current.entrySet())
                {
                if (pair.getValue().equals(address))
                    {
                    return pair.getKey();
                    }
                }
            return null;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Networking
    //----------------------------------------------------------------------------------------------

    protected class ReceiveLoopCallback extends RecvLoopRunnable.DegenerateCallback
        {
        @Override public CallbackResult commandEvent(Command command) throws RobotCoreException
            {
            switch (command.getName())
                {
                case CommandList.CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP:
                    CommandList.USBAccessibleLynxModulesResp serialNumbers = CommandList.USBAccessibleLynxModulesResp.deserialize(command.getExtra());
                    availableLynxModules.offer(serialNumbers);
                    return CallbackResult.HANDLED;
                }
            return super.commandEvent(command);
            }
        }

    protected List<USBAccessibleLynxModule> getUSBAccessibleLynxModules()
        {
        CommandList.USBAccessibleLynxModulesRequest request = new CommandList.USBAccessibleLynxModulesRequest();
        CommandList.USBAccessibleLynxModulesResp result = new CommandList.USBAccessibleLynxModulesResp();

        // Send the command
        availableLynxModules.clear();
        request.forFirmwareUpdate = true;
        sendOrInject(new Command(CommandList.CMD_GET_USB_ACCESSIBLE_LYNX_MODULES, request.serialize()));

        // Wait, but only a while, for  the result
        result = awaitResponse(availableLynxModules, result);

        RobotLog.vv(TAG, "found %d lynx modules", result.modules.size());
        return result.modules;
        }

    protected <T> T awaitResponse(BlockingQueue<T> queue, T defaultResponse)
        {
        return awaitResponse(queue, defaultResponse, msResponseWait, TimeUnit.MILLISECONDS);
        }

    protected <T> T awaitResponse(BlockingQueue<T> queue, T defaultResponse, long time, TimeUnit timeUnit)
        {
        try {
        T cur = queue.poll(time, timeUnit);
        if (cur != null)
            {
            return cur;
            }
        }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return defaultResponse;
        }

    }
