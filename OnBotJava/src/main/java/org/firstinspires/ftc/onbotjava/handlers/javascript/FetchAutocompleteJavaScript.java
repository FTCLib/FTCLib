/*
 * Copyright (c) 2018 David Sargent
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of David Sargent nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.onbotjava.handlers.javascript;


import org.firstinspires.ftc.onbotjava.OnBotJavaProgrammingMode;
import org.firstinspires.ftc.onbotjava.OnBotJavaWebInterfaceManager;
import org.firstinspires.ftc.onbotjava.RegisterWebHandler;
import org.firstinspires.ftc.onbotjava.StandardResponses;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.onbotjava.OnBotJavaManager;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import fi.iki.elonen.NanoHTTPD;

import static org.firstinspires.ftc.onbotjava.StandardResponses.serverError;

@RegisterWebHandler(uri = OnBotJavaProgrammingMode.URI_JS_AUTOCOMPLETE)
public class FetchAutocompleteJavaScript implements WebHandler {
    private static volatile String response = null;
    private static final Object lock = new Object();

    public FetchAutocompleteJavaScript() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildResponse();
                } catch (Exception e) {
                    RobotLog.ee(FetchAutocompleteJavaScript.class.getName(), e,
                            "Error with autocomplete response");
                    response = "";
                }
            }
        })).start();
    }

    @Override
    public NanoHTTPD.Response getResponse(NanoHTTPD.IHTTPSession session) {
        // Wait for the build response to complete, it is still running
        synchronized (lock) {
            while (response == null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return StandardResponses.serverError();
                }
            }
        }
        // Make sure the result is valid...
        if (response.equals("")) return StandardResponses.serverError();
        return StandardResponses.successfulJsonRequest(response);
    }

    private static void buildResponse() throws IOException {
        final List<File> jarFiles = AppUtil.getInstance().filesIn(OnBotJavaManager.libDir, ".jar");
        jarFiles.addAll(AppUtil.getInstance().filesIn(OnBotJavaManager.jarDir, ".jar"));
        final HashMap<String, List<AutoClass>> autoClassList = new HashMap<>();
        for (File jarFile : jarFiles) {
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                final String entryName = entry.getName();
                // Skip the unimportant classes
                if (!entryName.endsWith(".class")) continue;
                if (!packagesToAutoComplete(entryName)) continue;
                String className = entryName.replaceAll("/", "\\.");
                String myClass = className.substring(0, className.lastIndexOf('.'));
                // A "$" denotes an inner class which we will parse as referenced by the classes we scan
                if (myClass.contains("$")) continue;
                final Class currentClass;
                try {
                    currentClass = Class.forName(myClass, false, FetchAutocompleteJavaScript.class.getClassLoader());
                } catch (ClassNotFoundException ignored) {
                    continue;
                }

                parseClassForAutocomplete(autoClassList, currentClass);
            }
        }

        response = OnBotJavaWebInterfaceManager.instance().gson().toJson(autoClassList);
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private static void parseClassForAutocomplete(HashMap<String, List<AutoClass>> autoClassList, Class currentClass) {
        final AutoClass autoClass;
        final String currentClassName;

        SecurityModifier classModifier = SecurityModifier.fromModifierInt(currentClass.getModifiers());
        if (classModifier != SecurityModifier.PUBLIC && classModifier != SecurityModifier.PROTECTED)
            return;

        currentClassName = classNameFor(currentClass);
        String packageName = currentClass.getPackage().getName();

        // Check if we have already added this class (to prevent recursion loops), if so do nothing more
        if (autoClassList.containsKey(currentClassName)) {
            for (AutoClass klazz : autoClassList.get(currentClassName)) {
                if (klazz.packageName.equals(packageName)) return;
            }
        }

        HashMap<String, AutoField> fields = new HashMap<>();
        for (Field field : currentClass.getDeclaredFields()) {
            SecurityModifier fieldSecurityModifier = SecurityModifier.fromModifierInt(field.getModifiers());
            if (fieldSecurityModifier == SecurityModifier.PRIVATE) continue;
            final String name1 = field.getName();
            final String fieldType = field.getType().getName();
            fields.put(name1, new AutoField(fieldSecurityModifier, fieldType));
        }

        HashMap<String, ArrayList<AutoMethod>> methods = new HashMap<>();
        for (Method method : currentClass.getDeclaredMethods()) {
            SecurityModifier fieldSecurityModifier = SecurityModifier.fromModifierInt(method.getModifiers());
            if (fieldSecurityModifier == SecurityModifier.PRIVATE) continue;
            final String methodName = method.getName();
            final String fieldType = method.getReturnType().getName();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final List<String> paramTypes = new ArrayList<>(parameterTypes.length);
            for (Class<?> paramType : parameterTypes) {
                paramTypes.add(paramType.getName());
            }

            if (!methods.containsKey(methodName)) methods.put(methodName, new ArrayList<AutoMethod>());
            methods.get(methodName)
                    .add(new AutoMethod(fieldSecurityModifier, fieldType,paramTypes));
        }

        final Class superclass = currentClass.getSuperclass();
        final String superclassName;
        if (superclass != null) {
            superclassName = superclass.getName();
            parseClassForAutocomplete(autoClassList, superclass);
        } else {
            superclassName = Object.class.getName();
        }

        List<String> interfaces = getInterfacesFor(currentClass, autoClassList, new ArrayList<String>());

        autoClass = new AutoClass(classModifier, methods, fields, currentClassName, packageName, interfaces, superclassName);
        if (!autoClassList.containsKey(currentClassName))
            autoClassList.put(currentClassName, new ArrayList<AutoClass>());
        autoClassList.get(currentClassName).add(autoClass);

        for (Class<?> innerClass : currentClass.getDeclaredClasses()) {
            parseClassForAutocomplete(autoClassList, innerClass);
        }
    }

    private static List<String> getInterfacesFor(Class<?> currentClass, HashMap<String, List<AutoClass>> autoClassMap, List<String> list) {
        if (currentClass == null || currentClass.equals(Object.class)) return list;

        for (Class<?> klazz : currentClass.getInterfaces()) {
            list.add(klazz.getName());
            parseClassForAutocomplete(autoClassMap, klazz);
        }

        return getInterfacesFor(currentClass.getSuperclass(), autoClassMap, list);
    }

    private static boolean packagesToAutoComplete(String entryName) {
        for (String testPackage : OnBotJavaWebInterfaceManager.packagesToAutocomplete()) {
            if (entryName.startsWith(testPackage)) return true;
        }

        return false;
    }

    private static String classNameFor(Class<?> klazz) {
        final String fullClassName = klazz.getName();
        if (fullClassName.indexOf('.') >= 0) {
            return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        } else {
            return fullClassName;
        }
    }

    @SuppressWarnings("unused")
    private static class AutoClass {
        private SecurityModifier modifier;
        private HashMap<String, ArrayList<AutoMethod>> methods;
        private HashMap<String, AutoField> fields;
        private String packageName;
        private List<String> interfaces;
        private String parentClass;

        private AutoClass(SecurityModifier modifier, HashMap<String, ArrayList<AutoMethod>> methods, HashMap<String, AutoField> fields,
                          String name, String packageName, List<String> interfaces, String parentClass) {
            this.modifier = modifier;
            this.methods = methods;
            this.fields = fields;
            this.packageName = packageName;
            this.interfaces = interfaces;
            this.parentClass = parentClass;
        }
    }

    @SuppressWarnings("unused")
    private static class AutoField {
        private SecurityModifier modifier;
        private String type;

        private AutoField(SecurityModifier modifier, String type) {
            this.modifier = modifier;
            this.type = type;
        }
    }

    @SuppressWarnings("unused")
    private static class AutoMethod {
        private SecurityModifier modifier;
        private String type;
        private List<String> params;

        private AutoMethod(SecurityModifier modifier, String type, List<String> params) {
            this.modifier = modifier;
            this.type = type;
            this.params = params;
        }
    }

    private enum SecurityModifier {
        PUBLIC, PRIVATE, PROTECTED, PACKAGE_PRIVATE, UNKNOWN;

        static SecurityModifier fromModifierInt(int modifier) {
            if (Modifier.isPublic(modifier)) return PUBLIC;
            if (Modifier.isPrivate(modifier)) return PRIVATE;
            if (Modifier.isProtected(modifier)) return PROTECTED;
            return PACKAGE_PRIVATE;
        }
    }
}
