/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
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
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
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

package com.qualcomm.robotcore.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for performing network operations
 */
public class Network {

  /**
   * Get the Loopback Address
   * @return 127.0.0.1
   */
  public static InetAddress getLoopbackAddress() {
    try {
      return InetAddress.getByAddress(new byte[] {127, 0 , 0, 1});
    } catch (UnknownHostException e) {
      // since we don't expect a failure here, return null
      return null;
    }
  }

  /**
   * Get local IP addresses
   * @return a collection of all local IP addresses
   */
  public static ArrayList<InetAddress> getLocalIpAddresses() {

    ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();

    try {
      for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        addresses.addAll(Collections.list(iface.getInetAddresses()));
      }
    } catch (SocketException e) {
      // NetworkInterface.getNetworkInterfaces() threw an exception
      // return an empty collection
    }

    return addresses;
  }

  /**
   * Get local IP addresses of a given interface
   * @param networkInterface
   * @return a collection of all IP addresses for the given interface
   */
  public static ArrayList<InetAddress> getLocalIpAddress(String networkInterface) {

    ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();

    try {
      for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (iface.getName() == networkInterface) {
          addresses.addAll(Collections.list(iface.getInetAddresses()));
        }
      }
    } catch (SocketException e) {
      // NetworkInterface.getNetworkInterfaces() threw an exception
      // return an empty collection
    }

    return addresses;
  }

  /**
   * Remove all IPv6 addresses from a collection
   * @param addresses
   * @return A new collection with all IPv6 addresses removed
   */
  public static ArrayList<InetAddress> removeIPv6Addresses(Collection<InetAddress> addresses) {

    ArrayList<InetAddress> filtered = new ArrayList<InetAddress>();
    for (InetAddress addr : addresses) {
      if (addr instanceof Inet4Address) {
        filtered.add(addr);
      }
    }

    return filtered;
  }

  /**
   * Remove all IPv4 addresses from a collection
   * @param addresses
   * @return A new collection with all IPv4 addresses removed
   */
  public static ArrayList<InetAddress> removeIPv4Addresses(Collection<InetAddress> addresses) {

    ArrayList<InetAddress> filtered = new ArrayList<InetAddress>();
    for (InetAddress addr : addresses) {
      if (addr instanceof Inet6Address) {
        filtered.add(addr);
      }
    }

    return filtered;
  }

  /**
   * Remove all loopback addresses from a collection
   * @param addresses
   * @return A new collection with all loopback addresses removed.
   */
  public static ArrayList<InetAddress> removeLoopbackAddresses(Collection<InetAddress> addresses) {

    ArrayList<InetAddress> filtered = new ArrayList<InetAddress>();
    for (InetAddress addr : addresses) {
      if (!addr.isLoopbackAddress()) {
        filtered.add(addr);
      }
    }

    return filtered;
  }

  /**
   * Get the host address of each InetAddress in a collection
   * @param addresses
   * @return a collection of host addresses
   */
  public static ArrayList<String> getHostAddresses(Collection<InetAddress> addresses) {

    ArrayList<String> hostnames = new ArrayList<String>();

    for (InetAddress addr : addresses) {
      String host = addr.getHostAddress();
      if (host.contains("%")) host = host.substring(0, host.indexOf('%'));
      hostnames.add(host);
    }

    return hostnames;
  }
}
