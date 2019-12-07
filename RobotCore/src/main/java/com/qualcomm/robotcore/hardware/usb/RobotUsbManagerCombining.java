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
package com.qualcomm.robotcore.hardware.usb;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RobotUsbManagerCombining} merges together result from zero or more other managers
 */
@SuppressWarnings("WeakerAccess")
public class RobotUsbManagerCombining implements RobotUsbManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "RobotUsbManagerCombining";

    protected class ManagerInfo
        {
        public RobotUsbManager  manager;
        public int              scanCount;
        }

    protected List<ManagerInfo> managers;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobotUsbManagerCombining()
        {
        this.managers = new ArrayList<ManagerInfo>();
        }

    public void addManager(RobotUsbManager manager)
        {
        ManagerInfo info = new ManagerInfo();
        info.manager = manager;
        info.scanCount = 0;
        this.managers.add(info);
        }

    //----------------------------------------------------------------------------------------------
    // RobotUsbManager
    //----------------------------------------------------------------------------------------------

    @Override public synchronized List<SerialNumber> scanForDevices() throws RobotCoreException
        {
        List<SerialNumber> result = new ArrayList<>();
        for (ManagerInfo info : managers)
            {
            List<SerialNumber> local = null;
            try {
                local = info.manager.scanForDevices();
                }
            catch (RobotCoreException e)
                {
                continue;
                }
            result.addAll(local);
            }
        return result;
        }

    @Override public synchronized RobotUsbDevice openBySerialNumber(SerialNumber serialNumber) throws RobotCoreException
        {
        RobotUsbDevice result = null;
        for (ManagerInfo info : managers)
            {
            try {
                result = info.manager.openBySerialNumber(serialNumber);
                if (result != null)
                    {
                    break;
                    }
                }
            catch (RobotCoreException e)
                {
                // ignore: we'll throw below at the end if needed
                }
            }
        if (result == null)
            {
            throw new RobotCoreException("Combiner unable to open device with serialNumber = " + serialNumber);
            }
        return result;
        }
    }
