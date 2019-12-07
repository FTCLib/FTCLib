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
package com.qualcomm.ftccommon;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.ReadWriteFile;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.internal.collections.MutableReference;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.CallbackResult;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RecvLoopRunnable;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList.FWImage;
import org.firstinspires.ftc.robotcore.internal.stellaris.FlashLoaderManager;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.robotcore.internal.ui.ThemedActivity;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link FtcLynxFirmwareUpdateActivity} manages the updating of the firmware on
 * all available attached lynx modules.
 */
@SuppressWarnings("WeakerAccess")
public class FtcLynxFirmwareUpdateActivity extends ThemedActivity
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcLynxFirmwareUpdateActivity";
    @Override public String getTag() { return TAG; }
    @Override protected FrameLayout getBackBar() { return findViewById(org.firstinspires.inspection.R.id.backbar); }

    protected NetworkConnectionHandler              networkConnectionHandler = NetworkConnectionHandler.getInstance();
    protected RecvLoopRunnable.RecvLoopCallback     recvLoopCallback        = new ReceiveLoopCallback();
    protected boolean                               remoteConfigure         = AppUtil.getInstance().isDriverStation();
    protected int                                   msResponseWait          = 5000;     // a very generous time
    protected FWImage                               firmwareImageFile       = new FWImage(new File(""), false);
    protected List<USBAccessibleLynxModule>         modulesToUpdate         = new ArrayList<USBAccessibleLynxModule>();
    protected boolean                               enableUpdateButton      = true;
    protected boolean                               cancelUpdate            = false;

    protected BlockingQueue<CommandList.LynxFirmwareImagesResp>       availableLynxImages    = new ArrayBlockingQueue<CommandList.LynxFirmwareImagesResp>(1);
    protected BlockingQueue<CommandList.USBAccessibleLynxModulesResp> availableLynxModules   = new ArrayBlockingQueue<CommandList.USBAccessibleLynxModulesResp>(1);
    protected BlockingQueue<CommandList.LynxFirmwareUpdateResp>       availableFWUpdateResps = new ArrayBlockingQueue<CommandList.LynxFirmwareUpdateResp>(1);

    //----------------------------------------------------------------------------------------------
    // Initialization
    //----------------------------------------------------------------------------------------------

    public static void initializeDirectories()
        {
        // Create the director for lynx fw updates, and populate with a descriptive readme
        AppUtil.getInstance().ensureDirectoryExists(AppUtil.LYNX_FIRMWARE_UPDATE_DIR);
        String message = AppUtil.getDefContext().getString(R.string.lynxFirmwareUpdateReadme);
        ReadWriteFile.writeFile(AppUtil.LYNX_FIRMWARE_UPDATE_DIR, "readme.txt", message);

        // We also here, out of convenience, do likewise for the RC app, even though that
        // logically has nothing to do with firmware updating per se.
        message = AppUtil.getDefContext().getString(R.string.robotControllerAppUpdateReadme);
        ReadWriteFile.writeFile(AppUtil.RC_APP_UPDATE_DIR, "readme.txt", message);
        }

    //----------------------------------------------------------------------------------------------
    // Life Cycle
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftc_lynx_fw_update);

        networkConnectionHandler.pushReceiveLoopCallback(recvLoopCallback);
        }

    @Override protected void onStart()
        {
        super.onStart();

        TextView instructionsPre  = (TextView)findViewById(R.id.lynxFirmwareInstructionsPre);
        ListView modulesListView  = (ListView)findViewById(R.id.lynxFirmwareModuleList);
        TextView instructionsPost = (TextView)findViewById(R.id.lynxFirmwareInstructionsPost);
        Button   button           = (Button)findViewById(R.id.lynxFirmwareUpdateButton);

        CommandList.LynxFirmwareImagesResp candidateImages = getCandidateLynxFirmwareImages();

        if (candidateImages.firmwareImages.isEmpty())
            {
            File relativePath = AppUtil.getInstance().getRelativePath(candidateImages.firstFolder.getParentFile(), AppUtil.LYNX_FIRMWARE_UPDATE_DIR);
            instructionsPre.setText(getString(R.string.lynx_fw_instructions_no_binary, relativePath));
            modulesListView.setVisibility(View.GONE);
            instructionsPost.setVisibility(View.GONE);
            button.setEnabled(false);
            }
        else
            {
            Collections.sort(candidateImages.firmwareImages, new Comparator<FWImage>()
                {
                @Override public int compare(FWImage lhs, FWImage rhs)
                    {
                    // We sort in decreasing order, so that newest / highest numbered is first
                    return -lhs.getName().compareTo(rhs.getName());
                    }
                });
            firmwareImageFile = candidateImages.firmwareImages.get(0);

            modulesToUpdate = getLynxModulesForFirmwareUpdate();
            if (modulesToUpdate.isEmpty())
                {
                instructionsPre.setText(getString(R.string.lynx_fw_instructions_no_devices, firmwareImageFile.getName()));
                modulesListView.setVisibility(View.GONE);
                instructionsPost.setVisibility(View.GONE);
                button.setEnabled(false);
                }
            else
                {
                instructionsPre.setText(getString(R.string.lynx_fw_instructions_update, firmwareImageFile.getName()));

                class Item
                    {
                    String title;
                    String banter;
                    Item(String title, String banter) { this.title = title; this.banter = banter; }
                    }
                final List<Item> itemList = new ArrayList<>();
                for (USBAccessibleLynxModule module : modulesToUpdate)
                    {
                    String serialNumber = getString(R.string.lynx_fw_instructions_serial, module.getSerialNumber());
                    String moduleAddress = module.getModuleAddress()==0 ? getString(R.string.lynx_fw_instructions_module_address_unavailable) : getString(R.string.lynx_fw_instructions_module_address, module.getModuleAddress());
                    String firmware = getString(R.string.lynx_fw_instructions_firmware_version, module.getFinishedFirmwareVersionString());
                    String description = serialNumber + "\n" +
                            moduleAddress + "\n" +
                            firmware;
                    itemList.add(new Item(AppUtil.getDefContext().getString(R.string.lynx_fw_instructions_item_title), description));
                    }
                final int layoutRes = android.R.layout.simple_list_item_2;
                modulesListView.setAdapter(new ArrayAdapter<Item>(this, layoutRes, /*eh?*/ android.R.id.text1, itemList)
                    {
                    @Override public @NonNull View getView(int position, @Nullable View view, @NonNull ViewGroup parent)
                        {
                        if (view == null)
                            {
                            view = LayoutInflater.from(getContext()).inflate(layoutRes, parent, false);
                            }
                        View itemView = super.getView(position, view, parent);
                        TextView topLine = (TextView) itemView.findViewById(android.R.id.text1);
                        TextView bottomLine = (TextView) itemView.findViewById(android.R.id.text2);

                        Item item = getItem(position);
                        topLine.setVisibility(View.GONE);
                        bottomLine.setText(item.banter);

                        return view;
                        }
                    });

                button.setEnabled(enableUpdateButton);
                }
            }
        }

    @Override protected void onDestroy()
        {
        super.onDestroy();
        networkConnectionHandler.removeReceiveLoopCallback(recvLoopCallback);
        }

    //----------------------------------------------------------------------------------------------
    // UI interaction
    //----------------------------------------------------------------------------------------------

    protected enum FwResponseStatus
        {
            Succeeded,
            TimedOut,
            Cancelled
        }

    public void onUpdateLynxFirmwareClicked(View view)
        {
        // A second push is meaningless once we've entered fw update mode once
        enableUpdateButton = false;
        view.setEnabled(enableUpdateButton);

        // We're running on the UI thread here (of course). And the execution of the command
        // will need to put up dialogs, which means we can't be stuck here inside this method while
        // we do that. So we do the updating in a worker.
        //
        // We at least dismiss our background work here if the user dismisses our activity.
        // But we leave any actual update running to completion. That's probably a good thing,
        // (avoid bricking) but we might do better in managing the UI. Tricky back-and-forth, though.

        ThreadPool.getDefault().execute(new Runnable()
            {
            @Override public void run()
                {
                for (USBAccessibleLynxModule module : modulesToUpdate)
                    {
                    if (cancelUpdate)
                        break;

                    availableFWUpdateResps.clear();

                    RobotLog.vv(TAG, "updating %s with %s", module.getSerialNumber(), firmwareImageFile.getName());
                    CommandList.LynxFirmwareUpdate params = new CommandList.LynxFirmwareUpdate();
                    params.serialNumber = module.getSerialNumber();
                    params.firmwareImageFile = firmwareImageFile;
                    sendOrInject(new Command(CommandList.CMD_LYNX_FIRMWARE_UPDATE, SimpleGson.getInstance().toJson(params)));

                    MutableReference<FwResponseStatus> status = new MutableReference<>(FwResponseStatus.Succeeded);
                    CommandList.LynxFirmwareUpdateResp respParams = awaitResponse(availableFWUpdateResps, null, FlashLoaderManager.secondsFirmwareUpdateTimeout, TimeUnit.SECONDS, status);
                    if (respParams != null && respParams.success)
                        {
                        String message = getString(R.string.toastLynxFirmwareUpdateSuccessful, module.getSerialNumber());
                        RobotLog.vv(TAG, "%s", message);
                        AppUtil.getInstance().showToast(UILocation.BOTH, message);
                        }
                    else if (status.getValue() != FwResponseStatus.Cancelled)
                        {
                        String message = respParams==null
                                ? getString(R.string.alertLynxFirmwareUpdateTimedout, module.getSerialNumber())
                                : getString(R.string.alertLynxFirmwareUpdateFailed, module.getSerialNumber());
                        RobotLog.ee(TAG, "%s", message);
                        AppUtil.DialogContext alertDialogContext = AppUtil.getInstance().showAlertDialog(UILocation.BOTH, getString(R.string.alertLynxFirmwareUpdateFailedTitle), message);
                        try {
                            alertDialogContext.dismissed.await();
                            break;
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            }
                        break;
                        }
                    }
                FtcLynxFirmwareUpdateActivity.this.finish();
                }
            });
        }

    @Override protected void onPause()
        {
        super.onPause();
        cancelUpdate = true;
        }

    @Override protected void onStop()
        {
        super.onStop();
        AppUtil.getInstance().dismissProgress(UILocation.BOTH);
        }

    //----------------------------------------------------------------------------------------------
    // Receive loop
    //----------------------------------------------------------------------------------------------

    protected class ReceiveLoopCallback extends RecvLoopRunnable.DegenerateCallback
        {
        @Override public CallbackResult commandEvent(Command command) throws RobotCoreException
            {
            switch (command.getName())
                {
                case CommandList.CMD_GET_USB_ACCESSIBLE_LYNX_MODULES_RESP:
                    CommandList.USBAccessibleLynxModulesResp serialNumbers = CommandList.USBAccessibleLynxModulesResp.deserialize(command.getExtra());
                    availableLynxModules.offer(serialNumbers);
                    return CallbackResult.HANDLED;

                case CommandList.CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES_RESP:
                    CommandList.LynxFirmwareImagesResp candidates = CommandList.LynxFirmwareImagesResp.deserialize(command.getExtra());
                    availableLynxImages.offer(candidates);
                    return CallbackResult.HANDLED;

                case CommandList.CMD_LYNX_FIRMWARE_UPDATE_RESP:
                    CommandList.LynxFirmwareUpdateResp params = CommandList.LynxFirmwareUpdateResp.deserialize(command.getExtra());
                    availableFWUpdateResps.offer(params);
                    return CallbackResult.HANDLED;
                }
            return super.commandEvent(command);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected CommandList.LynxFirmwareImagesResp getCandidateLynxFirmwareImages()
        {
        CommandList.LynxFirmwareImagesResp result = new CommandList.LynxFirmwareImagesResp();

        availableLynxImages.clear();
        sendOrInject(new Command(CommandList.CMD_GET_CANDIDATE_LYNX_FIRMWARE_IMAGES));

        result = awaitResponse(availableLynxImages, result);
        RobotLog.vv(TAG, "found %d lynx firmware images", result.firmwareImages.size());
        return result;
        }

    protected List<USBAccessibleLynxModule> getLynxModulesForFirmwareUpdate()
        {
        CommandList.USBAccessibleLynxModulesRequest request = new CommandList.USBAccessibleLynxModulesRequest();
        CommandList.USBAccessibleLynxModulesResp result = new CommandList.USBAccessibleLynxModulesResp();

        // Send the command
        availableLynxModules.clear();
        request.forFirmwareUpdate = true;
        sendOrInject(new Command(CommandList.CMD_GET_USB_ACCESSIBLE_LYNX_MODULES, request.serialize()));

        // Wait, but only a while, for  the result
        result = awaitResponse(availableLynxModules, result);

        RobotLog.vv(TAG, "found %d lynx modules", result.modules.size());
        return result.modules;
        }

    protected void sendOrInject(Command cmd)
        {
        if (remoteConfigure)
            {
            NetworkConnectionHandler.getInstance().sendCommand(cmd);
            }
        else
            {
            NetworkConnectionHandler.getInstance().injectReceivedCommand(cmd);
            }
        }

    protected <T> T awaitResponse(BlockingQueue<T> queue, T defaultResponse)
        {
        return awaitResponse(queue, defaultResponse, msResponseWait, TimeUnit.MILLISECONDS, new MutableReference<FwResponseStatus>(FwResponseStatus.Succeeded));
        }

    protected <T> T awaitResponse(BlockingQueue<T> queue, T defaultResponse, long time, TimeUnit timeUnit, MutableReference<FwResponseStatus> status)
        {
        try {
            Deadline deadline = new Deadline(time, timeUnit);
            status.setValue(FwResponseStatus.TimedOut);
            while (!deadline.hasExpired())
                {
                T cur = queue.poll(100, TimeUnit.MILLISECONDS);
                if (cur != null)
                    {
                    status.setValue(FwResponseStatus.Succeeded);
                    return cur;
                    }
                if (cancelUpdate)
                    {
                    status.setValue(FwResponseStatus.Cancelled);
                    break;
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        return defaultResponse;
        }
    }
