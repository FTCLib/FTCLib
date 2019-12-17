/* Copyright (c) 2014, 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftccommon;

import android.support.annotation.NonNull;
import android.util.Base64;

import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * List of RobotCore Robocol commands used by the FIRST apps
 */
@SuppressWarnings("WeakerAccess")
public class CommandList extends RobotCoreCommandList {

  //------------------------------------------------------------------------------------------------
  // Opmodes
  //------------------------------------------------------------------------------------------------

  public static final String CMD_INIT_OP_MODE = "CMD_INIT_OP_MODE";
  public static final String CMD_RUN_OP_MODE = "CMD_RUN_OP_MODE";

  //------------------------------------------------------------------------------------------------
  // Configurations
  //------------------------------------------------------------------------------------------------

  public static final String CMD_RESTART_ROBOT = "CMD_RESTART_ROBOT";

  public static final String CMD_SCAN = "CMD_SCAN";
  public static final String CMD_SCAN_RESP = "CMD_SCAN_RESP";

  public static final String CMD_REQUEST_CONFIGURATIONS = "CMD_REQUEST_CONFIGURATIONS";
  public static final String CMD_REQUEST_CONFIGURATIONS_RESP = "CMD_REQUEST_CONFIGURATIONS_RESP";

  public static final String CMD_REQUEST_CONFIGURATION_TEMPLATES = "CMD_REQUEST_CONFIGURATION_TEMPLATES";
  public static final String CMD_REQUEST_CONFIGURATION_TEMPLATES_RESP = "CMD_REQUEST_CONFIGURATION_TEMPLATES_RESP";

  public static final String CMD_REQUEST_PARTICULAR_CONFIGURATION = "CMD_REQUEST_PARTICULAR_CONFIGURATION"; // also works for (resource-based) templates
  public static final String CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP = "CMD_REQUEST_PARTICULAR_CONFIGURATION_RESP";

  public static final String CMD_ACTIVATE_CONFIGURATION = "CMD_ACTIVATE_CONFIGURATION";

  public static final String CMD_SAVE_CONFIGURATION = "CMD_SAVE_CONFIGURATION";
  public static final String CMD_DELETE_CONFIGURATION = "CMD_DELETE_CONFIGURATION";

  public static final String CMD_DISCOVER_LYNX_MODULES = "CMD_DISCOVER_LYNX_MODULES";
  public static final String CMD_DISCOVER_LYNX_MODULES_RESP = "CMD_DISCOVER_LYNX_MODULES_RESP";

  //------------------------------------------------------------------------------------------------
  // Networking
  //------------------------------------------------------------------------------------------------

  public static final String CMD_REQUEST_REMEMBERED_GROUPS = "CMD_REQUEST_REMEMBERED_GROUPS";
  public static final String CMD_REQUEST_REMEMBERED_GROUPS_RESP = "CMD_REQUEST_REMEMBERED_GROUPS_RESP";

  //------------------------------------------------------------------------------------------------
  // Sounds
  //------------------------------------------------------------------------------------------------

  public static class CmdPlaySound {
    public static final String Command = "CMD_PLAY_SOUND";
    public final long msPresentationTime;
    public final String hashString;
    public final boolean waitForNonLoopingSoundsToFinish;
    public final float volume;
    public final int loopControl;
    public final float rate;

    public CmdPlaySound(long msPresentationTime, String hashString, SoundPlayer.PlaySoundParams params) {
      this.msPresentationTime = msPresentationTime;
      this.hashString = hashString;
      this.waitForNonLoopingSoundsToFinish = params.waitForNonLoopingSoundsToFinish;
      this.volume = params.volume;
      this.loopControl = params.loopControl;
      this.rate = params.rate;
    }
    public String serialize()
        {
        return SimpleGson.getInstance().toJson(this);
        }
    public static CmdPlaySound deserialize(String serialized) { return SimpleGson.getInstance().fromJson(serialized, CmdPlaySound.class); }
    public SoundPlayer.PlaySoundParams getParams() {
      SoundPlayer.PlaySoundParams result = new SoundPlayer.PlaySoundParams();
      result.waitForNonLoopingSoundsToFinish = this.waitForNonLoopingSoundsToFinish;
      result.volume = this.volume;
      result.loopControl = this.loopControl;
      result.rate = this.rate;
      return result;
    }
  }

  public static class CmdRequestSound {
    public static final String Command = "CMD_REQUEST_SOUND";
    public final String hashString;
    public final int port;

