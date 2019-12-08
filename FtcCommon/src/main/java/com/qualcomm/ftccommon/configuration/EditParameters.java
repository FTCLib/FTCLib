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
package com.qualcomm.ftccommon.configuration;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.ControlSystem;
import com.qualcomm.robotcore.hardware.ScannedDevices;
import com.qualcomm.robotcore.hardware.configuration.DeviceConfiguration;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link EditParameters} are the contents of the {@link Intent} bundle that we pass to and
 * from instances of {@link EditActivity}.
 */
@SuppressWarnings("WeakerAccess")
public class EditParameters<ITEM_T extends DeviceConfiguration> implements Serializable
    {
    //------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------

    /**
     * Is the editing of the config file dirty or not. Used for propagating dirty state
     * to and from parent-child configuration editing activities.
     */
    private boolean isConfigDirty = false;

    /**
     * The overall configuration we are to edit, if any
     */
    private DeviceConfiguration configuration = null;

    /**
     * The list of items we are to edit, if any
     */
    private List<ITEM_T> currentItems = null;
    private Class<ITEM_T> itemClass = null;

    /**
     * The initial port number for the items list
     */
    private int initialPortNumber = 0;

    /**
     * If we are editing a list, then this is the size of the list to present
     * to the user, which will always be at least the size of currentItems. If
     * this value is zero, then the editor should uses it's own internal notion
     * of this maximum list size
     */
    private int maxItemCount = 0;

    /**
     * the I2C bus number (only applicable to REV)
     */
    private int i2cBus = 0;

    /**
     * whether the user can grow the size of the list or not
     */
    private boolean growable = false;

    /**
     * what we know to be on the USB bus
     */
    private @NonNull ScannedDevices scannedDevices = new ScannedDevices();

    /**
     * for a USB controller, the entire list of controllers we have attached
     */
    private RobotConfigMap robotConfigMap = new RobotConfigMap();
    private boolean haveRobotConfigMapParameter = false;

    /**
     * the list of robot configurations we know about
     */
    private List<RobotConfigFile> extantRobotConfigurations = new ArrayList<RobotConfigFile>();

    /**
     * the type of Control System that is being edited
     */
    private ControlSystem controlSystem = null;

    /**
     * optional explicit configuration
     */
    private RobotConfigFile currentCfgFile = null;

    //------------------------------------------------------------------------------------------
    // Construction
    //------------------------------------------------------------------------------------------

    public EditParameters(EditActivity editActivity, DeviceConfiguration configuration)
        {
        this(editActivity);
        this.configuration = configuration;
        }

    public EditParameters(EditActivity editActivity, DeviceConfiguration configuration, RobotConfigMap robotConfigMap)
        {
        this(editActivity);
        this.configuration = configuration;
        this.robotConfigMap = robotConfigMap;
        this.haveRobotConfigMapParameter = true;
        }

    public EditParameters(EditActivity editActivity, Class<ITEM_T> itemClass, List<ITEM_T> list)
        {
        this(editActivity);
        setItems(itemClass, list);
        }

    public EditParameters(EditActivity editActivity, DeviceConfiguration configuration, Class<ITEM_T> itemClass, List<ITEM_T> list)
        {
        this(editActivity);
        this.configuration = configuration;
        setItems(itemClass, list);
        }

    public EditParameters(EditActivity editActivity, Class<ITEM_T> itemClass, List<ITEM_T> list, int maxItemCount)
        {
        this(editActivity);
        setItems(itemClass, list);
        this.maxItemCount = maxItemCount;
        }

    private void setItems(Class<ITEM_T> itemClass, List<ITEM_T> list)
        {
        this.itemClass = itemClass;
        this.currentItems = list;
        for (DeviceConfiguration item : list)
            {
            Assert.assertTrue(itemClass.isInstance(item));
            }
        }

    public EditParameters(EditActivity editActivity)
        {
        this.isConfigDirty = editActivity.currentCfgFile.isDirty();
        }

    public EditParameters()
        {
        }

    //------------------------------------------------------------------------------------------
    // Accessors
    //------------------------------------------------------------------------------------------

    public DeviceConfiguration getConfiguration()
        {
        return this.configuration;
        }

    public List<ITEM_T> getCurrentItems()
        {
        return this.currentItems == null ? new LinkedList<ITEM_T>() : this.currentItems;
        }

    public Class<ITEM_T> getItemClass()
        {
        Assert.assertNotNull(this.itemClass);
        return this.itemClass;
        }

    public int getMaxItemCount()
        {
        if (this.currentItems == null)
            return this.maxItemCount;
        else
            return Math.max(this.maxItemCount, this.currentItems.size());
        }

    public boolean isGrowable()
        {
        return this.growable;
        }

    public void setGrowable(boolean growable)
        {
        this.growable = growable;
        }

    public @NonNull ScannedDevices getScannedDevices()
        {
        return this.scannedDevices;
        }

    public void setScannedDevices(@NonNull ScannedDevices devices)
        {
        this.scannedDevices = devices;
        }

    public void setInitialPortNumber(int initialPortNumber)
        {
        this.initialPortNumber = initialPortNumber;
        }

    public int getInitialPortNumber()
        {
        return this.initialPortNumber;
        }

    public int getI2cBus()
        {
        return i2cBus;
        }

    public void setI2cBus(int i2cBus)
        {
        this.i2cBus = i2cBus;
        }

    public RobotConfigMap getRobotConfigMap()
        {
        return this.robotConfigMap;
        }

    public void setRobotConfigMap(RobotConfigMap robotConfigMap)
        {
        this.robotConfigMap = robotConfigMap;
        this.haveRobotConfigMapParameter = true;
        }

    public boolean haveRobotConfigMapParameter()
        {
        return this.haveRobotConfigMapParameter;
        }

    public @NonNull List<RobotConfigFile> getExtantRobotConfigurations()
        {
        return this.extantRobotConfigurations;
        }

    public void setExtantRobotConfigurations(List<RobotConfigFile> configurations)
        {
        this.extantRobotConfigurations = configurations;
        }

    public ControlSystem getControlSystem()
        {
        return controlSystem;
        }

    public void setControlSystem(ControlSystem controlSystem)
        {
        this.controlSystem = controlSystem;
        }

    public RobotConfigFile getCurrentCfgFile()
        {
        return this.currentCfgFile;
        }

    public void setCurrentCfgFile(RobotConfigFile currentCfgFile)
        {
        this.currentCfgFile = currentCfgFile;
        }

    //------------------------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------------------------

    public void putIntent(Intent intent)
        {
        intent.putExtras(this.toBundle());
        }

    public Bundle toBundle()
        {
        Bundle result = new Bundle();

        if (this.configuration != null)
            {
            result.putSerializable("configuration", this.configuration);
            }
        if (this.scannedDevices != null && this.scannedDevices.size() > 0)
            {
            result.putString("scannedDevices", this.scannedDevices.toSerializationString());
            }
        if (this.robotConfigMap != null && this.robotConfigMap.size() > 0)
            {
            result.putSerializable("robotConfigMap", this.robotConfigMap);
            }
        if (this.extantRobotConfigurations != null && this.extantRobotConfigurations.size() > 0)
            {
            result.putString("extantRobotConfigurations", RobotConfigFileManager.serializeXMLConfigList(extantRobotConfigurations));
            }
        if (this.controlSystem != null)
            {
            result.putSerializable("controlSystem", this.controlSystem);
            }
        if (this.currentCfgFile != null)
            {
            result.putString("currentCfgFile", RobotConfigFileManager.serializeConfig(this.currentCfgFile));
            }
        result.putBoolean("haveRobotConfigMap", this.haveRobotConfigMapParameter);
        result.putInt("initialPortNumber", this.initialPortNumber);
        result.putInt("maxItemCount", this.maxItemCount);
        result.putInt("i2cBus", this.i2cBus);
        result.putBoolean("growable", this.growable);
        result.putBoolean("isConfigDirty", this.isConfigDirty);
        if (this.itemClass != null)
            {
            result.putString("itemClass", this.itemClass.getCanonicalName());
            }
        if (this.currentItems != null)
            {
            for (int i = 0; i < currentItems.size(); i++)
                {
                result.putSerializable(String.valueOf(i), currentItems.get(i));
                }
            }
        return result;
        }

    public static @NonNull <RESULT_ITEM extends DeviceConfiguration> EditParameters<RESULT_ITEM> fromIntent(EditActivity editActivity, Intent intent)
        {
        return fromBundle(editActivity, intent.getExtras());
        }

    public static @NonNull <RESULT_ITEM extends DeviceConfiguration> EditParameters<RESULT_ITEM> fromBundle(EditActivity editActivity, Bundle bundle)
        {
        EditParameters<RESULT_ITEM> result = new EditParameters<RESULT_ITEM>();
        if (bundle == null) return result;

        for (String key : bundle.keySet())
            {
            if (key.equals("configuration"))
                {
                result.configuration = (DeviceConfiguration) bundle.getSerializable(key);
                }
            else if (key.equals("scannedDevices"))
                {
                result.scannedDevices = ScannedDevices.fromSerializationString(bundle.getString(key));
                }
            else if (key.equals("robotConfigMap"))
                {
                result.robotConfigMap = (RobotConfigMap)(bundle.getSerializable(key));
                }
            else if (key.equals("haveRobotConfigMap"))
                {
                result.haveRobotConfigMapParameter = bundle.getBoolean(key);
                }
            else if (key.equals("extantRobotConfigurations"))
                {
                result.extantRobotConfigurations = RobotConfigFileManager.deserializeXMLConfigList(bundle.getString(key));
                }
            else if (key.equals("controlSystem"))
                {
                result.controlSystem = (ControlSystem) bundle.getSerializable(key);
                }
            else if (key.equals("currentCfgFile"))
                {
                result.currentCfgFile = RobotConfigFileManager.deserializeConfig(bundle.getString(key));
                }
            else if (key.equals("initialPortNumber"))
                {
                result.initialPortNumber = bundle.getInt(key);
                }
            else if (key.equals("i2cBus"))
                {
                result.i2cBus = bundle.getInt(key);
                }
            else if (key.equals("maxItemCount"))
                {
                result.maxItemCount = bundle.getInt(key);
                }
            else if (key.equals("growable"))
                {
                result.growable = bundle.getBoolean(key);
                }
            else if (key.equals("isConfigDirty"))
                {
                result.isConfigDirty = bundle.getBoolean(key);
                }
            else if (key.equals("itemClass"))
                {
                try {
                    result.itemClass = (Class<RESULT_ITEM>) Class.forName(bundle.getString(key));
                    }
                catch (ClassNotFoundException e)
                    {
                    result.itemClass = null;
                    }
                }
            else
                {
                try
                    {
                    int i = Integer.parseInt(key);
                    RESULT_ITEM dev = (RESULT_ITEM) bundle.getSerializable(key);
                    if (result.currentItems == null)
                        {
                        result.currentItems = new ArrayList<RESULT_ITEM>();
                        }
                    result.currentItems.add(i, dev);
                    }
                catch (NumberFormatException e)
                    {
                    continue;
                    }
                }
            }

        // Propagate dirtiness upon deserialization, both forwards and backwards through the activity stack.
        if (result.isConfigDirty)
            {
            editActivity.currentCfgFile.markDirty();
            }

        return result;
        }
    }
