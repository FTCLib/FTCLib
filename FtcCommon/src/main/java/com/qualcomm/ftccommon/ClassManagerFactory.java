/*
 * Copyright (c) 2016 Craig MacFarlane
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

package com.qualcomm.ftccommon;

import com.qualcomm.ftccommon.configuration.RobotConfigFileManager;
import com.qualcomm.ftccommon.configuration.RobotConfigResFilter;
import com.qualcomm.robotcore.hardware.configuration.ConfigurationTypeManager;

import org.firstinspires.ftc.robotcore.external.Supplier;
import org.firstinspires.ftc.robotcore.internal.opmode.AnnotatedOpModeClassFilter;
import org.firstinspires.ftc.robotcore.internal.opmode.ClassManager;

import java.util.Collection;

/**
 * A helper for classes that want to inspect the list of classes packaged with an APK.
 *
 * This is called very early in startup.  Any classes that want to select a subset of the
 * classes packaged with the APK may implement ClassFilter and register with the ClassManager
 * here.
 */
public class ClassManagerFactory {

    /** Register all them that wants to see the classes in our APK */
    public static void registerFilters()
    {
        registerResourceFilters();

        ClassManager classManager = ClassManager.getInstance();
        classManager.registerFilter(AnnotatedOpModeClassFilter.getInstance());
        classManager.registerFilter(ConfigurationTypeManager.getInstance());
    }

    public static void registerResourceFilters()
    {
        ClassManager classManager = ClassManager.getInstance();

        final RobotConfigResFilter idResFilter = new RobotConfigResFilter(RobotConfigFileManager.getRobotConfigTypeAttribute());
        RobotConfigFileManager.setXmlResourceIdSupplier(new Supplier<Collection<Integer>>()
            {
            @Override public Collection<Integer> get()
                {
                return idResFilter.getXmlIds();
                }
            });

        final RobotConfigResFilter idTemplateResFilter = new RobotConfigResFilter(RobotConfigFileManager.getRobotConfigTemplateAttribute());
        RobotConfigFileManager.setXmlResourceTemplateIdSupplier(new Supplier<Collection<Integer>>()
            {
            @Override public Collection<Integer> get()
                {
                return idTemplateResFilter.getXmlIds();
                }
            });

        classManager.registerFilter(idResFilter);
        classManager.registerFilter(idTemplateResFilter);
    }

    public static void processAllClasses()
    {
        ClassManager.getInstance().processAllClasses();
    }
}
