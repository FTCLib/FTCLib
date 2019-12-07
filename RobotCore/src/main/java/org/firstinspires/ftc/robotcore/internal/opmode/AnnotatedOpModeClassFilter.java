/*
 * Copyright (c) 2015 Robert Atkinson
 *
 *    Ported from the Swerve library by Craig MacFarlane
 *    Based upon contributions and original idea by dmssargent.
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
 * Neither the name of Robert Atkinson, Craig MacFarlane nor the names of their contributors may be used to
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

import android.content.Context;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.AnnotatedOpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.exception.DuplicateNameException;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Does the heavy lifting of managing annotation-based opmode registrations.
 *
 * This is modelled after the following, without which we could not have been successful
 * in this endeavor.
 *      https://github.com/dmssargent/Xtensible-ftc_app/blob/master/FtcRobotController/src/main/java/com/qualcomm/ftcrobotcontroller/opmodes/FtcOpModeRegister.java
 * Many thanks.
 */
@SuppressWarnings("WeakerAccess")
public class AnnotatedOpModeClassFilter implements ClassFilter
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "OpmodeRegistration";

    private Context                                         context;
    private RegisteredOpModes                               registeredOpModes;
    private final String                                    defaultOpModeGroupName = OpModeMeta.DefaultGroup;

    private final        Set<Class<OpMode>>                 filteredAnnotatedOpModeClasses = new HashSet<Class<OpMode>>();
    private final        List<OpModeMetaAndClass>           knownOpModes = new ArrayList<OpModeMetaAndClass>();
    private final        List<OpModeMetaAndClass>           newOpModes = new ArrayList<OpModeMetaAndClass>();
    private final        HashMap<Class, OpModeMetaAndClass> classNameOverrides = new HashMap<Class, OpModeMetaAndClass>();
    private final        Set<Method>                        registrarMethods = new HashSet<Method>();

    private static class InstanceHolder
        {
        public static AnnotatedOpModeClassFilter theInstance = new AnnotatedOpModeClassFilter();
        }
    public static AnnotatedOpModeClassFilter getInstance()
        {
        return InstanceHolder.theInstance;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    private AnnotatedOpModeClassFilter()
        {
        this.registeredOpModes = null;
        this.context       = AppUtil.getDefContext();
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected String resolveDuplicateName(OpModeMetaAndClass opModeMetaAndClass)
        {
            return getOpModeName(opModeMetaAndClass) + "-" + opModeMetaAndClass.clazz.getSimpleName();
        }

    void registerAllClasses(RegisteredOpModes registeredOpModes)
        {
        this.registeredOpModes = registeredOpModes;
        try {
            this.callOpModeRegistrarMethods(new Predicate<Class>()
                {
                @Override public boolean test(Class clazz)
                    {
                    return true;
                    }
                });

            for (Class<OpMode> clazz : filteredAnnotatedOpModeClasses)
                {
                addAnnotatedOpMode(clazz);
                }

            for (OpModeMetaAndClass opModeMetaAndClass : newOpModes)
                {
                String name = getOpModeName(opModeMetaAndClass);
                try
                    {
                    this.registeredOpModes.register(OpModeMeta.forName(name, opModeMetaAndClass.meta), opModeMetaAndClass.clazz);
                    }
                catch (DuplicateNameException e)
                    {
                    name = resolveDuplicateName(opModeMetaAndClass);
                    this.registeredOpModes.register(OpModeMeta.forName(name, opModeMetaAndClass.meta), opModeMetaAndClass.clazz);
                    }
                }
            for (OpModeMetaAndClass opModeMetaAndClass : knownOpModes)
                {
                if (!newOpModes.contains(opModeMetaAndClass))
                    {
                    String name = getOpModeName(opModeMetaAndClass);
                    try
                        {
                        this.registeredOpModes.register(OpModeMeta.forName(name, opModeMetaAndClass.meta), opModeMetaAndClass.clazz);
                        }
                    catch (DuplicateNameException e)
                        {
                        name = resolveDuplicateName(opModeMetaAndClass);
                        this.registeredOpModes.register(OpModeMeta.forName(name, opModeMetaAndClass.meta), opModeMetaAndClass.clazz);
                        }
                    }
                }
            }
        finally
            {
            this.registeredOpModes = null;
            }
        }

    public void registerOnBotJavaClasses(RegisteredOpModes registeredOpModes)
        {
        this.registeredOpModes = registeredOpModes;
        try {
            this.callOpModeRegistrarMethods(new Predicate<Class>()
                {
                @Override public boolean test(Class clazz)
                    {
                    return OnBotJavaDeterminer.isOnBotJava(clazz);
                    }
                });

            for (Class<OpMode> clazz : filteredAnnotatedOpModeClasses)
                {
                if (OnBotJavaDeterminer.isOnBotJava(clazz))
                    {
                    addAnnotatedOpMode(clazz);
                    }
                }

            for (OpModeMetaAndClass opModeMetaAndClass : newOpModes)
                {
                String name = getOpModeName(opModeMetaAndClass);
                this.registeredOpModes.register(OpModeMeta.forName(name, opModeMetaAndClass.meta), opModeMetaAndClass.clazz);
                }
            }
        finally
            {
            this.registeredOpModes = null;
            }
        }


    void reportOpModeConfigurationError(String format, Object... args)
        {
        String message = String.format(format, args);
        // Show the message in the log
        Log.w(TAG, String.format("configuration error: %s", message));
        // Make the message appear on the driver station (only the first one will actually appear)
        RobotLog.setGlobalErrorMsg(message);
        }

    boolean checkOpModeClassConstraints(Class clazz, String opModeName)
        {
        // If the class doesn't extend OpMode, that's an error, we'll ignore the class
        if (!isOpMode(clazz))
            {
            reportOpModeConfigurationError("'%s' class doesn't inherit from the class 'OpMode'", clazz.getSimpleName());
            return false;
            }

        // If it's not 'public', it can't be loaded by the system and won't work. We report
        // the error and ignore the class
        if (!Modifier.isPublic(clazz.getModifiers()))
            {
            reportOpModeConfigurationError("'%s' class is not declared 'public'", clazz.getSimpleName());
            return false;
            }

        // Some opmode names aren't allowed to be used
        if (opModeName == null)
            {
            opModeName = getOpModeName((Class<OpMode>)clazz);
            }
        if (!isLegalOpModeName(opModeName))
            {
            reportOpModeConfigurationError("\"%s\" is not a legal OpMode name", opModeName);
            return false;
            }

        return true;
        }

    /*
     * filter(Class class)
     *
     * The class manager calls us as it iterates through the APK's classes on startup.
     *
     * We will use this opportunity to cache classes/methods that we are interested in
     * for later use.
     *
     * Note that the cached lists are declared static as the class manager
     * may be instantiated with a different instance of this class than the one that is
     * used later when registration is actually performed.  This is fine because there's
     * one and only one list of classes in an APK and it's immutable within any given run
     * of the application. [Note: not so any more; see RobotJavaManager]
     *
     * Doing it this way allows localization of different entities that may be interested
     * in the set of classes packaged in the APK without forcing multiple iterations over the
     * entire set.  See ClassManager usage in event loop initialization.
     */
    @Override
    public void filterClass(Class clazz)
        {
        filterOpModeRegistrarMethods(clazz);

        // Is this an annotated OpMode?
        boolean isTeleOp     = clazz.isAnnotationPresent(TeleOp.class);
        boolean isAutonomous = clazz.isAnnotationPresent(Autonomous.class);

        // If it's neither teleop or autonomous, then it's not interesting to us
        if (!isTeleOp && !isAutonomous)
            return;

        // If we have BOTH Autonomous and TeleOp annotations on a class, that's an error, we'll ignore it.
        if (isTeleOp && isAutonomous)
            {
            reportOpModeConfigurationError("'%s' class is annotated both as 'TeleOp' and 'Autonomous'; please choose at most one", clazz.getSimpleName());
            return;
            }

        // There's some things we need to check about the actual class
        if (!checkOpModeClassConstraints(clazz, null))
            return;

        // If the class has been annotated as @Disabled, then ignore it
        if (clazz.isAnnotationPresent(Disabled.class))
            return;

        filteredAnnotatedOpModeClasses.add((Class<OpMode>) clazz);
        }

    @Override public void filterOnBotJavaClass(Class clazz)
        {
        filterClass(clazz);
        }

    @Override public void filterAllClassesStart()
        {
        newOpModes.clear();

        classNameOverrides.clear();
        knownOpModes.clear();
        filteredAnnotatedOpModeClasses.clear();
        registrarMethods.clear();
        }

    @Override public void filterOnBotJavaClassesStart()
        {
        newOpModes.clear();

        // NB: n-squared implementations, but n is small

        for (OpModeMetaAndClass opModeMetaAndClass : new ArrayList<>(classNameOverrides.values()))
            {
            if (opModeMetaAndClass.isOnBotJava())
                {
                classNameOverrides.remove(opModeMetaAndClass.clazz);
                }
            }

        for (OpModeMetaAndClass opModeMetaAndClass : new ArrayList<>(knownOpModes))
            {
            if (opModeMetaAndClass.isOnBotJava())
                {
                knownOpModes.remove(opModeMetaAndClass);
                }
            }

        for (Class<OpMode> clazz : new ArrayList<>(filteredAnnotatedOpModeClasses))
            {
            if (OnBotJavaDeterminer.isOnBotJava(clazz))
                {
                filteredAnnotatedOpModeClasses.remove(clazz);
                }
            }

        for (Method method : new ArrayList<>(registrarMethods))
            {
            if (OnBotJavaDeterminer.isOnBotJava(method.getDeclaringClass()))
                {
                registrarMethods.remove(method);
                }
            }
        }

    @Override public void filterAllClassesComplete()
        {
        // Nothing to do
        }

    @Override public void filterOnBotJavaClassesComplete()
        {
        // Nothing to do
        }

    /*
     * Does this class have any custom registration annotated methods?
     */
    void filterOpModeRegistrarMethods(Class clazz)
        {
        List<Method> methods = ClassUtil.getLocalDeclaredMethods(clazz);
        for (Method method : methods)
            {
            int requiredModifiers   = Modifier.STATIC | Modifier.PUBLIC;
            int prohibitedModifiers = Modifier.ABSTRACT;
            if (!((method.getModifiers() & requiredModifiers) == requiredModifiers && (method.getModifiers() & prohibitedModifiers) == 0))
                continue;

            if (method.isAnnotationPresent(OpModeRegistrar.class))
                {
                // the 1-parameter version is legacy (just a manager) instead of also a context
                if (getParameterCount(method)==1 || getParameterCount(method)==2)
                    {
                    registrarMethods.add(method);
                    }
                }
            }
        }

    private void callOpModeRegistrarMethods(Predicate<Class> predicate)
        {
        // Call the OpMode registration methods now
        OpModeRegistrarMethodManager manager = new OpModeRegistrarMethodManager();
        for (Method method : registrarMethods)
            {
            if (predicate.test(method.getDeclaringClass()))
                {
                try {
                    // We support both with and without a context for compatibility
                    if (getParameterCount(method)==1)
                        method.invoke(null, manager);
                    else if (getParameterCount(method)==2)
                        method.invoke(null, context, manager);
                    }
                catch (Exception e)
                    {
                    // ignored
                    }
                }
            }
        }

    private int getParameterCount(Method method)
        {
        Class<?>[] parameters = method.getParameterTypes();
        return parameters.length;
        }

    /**
     * An instance of this class is passed to annotated static registration methods
     */
    class OpModeRegistrarMethodManager implements AnnotatedOpModeManager
        {
        public void register(Class clazz)
            {
            if (checkOpModeClassConstraints(clazz, null))
                {
                addAnnotatedOpMode((Class<OpMode>)clazz);
                }
            }

        public void register(String name, Class clazz)
            {
            if (checkOpModeClassConstraints(clazz, name))
                {
                addUserNamedOpMode((Class<OpMode>)clazz, new OpModeMeta(name));
                }
            }

        public void register(OpModeMeta meta, Class clazz)
            {
            if (checkOpModeClassConstraints(clazz, meta.name))
                {
                addUserNamedOpMode((Class<OpMode>)clazz, meta);
                }
            }

        public void register(String name, OpMode opModeInstance)
            {
            // We just go ahead and register this, as there's nothing else to do.
            registeredOpModes.register(name, opModeInstance);
            RobotLog.dd(TAG, String.format("registered instance {%s} as {%s}", opModeInstance.toString(), name));
            }

        public void register(OpModeMeta meta, OpMode opModeInstance)
            {
            // We just go ahead and register this, as there's nothing else to do.
            registeredOpModes.register(meta, opModeInstance);
            RobotLog.dd(TAG, String.format("registered instance {%s} as {%s}", opModeInstance.toString(), meta.name));
            }
        }

    /** add this class, which has opmode annotations, to the map of classes to register */
    private boolean addAnnotatedOpMode(Class<OpMode> clazz)
        {
        if (clazz.isAnnotationPresent(TeleOp.class))
            {
            Annotation annotation = clazz.getAnnotation(TeleOp.class);
            String groupName = ((TeleOp) annotation).group();
            return addOpModeWithGroupName(clazz, OpModeMeta.Flavor.TELEOP, groupName);
            }
        else if (clazz.isAnnotationPresent(Autonomous.class))
            {
            Annotation annotation = clazz.getAnnotation(Autonomous.class);
            String groupName = ((Autonomous) annotation).group();
            return addOpModeWithGroupName(clazz, OpModeMeta.Flavor.AUTONOMOUS, groupName);
            }
        else
            return false;
        }

    private boolean addOpModeWithGroupName(Class<OpMode> clazz, OpModeMeta.Flavor flavor, String groupName)
        {
        OpModeMetaAndClass meta = new OpModeMetaAndClass(new OpModeMeta(flavor, groupName), clazz);
        if (groupName.equals(""))
            return addToOpModeGroup(defaultOpModeGroupName, meta);
        else
            return addToOpModeGroup(groupName, meta);
        }

    /** Add a class for which the user has provided the name as opposed to
     *  the name being taken from the class and its own annotations */
    private boolean addUserNamedOpMode(Class<OpMode> clazz, OpModeMeta meta)
        {
        OpModeMetaAndClass opModeMetaAndClass = new OpModeMetaAndClass(meta, clazz);
        this.classNameOverrides.put(clazz, opModeMetaAndClass);
        return addToOpModeGroup(defaultOpModeGroupName, opModeMetaAndClass);
        }

    /** Add a class to the map under the indicated key */
    private boolean addToOpModeGroup(String groupName, OpModeMetaAndClass opModeMetaAndClass)
        {
        Class<OpMode> clazz = opModeMetaAndClass.clazz;
        opModeMetaAndClass = new OpModeMetaAndClass(OpModeMeta.forGroup(groupName, opModeMetaAndClass.meta), clazz);

        // Have we seen this class before? Don't add if we've already got it placed elsewhere
        if (!isKnown(clazz))
            {
            this.knownOpModes.add(opModeMetaAndClass);
            this.newOpModes.add(opModeMetaAndClass);
            return true;
            }
        else
            return false;
        }

    private boolean isKnown(Class<OpMode> clazz)
        {
        for (OpModeMetaAndClass opModeMetaAndClass : this.knownOpModes)
            {
            if (opModeMetaAndClass.clazz == clazz)
                {
                return true;
                }
            }
        return false;
        }

    private String getOpModeName(OpModeMetaAndClass opModeMetaAndClassData)
        {
        return getOpModeName(opModeMetaAndClassData.clazz);
        }

    /** Returns the name we are to use for this class in the driver station display */
    private String getOpModeName(Class<OpMode> clazz)
        {
        String name;

        if (this.classNameOverrides.containsKey(clazz))
            name = this.classNameOverrides.get(clazz).meta.name;
        else if (clazz.isAnnotationPresent(TeleOp.class))
            name = clazz.getAnnotation(TeleOp.class).name();
        else if (clazz.isAnnotationPresent(Autonomous.class))
            name = clazz.getAnnotation(Autonomous.class).name();
        else
            name = clazz.getSimpleName();

        if (name.trim().equals(""))
            name = clazz.getSimpleName();

        return name;
        }

    private boolean isLegalOpModeName(String name)
        {
        if (name == null)
            return false;
        if ((name.equals(OpModeManager.DEFAULT_OP_MODE_NAME)) ||
            (name.trim().equals("")))
            return false;
        else
            return true;
        }

    private boolean isOpMode(Class clazz)
        {
        return ClassUtil.inheritsFrom(clazz, OpMode.class);
        }

    }
