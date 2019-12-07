package org.firstinspires.ftc.onbotjava;

import android.util.ArraySet;

import org.firstinspires.ftc.robotcore.internal.opmode.OnBotJavaHelper;

import java.util.Set;
import java.util.TreeSet;

public class OnBotJavaHelperImpl extends ClassLoader implements OnBotJavaHelper {
    @Override
    public ClassLoader getOnBotJavaClassLoader() {
        return OnBotJavaHelperImpl.class.getClassLoader();
    }

    @Override
    public Set<String> getOnBotJavaClassNames() {
        return new TreeSet<>();
    }

    @Override
    public void close(ClassLoader classLoader) {

    }
}
