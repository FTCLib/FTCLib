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
package org.firstinspires.ftc.robotcore.internal.network;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;

import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Consumer;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Assert;
import org.firstinspires.ftc.robotcore.internal.system.CallbackRegistrar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link WifiDirectAgent provides *low-level* basic connectivity to Wifi Direct. Most
 * importantly, this handles callback threading reasonably for us so we don't end up in
 * deadlock situations with the main app thread.
 */
@SuppressWarnings("WeakerAccess") public class WifiDirectAgent extends WifiStartStoppable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = NetworkDiscoveryManager.TAG + "_wifiDirectAgent";

    public String getTag() { return TAG; }

    @SuppressLint("StaticFieldLeak") protected static WifiDirectAgent theInstance;
    protected static StartResult theInstanceStartResult = new StartResult();
    public static WifiDirectAgent getInstance() { return theInstance; }
    static {
        theInstance = new WifiDirectAgent();
        // Auto-start WifiDirectAgent(), as it's so fundamental. That way everybody and his dog
        // doesn't have to manually do same
        theInstance.start(theInstanceStartResult);
        }

    protected final Context                     context;
    protected final CallbackLooper              looper;
    protected final WifiP2pManager              wifiP2pManager;
    protected final WifiP2pManager.Channel      wifiP2pChannel;
    protected final ChannelListener             channelListener;
    protected final CallbackRegistrar<Callback> callbacks = new CallbackRegistrar<Callback>();
    protected final WifiBroadcastReceiver       wifiBroadcastReceiver;

    protected boolean                           isWifiP2pEnabled = false;
    protected WifiState                         wifiState = WifiState.UNKNOWN;
    protected NetworkInfo.State                 wifiP2pState = NetworkInfo.State.UNKNOWN;
    protected IntentFilter                      intentFilter;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public WifiDirectAgent()
        {
        super(0);
        context                 = AppUtil.getInstance().getApplication();
        looper                  = CallbackLooper.getDefault();
        channelListener         = new ChannelListener();
        wifiP2pManager          = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel          = wifiP2pManager.initialize(context, looper.getLooper(), channelListener); // note threading
        wifiBroadcastReceiver   = new WifiBroadcastReceiver();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiDirectPersistentGroupManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        }

    //----------------------------------------------------------------------------------------------
    // Start / stop
    //----------------------------------------------------------------------------------------------

    @Override
    protected boolean doStart()
        {
        boolean localSuccess = true;

        doListen();

        new WifiDirectPersistentGroupManager(this).requestPersistentGroups(new WifiDirectPersistentGroupManager.PersistentGroupInfoListener()
            {
            @Override public void onPersistentGroupInfoAvailable(Collection<WifiP2pGroup> groups)
                {
                for (WifiP2pGroup group : groups)
                    {
                    RobotLog.vv(TAG, "found persistent group: %s", group.getNetworkName());
                    }
                }
            }); // TEMPORARY

        return localSuccess;
        }

    @Override
    protected void doStop()
        {
        doNotListen();
        }

    public void doNotListen()
        {
        context.unregisterReceiver(wifiBroadcastReceiver);
        }

    public void doListen()
        {
        context.registerReceiver(wifiBroadcastReceiver, intentFilter, null, looper.getHandler()); // note threading
        }

    //----------------------------------------------------------------------------------------------
    // Low level accessing
    //----------------------------------------------------------------------------------------------

    public WifiP2pManager getWifiP2pManager()
        {
        return wifiP2pManager;
        }

    public WifiP2pManager.Channel getWifiP2pChannel()
        {
        return wifiP2pChannel;
        }

    public CallbackLooper getLooper()
        {
        return looper;
        }

    public boolean isLooperThread()
        {
        Assert.assertNotNull(looper);
        return looper.isLooperThread();
        }

    public boolean isWifiDirectEnabled()
        {
        return isWifiP2pEnabled;
        }

    public WifiState getWifiState()
        {
        return wifiState;
        }

    public NetworkInfo.State getWifiDirectState()
        {
        return wifiP2pState;
        }

    //----------------------------------------------------------------------------------------------
    // General wifi-related queries & utilities
    //
    // Not really wifi-*direct*-related, per se, but it's handy and convenient to locate there here
    //----------------------------------------------------------------------------------------------

    @Deprecated
    public boolean isAirplaneModeOn()
        {
        return WifiUtil.isAirplaneModeOn();
        }

    @Deprecated
    public boolean isBluetoothOn()
        {
        return WifiUtil.isBluetoothOn();
        }

    @Deprecated
    public boolean isWifiEnabled()
        {
        return WifiUtil.isWifiEnabled();
        }

    @Deprecated
    public boolean isWifiConnected()
        {
        return WifiUtil.isWifiConnected();
        }

    public boolean isWifiDirectConnected()
        {
        NetworkInfo.State state = getWifiDirectState();
        return state== NetworkInfo.State.CONNECTED || state== NetworkInfo.State.CONNECTING;
        }

    /** synchronously disconnects from wifi direct. must not be called on the callback looper thread */
    public boolean disconnectFromWifiDirect()
        {
        return lockCompletion(false, new Func<Boolean>()
            {
            @Override public Boolean value()
                {
                boolean success = resetCompletion();
                if (success)
                    {
                    try {
                        wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener()
                            {
                            @Override public void onGroupInfoAvailable(WifiP2pGroup group)
                                {
                                if (group != null && group.isGroupOwner())
                                    {
                                    wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener()
                                        {
                                        @Override public void onSuccess()
                                            {
                                            releaseCompletion(true);
                                            }
                                        @Override public void onFailure(int reason)
                                            {
                                            releaseCompletion(false);
                                            }
                                        });
                                    }
                                else
                                    {
                                    releaseCompletion(false);
                                    }
                                }
                            });
                        success = waitForCompletion();
                        }
                    catch (InterruptedException e)
                        {
                        success = receivedCompletionInterrupt(e);
                        }
                    }
                return success;
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Changing Channels
    //----------------------------------------------------------------------------------------------

    /**
     * setWifiP2pChannels is a hidden method that went in the AOSP tree on 2013.05.03, and so,
     * we believe is included even in Kitcat.
     * @param lc        the channel to listen on. 0 = let supplicant pick (preferable)
     * @param oc        operating channel. 0 = let platform pick, 1-14 - set per requirement and validity within regulatory domain
     * @param listener  asynchronously receives results of setting
     */
    public void setWifiP2pChannels(int lc, int oc, WifiP2pManager.ActionListener listener)
        {
        Method method = ClassUtil.getDeclaredMethod(this.getWifiP2pManager().getClass(), "setWifiP2pChannels",
                            WifiP2pManager.Channel.class,
                            int.class,
                            int.class,
                            WifiP2pManager.ActionListener.class);
        if (method != null)
            {
            ClassUtil.invoke(this.getWifiP2pManager(), method, this.getWifiP2pChannel(), lc, oc, listener);
            }
        else
            {
            throw new RuntimeException("setWifiP2pChannels() is not supported on this device");
            }
        }

    //----------------------------------------------------------------------------------------------
    // Types and Notifications
    //----------------------------------------------------------------------------------------------

    public interface Callback
        {
        void onReceive(Context context, Intent intent);
        }

    public void registerCallback(Callback callback)
        {
        callbacks.registerCallback(callback);
        }
    public void unregisterCallback(Callback callback)
        {
        callbacks.unregisterCallback(callback);
        }

    protected class WifiBroadcastReceiver extends BroadcastReceiver
        {
        protected static final String TAG = WifiDirectAgent.TAG + "_bcast";

        @Override public void onReceive(final Context context, final Intent intent)
            {
            //--------------------------------------------------------------------------------------
            // Log the broadcast, here, in one place. Also some small bookkeeping.

            String action = intent.getAction();
            switch (action)
                {
                case WifiManager.WIFI_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                    wifiState = WifiState.from(state);
                    RobotLog.vv(TAG, "wifiState=%s", wifiState);
                    break;
                    }
                case WifiDirectPersistentGroupManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION: {
                    RobotLog.vv(TAG, "wifi direct remembered groups cleared");
                    // Let our network peer know if he's connected and listening
                    NetworkConnectionHandler.getInstance().sendCommand(new Command(RobotCoreCommandList.CMD_NOTIFY_WIFI_DIRECT_REMEMBERED_GROUPS_CHANGED));
                    break;
                    }
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
                    isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                    RobotLog.vv(TAG, "wifiP2pEnabled=%s", isWifiP2pEnabled);
                    break;
                    }
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    WifiP2pDeviceList wifiP2pDeviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                    dump(wifiP2pDeviceList);
                    break;
                    }
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    WifiP2pInfo wifip2pinfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                    WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

                    wifiP2pState = networkInfo.getState();

                    RobotLog.dd(TAG, "connection changed: networkInfo.state=%s", networkInfo.getState());
                    dump(networkInfo);
                    dump(wifip2pinfo);
                    dump(wifiP2pGroup);

                    break;
                    }
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
                    WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    dump(wifiP2pDevice);
                    break;
                    }
                case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                    boolean discovering = (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
                    RobotLog.vv(TAG, "p2p discoverPeers()=%s", discovering);
                    break;
                    }
                }

            //--------------------------------------------------------------------------------------
            // Send the broadcast on to anyone who's registered to hear it

            callbacks.callbacksDo(new Consumer<Callback>()
                {
                @Override public void accept(Callback callback)
                    {
                    callback.onReceive(context, intent);
                    }
                });
            }

        protected void dump(WifiP2pDevice wifiP2pDevice)
            {
            RobotLog.vv(TAG, "this device changed: %s", format(wifiP2pDevice));
            }

        protected void dump(WifiP2pDeviceList wifiP2pDeviceList)
            {
            List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(wifiP2pDeviceList.getDeviceList());
            RobotLog.vv(TAG, "peers found: count=" + peers.size());
            for (WifiP2pDevice peer : peers)
                {
                // deviceAddress is the MAC address, deviceName is the human readable name
                String s = "    peer: " + peer.deviceAddress + " " + peer.deviceName;
                RobotLog.vv(TAG, s);
                }
            }

        protected void dump(NetworkInfo info)
            {
            Assert.assertNotNull(info);
            RobotLog.vv(TAG, "NetworkInfo: %s", info.toString());
            }

        protected void dump(WifiP2pInfo info)
            {
            Assert.assertNotNull(info);
            RobotLog.vv(TAG, "WifiP2pInfo: %s", info.toString());
            }

        protected void dump(WifiP2pGroup info)
            {
            Assert.assertNotNull(info);
            RobotLog.vv(TAG, "WifiP2pGroup: %s", (info.toString().replace("\n ", ", ")));
            }
        }

    protected static String format(WifiP2pDevice wifiP2pDevice)
        {
        return wifiP2pDevice.toString().replace(": ","=").replace("\n "," ");
        }


    protected class ChannelListener implements WifiP2pManager.ChannelListener
        {
        @Override public void onChannelDisconnected()
            {
            // Nothing to do, at the moment
            }
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    }
