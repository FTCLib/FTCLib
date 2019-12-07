/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firstinspires.ftc.robotcore.internal.opmode;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface OnBotJavaHelper {

    File javaRoot               = new File(AppUtil.FIRST_FOLDER, "/java/");
    File srcDir                 = new File(javaRoot, "/src/");
    File jarDir                 = new File(srcDir,   "/jars/");
    File statusDir              = new File(javaRoot, "/status/");
    File buildSuccessfulFile    = new File(statusDir, "buildSuccessful.txt");

    File controlDir             = new File(javaRoot, "/control/");
    File buildLockDir           = new File(controlDir, "/buildLock/");

    /*
     * This probably needs some rework...
     *
     * Any given instance of an OBJ class loader can only be used once as it closes
     * it's dex files when done.  If you use the class loader to get a list of class
     * names, and then reuse it to try to load those classes, it will not find any classes.
     * Additionally, be careful when caching this class loader as a rebuild of OBJ software
     * produces a _new_ jar which any given cached instance of the class loader will not
     * know about, and hence changes will not appear.
     */
    ClassLoader getOnBotJavaClassLoader();

    Set<String> getOnBotJavaClassNames();

    void close(ClassLoader classLoader);

}
