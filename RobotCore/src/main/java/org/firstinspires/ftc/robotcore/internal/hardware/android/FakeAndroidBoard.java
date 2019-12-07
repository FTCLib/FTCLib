/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
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
package org.firstinspires.ftc.robotcore.internal.hardware.android;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;

import java.io.File;

/**
 * A FakeAndroidBoard means that we don't recognize the Control Hub type. The user will hopefully
 * be able to connect to us, so we want to show them an error message, rather than crashing.
 * Therefore, FakeAndroidBoard should never return a null value or throw an exception.
 * Nothing much will work, but at least it won't crash.
 */
public class FakeAndroidBoard extends AndroidBoard {
    @Override public String getDeviceType() {
        return "Fake Android board";
    }

    @Override public DigitalChannel getAndroidBoardIsPresentPin() {
        return new FakeDigitalChannel(DigitalChannel.Mode.OUTPUT);
    }

    @Override public DigitalChannel getProgrammingPin() {
        return new FakeDigitalChannel(DigitalChannel.Mode.OUTPUT);
    }

    @Override public DigitalChannel getLynxModuleResetPin() {
        return new FakeDigitalChannel(DigitalChannel.Mode.OUTPUT);
    }

    @Override public DigitalChannel getUserButtonPin() {
        return new FakeDigitalChannel(DigitalChannel.Mode.INPUT);
    }

    @Override public File getUartLocation() {
        return new File("/dev/null");
    }

    @Override public boolean supports5GhzAp() {
        return true;
    }

    @Override public boolean hasControlHubUpdater() {
        return false;
    }

    private static class FakeDigitalChannel implements DigitalChannel {
        Mode mode;

        public FakeDigitalChannel(Mode mode) {
            this.mode = mode;
        }

        @Override public Mode getMode() {
            return mode;
        }

        @Override public void setMode(Mode mode) {
        }

        @Override public boolean getState() {
            return false;
        }

        @Override public void setState(boolean state) {

        }

        @Override public void setMode(DigitalChannelController.Mode mode) {

        }

        @Override public Manufacturer getManufacturer() {
            return Manufacturer.Other;
        }

        @Override public String getDeviceName() {
            return "Fake Digital Channel";
        }

        @Override public String getConnectionInfo() {
            return "";
        }

        @Override public int getVersion() {
            return 0;
        }

        @Override public void resetDeviceConfigurationForOpMode() {

        }

        @Override public void close() {

        }
    }
}
