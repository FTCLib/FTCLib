/*
Copyright (c) 2018 Robert Atkinson

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
package org.firstinspires.ftc.robotcore.internal.camera;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.qualcomm.robotcore.eventloop.SyncdDevice;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Function;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.LibUsbDevice;
import org.firstinspires.ftc.robotcore.internal.camera.libuvc.nativeobject.UvcDevice;
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.hardware.usb.ArmableUsbDevice;

import java.util.List;
import java.util.concurrent.Executor;

public interface CameraManagerInternal extends CameraManager
    {
    /* In early developmental work, there were paths necessary on KitKat that subsequent work
    * no longer requires (most had to do with serial numbers). Which should we use? */
    boolean avoidKitKatLegacyPaths = true;

    /*If KitKat *has to* use one path, but there's a non-native one post KitKat, should we use it?*/
    boolean useNonKitKatPaths = true;

    /* If false, then on KitKat we use libusb for USB device enumeration */
    boolean forceJavaUsbEnumerationKitKat = true;

    @Nullable UvcDevice findUvcDevice(@NonNull WebcamName cameraName);

    WebcamName webcamNameFromDevice(UsbDevice usbDevice);

    WebcamName webcamNameFromDevice(LibUsbDevice libUsbDevice);

    WebcamName webcamNameFromSerialNumber(@NonNull SerialNumber serialNumber, @NonNull ArmableUsbDevice.OpenRobotUsbDevice opener, @NonNull SyncdDevice.Manager manager);

    boolean isWebcamAttached(@NonNull SerialNumber serialNumberPattern);

    BuiltinCameraName nameFromCameraDirection(VuforiaLocalizer.CameraDirection cameraDirection);

    UsbManager getUsbManager();

    @Nullable SerialNumber getRealOrVendorProductSerialNumber(UsbDevice usbDevice);

    @NonNull List<LibUsbDevice> getMatchingLibUsbDevices(Function<SerialNumber, Boolean> matcher);

    void enumerateAttachedSerialNumbers(Consumer<SerialNumber> consumer);

    Executor getSerialThreadPool();

    interface UsbAttachmentCallback
        {
        void onAttached(UsbDevice usbDevice, SerialNumber serialNumber, MutableReference<Boolean> claimed);
        void onDetached(UsbDevice usbDevice);
        }

    void registerReceiver(UsbAttachmentCallback receiver);
    void unregisterReceiver(UsbAttachmentCallback receiver);
    }
