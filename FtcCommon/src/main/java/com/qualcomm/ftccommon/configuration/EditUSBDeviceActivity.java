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
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import android.view.View;

import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.hardware.DeviceManager;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.util.List;
import java.util.Map;

/**
 * {@link EditUSBDeviceActivity} handles the swapping and fixing of USB device configurations
 */
public class EditUSBDeviceActivity extends EditActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

	@Override public String getTag() { return this.getClass().getSimpleName(); }

    protected ScannedDevices extraUSBDevices = new ScannedDevices();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public EditUSBDeviceActivity()
        {
        }

    protected void deserialize(EditParameters parameters)
        {
        super.deserialize(parameters);
        determineExtraUSBDevices();
        }

    //----------------------------------------------------------------------------------------------
    // Fixing and swapping
    //----------------------------------------------------------------------------------------------

    protected void swapConfiguration()
        {
        if (getRobotConfigMap().isSwappable(controllerConfiguration, scannedDevices, this))
            {
            EditParameters parameters = new EditParameters(this, controllerConfiguration);
            parameters.setRobotConfigMap(getRobotConfigMap());
            parameters.setScannedDevices(scannedDevices);
            handleLaunchEdit(EditSwapUsbDevices.requestCode, EditSwapUsbDevices.class, parameters);
            }
        }

    protected boolean completeSwapConfiguration(int requestCodeValue, int resultCode, Intent data)
        {
        if (resultCode == RESULT_OK)
            {
            RequestCode requestCode = RequestCode.fromValue(requestCodeValue);
            if (requestCode == EditSwapUsbDevices.requestCode)
                {
                // Find out whom the user picked to swap with. Be careful about object identities
                EditParameters returnedParameters = EditParameters.fromIntent(this, data);
                SerialNumber swappeeSerialNumber = ((ControllerConfiguration)returnedParameters.getConfiguration()).getSerialNumber();

                ControllerConfiguration swappee = getRobotConfigMap().get(swappeeSerialNumber);
                if (swappee != null)
                    {
                    // He swapped with something already in the configuration
                    robotConfigMap.swapSerialNumbers(controllerConfiguration, swappee);
                    }
                else
                    {
                    // He must have swapped with an extra device
                    robotConfigMap.setSerialNumber(controllerConfiguration, swappeeSerialNumber);
                    controllerConfiguration.setKnownToBeAttached(true);
                    }

                // Adjust 'extraDevices' to accommodate the swap
                determineExtraUSBDevices();

                // Update the UI
                refreshAfterSwap();

                return true;
                }
            }
        return false;
        }

    protected void fixConfiguration()
        {
        SerialNumber candidate = getFixableCandidate();
        boolean isFixable = candidate != null;
        if (isFixable)
            {
            robotConfigMap.setSerialNumber(controllerConfiguration, candidate);
            controllerConfiguration.setKnownToBeAttached(true);
            determineExtraUSBDevices();
            }
        else
            {
            String format = getString(R.string.fixFailNoneAvailable);
            String name   = controllerConfiguration.getName();
            String type   = displayNameOfConfigurationType(ConfigurationType.DisplayNameFlavor.Normal, controllerConfiguration.getConfigurationType());
            appUtil.showToast(UILocation.ONLY_LOCAL, String.format(format, name, type));
            }

        refreshAfterFix();
        }

    protected @Nullable SerialNumber getFixableCandidate()
        {
        SerialNumber candidate = null;

        // If it's already attached, no fixing is needed
        if (controllerConfiguration.isKnownToBeAttached())
            {
            return null;
            }

        boolean isFixable = false;
        DeviceManager.UsbDeviceType deviceType = controllerConfiguration.toUSBDeviceType();

        // Match up extraDevices by type
        for (Map.Entry<SerialNumber,DeviceManager.UsbDeviceType> pair : extraUSBDevices.entrySet())
            {
            if (pair.getValue() == deviceType)
                {
                if (candidate != null)
                    {
                    // More than one candidate: ambiguous
                    isFixable = false;
                    break;
                    }
                // Found a first candidate: fixable
                candidate = pair.getKey();
                isFixable = true;
                }
            }
        return isFixable ? candidate : null;
        }

    // If we were to attempt to fix this configuration, would it work?
    protected boolean isFixable()
        {
        return getFixableCandidate() != null;
        }

    protected boolean isSwappable()
        {
        List<ControllerConfiguration> swapCandidates = getRobotConfigMap().getEligibleSwapTargets(controllerConfiguration, scannedDevices, this);
        SerialNumber fixCandidate = getFixableCandidate();

        // We need swap candidates, but not just the one that we'd automatically 'fix' to
        return !swapCandidates.isEmpty() &&
                (fixCandidate==null
                        || !(swapCandidates.size()==1
                            && swapCandidates.get(0).getSerialNumber().equals(fixCandidate)));
        }

    //----------------------------------------------------------------------------------------------
    // Fixing and swapping support
    //----------------------------------------------------------------------------------------------

    protected void refreshSerialNumber()
        {
        // subclasses to override
        }

    protected void refreshAfterFix()
        {
        showFixSwapButtons();
        currentCfgFile.markDirty();
        robotConfigFileManager.updateActiveConfigHeader(currentCfgFile);
        }

    protected void refreshAfterSwap()
        {
        showFixSwapButtons();
        currentCfgFile.markDirty();
        robotConfigFileManager.updateActiveConfigHeader(currentCfgFile);
        }

    protected void showFixSwapButtons()
        {
        showFixButton(isFixable());
        showSwapButton(isSwappable());
        refreshSerialNumber();
        }

    protected void showFixButton(boolean show)
        {
        showButton(idFixButton, show);
        }

    protected void showSwapButton(boolean show)
        {
        showButton(idSwapButton, show);
        }

    protected void showButton(@IdRes int id, boolean show)
        {
        View button = findViewById(id);
        if (button != null)
            {
            button.setVisibility(show ? View.VISIBLE: View.GONE);
            }
        }

    protected void determineExtraUSBDevices()
        {
        // The extra devices are the ones that are on the USB bus but aren't in the current config map
        extraUSBDevices = new ScannedDevices(scannedDevices);
        for (SerialNumber serialNumber : getRobotConfigMap().serialNumbers())
            {
            extraUSBDevices.remove(serialNumber);
            }
        for (ControllerConfiguration controllerConfiguration : getRobotConfigMap().controllerConfigurations())
            {
            controllerConfiguration.setKnownToBeAttached(scannedDevices.containsKey(controllerConfiguration.getSerialNumber()));
            }
        }


    }
