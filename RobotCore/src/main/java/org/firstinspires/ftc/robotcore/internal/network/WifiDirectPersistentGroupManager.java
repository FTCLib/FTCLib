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

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;

import com.qualcomm.robotcore.util.ClassUtil;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Func;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link WifiDirectPersistentGroupManager} is a utility class for discovering and
 * manipulating WifiDirect persistent groups
 */
@SuppressWarnings("WeakerAccess")
public class WifiDirectPersistentGroupManager extends WifiStartStoppable
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "WifiDirectPersistentGroupManager";
    public String getTag() { return TAG; }

    // This is @hide in WifiP2pManager, but functional. There is no extra; one can simply poll for extant groups.
    public static final String WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION = "android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED";

    protected static Class      classWifiP2pGroupList;
    protected static Class      classPersistentGroupInfoListener;
    protected static Method     methodGetGroupList;
    protected static Method     methodRequestPersistentGroupInfo;
    protected static Method     methodDeletePersistentGroup;
    protected static Method     methodGetNetworkId;

    /* From WifiP2pManager:

        /** Interface for callback invocation when stored group info list is available {@hide} *|
        public interface PersistentGroupInfoListener {
            /**
             * The requested stored p2p group info list is available
             * @param groups Wi-Fi p2p group info list
             *|
            public void onPersistentGroupInfoAvailable(WifiP2pGroupList groups);
        }

     Because that interface is hidden, we can't implement it directly. Fortunately, we can
     resort to java.lang.reflect.Proxy in order to accomplish effectively the same thing,
     though it's a bit of mouthful to look at.
    */

    static
        {
        try {
            classWifiP2pGroupList = Class.forName("android.net.wifi.p2p.WifiP2pGroupList");
            classPersistentGroupInfoListener = Class.forName("android.net.wifi.p2p.WifiP2pManager$PersistentGroupInfoListener");

            // Find the 'public Collection<WifiP2pGroup> getGroupList()' method
            methodGetGroupList = ClassUtil.getDeclaredMethod(classWifiP2pGroupList, "getGroupList");

            // WifiP2pManger: public void requestPersistentGroupInfo(Channel c, PersistentGroupInfoListener listener) {
            methodRequestPersistentGroupInfo = ClassUtil.getDeclaredMethod(WifiP2pManager.class,
                    "requestPersistentGroupInfo",
                    WifiP2pManager.Channel.class, classPersistentGroupInfoListener);

            // WifiP2pManger: public void deletePersistentGroup(Channel c, int netId, ActionListener listener) {
            methodDeletePersistentGroup = ClassUtil.getDeclaredMethod(WifiP2pManager.class,
                    "deletePersistentGroup",
                    WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);

            // WifiP2pGroup: public int getNetworkId()
            methodGetNetworkId = ClassUtil.getDeclaredMethod(WifiP2pGroup.class, "getNetworkId");
            }
        catch (ClassNotFoundException e)
            {
            RobotLog.ee(TAG, e, "exception thrown in static initialization");
            }
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public WifiDirectPersistentGroupManager(WifiDirectAgent wifiDirectAgent)
        {
        super(wifiDirectAgent);
        }

    //----------------------------------------------------------------------------------------------
    // Start / stop: nothing to actually do
    //----------------------------------------------------------------------------------------------

    @Override protected boolean doStart() throws InterruptedException
        {
        return true;
        }

    @Override protected void doStop() throws InterruptedException
        {
        }

    //----------------------------------------------------------------------------------------------
    // Group management
    //----------------------------------------------------------------------------------------------

    public interface PersistentGroupInfoListener
        {
        void onPersistentGroupInfoAvailable(Collection<WifiP2pGroup> groups);
        }

    /** Asynchronously deletes the indicated persistent group */
    public void deletePersistentGroup(int netId, WifiP2pManager.ActionListener listener)
        {
        RobotLog.vv(TAG, "deletePersistentGroup() netId=%d", netId);
        ClassUtil.invoke(wifiDirectAgent.getWifiP2pManager(), methodDeletePersistentGroup, wifiDirectAgent.getWifiP2pChannel(), netId, listener);
        }

    /** Synchronously deletes the indicated persistent group */
    public boolean deletePersistentGroup(final int netId)
        {
        return lockCompletion(false, new Func<Boolean>()
            {
            @Override public Boolean value()
                {
                boolean success = resetCompletion();
                try {
                    deletePersistentGroup(netId, new WifiP2pManager.ActionListener()
                        {
                        @Override public void onSuccess()
                            {
                            releaseCompletion(true);
                            }
                        @Override public void onFailure(int reason)
                            {
                            RobotLog.vv(TAG, "failed to delete persistent group: netId=%d", netId);
                            releaseCompletion(false);
                            }
                        });
                    success = waitForCompletion();
                    }
                catch (InterruptedException e)
                    {
                    success = receivedCompletionInterrupt(e);
                    }
                return success;
                }
            });
        }

    /** Synchronously deletes the indicated persistent group */
    public boolean deletePersistentGroup(WifiP2pGroup group)
        {
        return deletePersistentGroup(getNetworkId(group));
        }

    /** Synchronously deletes all persistent groups */
    public void deleteAllPersistentGroups()
        {
        for (WifiP2pGroup group : getPersistentGroups())
            {
            deletePersistentGroup(group);
            }
        }

    /** Returns the network id of the indicted WifiP2pGroup */
    public int getNetworkId(WifiP2pGroup group)
        {
        return (int)ClassUtil.invoke(group, methodGetNetworkId);
        }

    protected Object createProxy(final PersistentGroupInfoListener target)
        {
        // Dynamically create an implementation of WifiP2pManager$PersistentGroupInfoListener
        return java.lang.reflect.Proxy.newProxyInstance(
            classPersistentGroupInfoListener.getClassLoader(),
            new Class[]{classPersistentGroupInfoListener},
            new InvocationHandler()
                {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                    {
                    if (method.getName().equals("onPersistentGroupInfoAvailable"))
                        {
                        // wifiP2pGroupList is of type android.net.wifi.p2p.WifiP2pGroupList, which is also hidden
                        Object wifiP2pGroupList = args[0];

                        // call getGroupList()
                        Collection<WifiP2pGroup> wifiP2pGroups = (Collection<WifiP2pGroup>)ClassUtil.invoke(wifiP2pGroupList, methodGetGroupList);

                        // Call our nice pretty method
                        target.onPersistentGroupInfoAvailable(wifiP2pGroups);
                        }

                    return null;
                    }
                });
        }

    /** Asynchronously enumerates the extant persistent Wifi Direct groups. Aka 'Remembered Groups' */
    public void requestPersistentGroups(PersistentGroupInfoListener listener)
        {
        Object persistentGroupInfoListenerProxy = createProxy(listener);
        ClassUtil.invoke(wifiDirectAgent.getWifiP2pManager(), methodRequestPersistentGroupInfo, wifiDirectAgent.getWifiP2pChannel(), persistentGroupInfoListenerProxy);
        }

    /** Synchronously enumerates the extant persistent Wifi Direct groups. Must NOT be called
     * on the callback looper thread. If an error or interrupt occurs, an empty list is returned. */
    public Collection<WifiP2pGroup> getPersistentGroups()
        {
        final Collection<WifiP2pGroup> defRefResult = new ArrayList<WifiP2pGroup>();
        return lockCompletion(defRefResult, new Func<Collection<WifiP2pGroup>>()
            {
            Collection<WifiP2pGroup> result;
            @Override public Collection<WifiP2pGroup> value()
                {
                result = defRefResult;
                resetCompletion();
                try {
                    requestPersistentGroups(new PersistentGroupInfoListener()
                        {
                        @Override public void onPersistentGroupInfoAvailable(Collection<WifiP2pGroup> groups)
                            {
                            result = groups;
                            releaseCompletion(true);;
                            }
                        });
                    waitForCompletion();
                    }
                catch (InterruptedException e)
                    {
                    receivedCompletionInterrupt(e);
                    }
                return result;
                }
            });
        }

    }
