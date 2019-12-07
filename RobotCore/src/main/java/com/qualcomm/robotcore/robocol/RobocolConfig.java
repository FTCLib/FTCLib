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

package com.qualcomm.robotcore.robocol;

import com.qualcomm.robotcore.util.Network;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.TypeConversion;

import org.firstinspires.ftc.robotcore.internal.network.SendOnceRunnable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Configuration Data for Robocol
 */
@SuppressWarnings("unused,WeakerAccess")
public class RobocolConfig {

  /**
   * ROBOCOL_VERSION controls the compatibility of the network protocol between driver station
   * and network controller. If that protocol changes in a non-backward-compatible way, increment
   * this constant to ensure incompatible apps do not attempt to communicate with each other.
   */

  public static final byte ROBOCOL_VERSION = 121;

  // The actual max packet size is the min of this value and whatever the OS says we can use
  public static final int MAX_MAX_PACKET_SIZE = 65520;  // + 16 bytes overhead == 64k

  public static final int MS_RECEIVE_TIMEOUT = SendOnceRunnable.MS_HEARTBEAT_TRANSMISSION_INTERVAL * 3;
  
  public static final int PORT_NUMBER = 20884;

  public static final int TTL = 3;

  public static final int TIMEOUT = 1000;

  public static final int WIFI_P2P_SUBNET_MASK = 0xFFFFFF00; // 255.255.255.0

  /**
   * Find a bind address. If no bind address can be found, return the loopback
   * address
   * @param destAddress destination address
   * @return address to bind to
   */
  public static InetAddress determineBindAddress(InetAddress destAddress) {
    ArrayList<InetAddress> localIpAddresses = Network.getLocalIpAddresses();
    localIpAddresses = Network.removeLoopbackAddresses(localIpAddresses);
    localIpAddresses = Network.removeIPv6Addresses(localIpAddresses);

    // if an iface has the destAddress, pick that one
    for (InetAddress address : localIpAddresses) {
      try {
        NetworkInterface iface = NetworkInterface.getByInetAddress(address);
        Enumeration<InetAddress> ifaceAddresses = iface.getInetAddresses();
        while (ifaceAddresses.hasMoreElements()) {
          InetAddress ifaceAddress = ifaceAddresses.nextElement();
          if (ifaceAddress.equals(destAddress)) {
            return ifaceAddress; // we found a match
          }
        }
      } catch (SocketException e) {
        RobotLog.v(String.format("socket exception while trying to get network interface of %s",
            address.getHostAddress()));
      }
    }

    return determineBindAddressBasedOnWifiP2pSubnet(localIpAddresses, destAddress);
  }

  /**
   * Find a bind address that is in the Wifi P2P subnet. If no bind address
   * can be found, return the loopback address.
   *
   * @param destAddress destination address
   * @return address to bind to
   */
  public static InetAddress determineBindAddressBasedOnWifiP2pSubnet(ArrayList<InetAddress> localIpAddresses, InetAddress destAddress) {
    int destAddrInt = TypeConversion.byteArrayToInt(destAddress.getAddress());

    // pick the first address where the destAddress is in the same subnet as an IP address assigned to a local network interface
    for (InetAddress address : localIpAddresses) {
      int addrInt = TypeConversion.byteArrayToInt(address.getAddress());
      if ((addrInt & WIFI_P2P_SUBNET_MASK) == (destAddrInt & WIFI_P2P_SUBNET_MASK)) {
        // we found a match
        return address;
      }
    }

    // We couldn't find a match
    return Network.getLoopbackAddress();
  }


   /**
    * Find a bind address that can reach the destAddress. If no bind address
    * can be found, return the loopback address. The InetAddress.isReachable()
    * method is used to determine if the address is reachable.
    *
    * @param destAddress destination address
    * @return address to bind to
    */
   public static InetAddress determineBindAddressBasedOnIsReachable(ArrayList<InetAddress> localIpAddresses, InetAddress destAddress) {
     // pick the first address where the destAddress is reachable
      for (InetAddress address : localIpAddresses) {
        try {
          NetworkInterface iface = NetworkInterface.getByInetAddress(address);
          if (address.isReachable(iface, TTL, TIMEOUT)) {
            return address; // we found a match
          }
        } catch (SocketException e) {
          RobotLog.v(String.format("socket exception while trying to get network interface of %s",
              address.getHostAddress()));
        } catch (IOException e) {
          RobotLog.v(String.format("IO exception while trying to determine if %s is reachable via %s",
              destAddress.getHostAddress(), address.getHostAddress()));
        }
      }

      // We couldn't find a match
      return Network.getLoopbackAddress();
   }
}
