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
package org.firstinspires.ftc.robotcore.external;

import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

/**
 * {@link ClassFactory} provides a means by which various objects in the SDK may be logically
 * instantiated without exposing their external class identities to user's programs.
 */
@SuppressWarnings("WeakerAccess")
public abstract class ClassFactory
    {
    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public static ClassFactory getInstance()
        {
        return InstanceHolder.theInstance;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    /**
     * {@link #createVuforia(VuforiaLocalizer.Parameters) createVuforia} returns
     * an instance of the Vuforia localizer engine configured with the indicated set of parameters.
     *
     * @param parameters the parameters used to configure the instance of the engine
     * @return an instance of the Vuforia robot localization engine.
     *
     * @see VuforiaLocalizer
     * @see org.firstinspires.ftc.robotcore.external.navigation.Orientation
     * @see <a href="http://www.vuforia.com/">vuforia.com</a>
     */
    public abstract VuforiaLocalizer createVuforia(VuforiaLocalizer.Parameters parameters);

    /**
     * Return true if this device is compatible with TensorFlow Object Detection, false otherwise.
     */
    public abstract boolean canCreateTFObjectDetector();

    /**
     * {@link #createTFObjectDetector(TFObjectDetector.Parameters) createTFObjectDetector} returns
     * an instance of the TensorFlow object detector engine configured with the indicated set of parameters.
     *
     * @param parameters the parameters used to configure the instance of the engine
     * @param vuforiaLocalizer the VuforiaLocalizer that will be used to obtain camera frames
     * @return an instance of the TensorFlow object detector engine.
     *
     * @see TFObjectDetector
     */
    public abstract TFObjectDetector createTFObjectDetector(TFObjectDetector.Parameters parameters,
        VuforiaLocalizer vuforiaLocalizer);

    /**
     * Returns a {@link CameraManager} which can be used to access the USB webcams
     * attached to the robot controller.
     * @see CameraManager
     */
    public abstract CameraManager getCameraManager();

    //----------------------------------------------------------------------------------------------
    // Internal
    //----------------------------------------------------------------------------------------------

    /** @deprecated Use {@link #createVuforia(VuforiaLocalizer.Parameters)} instead */
    @Deprecated
    public static VuforiaLocalizer createVuforiaLocalizer(VuforiaLocalizer.Parameters parameters)
        {
        return getInstance().createVuforia(parameters);
        }

    protected static class InstanceHolder
        {
        public static ClassFactory theInstance = null;
        }
    }
