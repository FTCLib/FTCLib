package org.firstinspires.ftc.robotcore.internal.network;

import com.qualcomm.robotcore.util.Device;

public class PasswordManagerFactory {

    protected static PasswordManager passwordManager = null;

    public static PasswordManager getInstance()
    {
        if (passwordManager == null) {
            if (Device.isRevControlHub() == true) {
                passwordManager = new ControlHubPasswordManager();
            } else {
                passwordManager = new DegeneratePasswordManager();
            }
        }
        return passwordManager;
    }
}