    public CmdRequestSound(String hashString, int port) { this.hashString = hashString; this.port = port;}
    public String serialize()
        {
        return SimpleGson.getInstance().toJson(this);
        }
    public static CmdRequestSound deserialize(String serialized) { return SimpleGson.getInstance().fromJson(serialized, CmdRequestSound.class); }
  }

  public static class CmdStopPlayingSounds {
    public static final String Command = "CMD_STOP_PLAYING_SOUNDS";
    public final SoundPlayer.StopWhat stopWhat;
    public CmdStopPlayingSounds(SoundPlayer.StopWhat stopWhat) { this.stopWhat = stopWhat; }
    public String serialize()
        {
        return SimpleGson.getInstance().toJson(this);
        }
    public static CmdStopPlayingSounds deserialize(String serialized) { return SimpleGson.getInstance().fromJson(serialized, CmdStopPlayingSounds.class); }
  }
  
  //------------------------------------------------------------------------------------------------
  // Programming and management
  //------------------------------------------------------------------------------------------------

  /**
   * Command to start Program and Manage mode.
   */
  public static final String CMD_START_DS_PROGRAM_AND_MANAGE = "CMD_START_DS_PROGRAM_AND_MANAGE";

  /**
   * Response to a command to start Program and Manage mode.
   *
   * Connection information will be in extra data.
   */
  public static final String CMD_START_DS_PROGRAM_AND_MANAGE_RESP = "CMD_START_DS_PROGRAM_AND_MANAGE_RESP";

  public static final String CMD_SET_MATCH_NUMBER = "CMD_SET_MATCH_NUMBER";

  //------------------------------------------------------------------------------------------------
  // Lynx firmware update support
  //------------------------------------------------------------------------------------------------

  public static final String CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES = "CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES";
  public static final String CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES_RESP = "CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES_RESP";

  public static class LynxFirmwareImagesResp {
    /** used to prompt user as to where to load images for updating */
    File firstFolder = AppUtil.FIRST_FOLDER;
    /** currently available images. files or assets. */
    ArrayList<FWImage> firmwareImages = new ArrayList<FWImage>();

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static LynxFirmwareImagesResp deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareImagesResp.class);
    }
  }
  public static final String CMD_GET_USB_ACCESSIBLE_LYNX_MODULES = "CMD_GET_USB_ACCESSIBLE_LYNX_MODULES";
  public static class USBAccessibleLynxModulesRequest {
    public boolean forFirmwareUpdate = false;

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static USBAccessibleLynxModulesRequest deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, USBAccessibleLynxModulesRequest.class);
    }
  }
  public static final String CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP = "CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP";

  public static class USBAccessibleLynxModulesResp
    {
    ArrayList<USBAccessibleLynxModule> modules = new ArrayList<USBAccessibleLynxModule>();

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static USBAccessibleLynxModulesResp deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, USBAccessibleLynxModulesResp.class);
    }
  }

  public static final String CMD_LYNX_FIRMWARE_UPDATE = "CMD_LYNX_FIRMWARE_UPDATE";
  public static class LynxFirmwareUpdate {

    SerialNumber serialNumber;
    FWImage firmwareImageFile;

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static LynxFirmwareUpdate deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareUpdate.class);
    }
  }
  public static final String CMD_LYNX_FIRMWARE_UPDATE_RESP = "CMD_LYNX_FIRMWARE_UPDATE_RESP";
  public static class LynxFirmwareUpdateResp {

    boolean success;

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static LynxFirmwareUpdateResp deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, LynxFirmwareUpdateResp.class);
    }
  }

  public static final String CMD_LYNX_ADDRESS_CHANGE = "CMD_LYNX_ADDRESS_CHANGE";
  public static class LynxAddressChangeRequest {

    public static class AddressChange {
      SerialNumber serialNumber;
      int oldAddress;
      int newAddress;
    }

    ArrayList<AddressChange> modulesToChange;

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }
    public static LynxAddressChangeRequest deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, LynxAddressChangeRequest.class);
    }
  }

  public static class CmdVisuallyIdentify {
    public static final String Command = "CMD_VISUALLY_IDENTIFY";
    public final @NonNull SerialNumber serialNumber;
    public final boolean shouldIdentify;

    public CmdVisuallyIdentify(@NonNull SerialNumber serialNumber, boolean shouldIdentify) {
      this.serialNumber = serialNumber;
      this.shouldIdentify = shouldIdentify;
    }

    public String serialize() {
      return SimpleGson.getInstance().toJson(this);
    }

    public static CmdVisuallyIdentify deserialize(String serialized) {
      return SimpleGson.getInstance().fromJson(serialized, CmdVisuallyIdentify.class);
    }
  }
}
