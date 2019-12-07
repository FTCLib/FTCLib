// Copyright 2016 Google Inc.

package org.firstinspires.ftc.robotserver.internal.webserver;

import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Container of recent PingDetails. Adapted from logic formerly in AbstractProgrammingModeActivity.
 */
@SuppressWarnings("WeakerAccess")
public class PingDetailsHolder
    {
    public static final long EXPIRATION_DURATION_SECONDS = 3;
    public static final long REMOVE_OLD_PINGS_INTERVAL_SECONDS = 2;
    private static final Comparator<PingDetails> COMPARE_PING_DETAILS = new Comparator<PingDetails>()
        {
        @Override
        public int compare(PingDetails pingDetails1, PingDetails pingDetails2)
            {
            int result = pingDetails1.machineName.compareToIgnoreCase(pingDetails2.machineName);
            if (result == 0)
                {
                result = pingDetails1.name.compareToIgnoreCase(pingDetails2.name);
                }
            return result;
            }
        };

    private final Object pingLock = new Object();
    private final List<Long> pingTimes = new LinkedList<Long>();
    private final List<PingDetails> pingDetailsList = new LinkedList<PingDetails>();

    public void addPing(PingDetails pingDetails)
        {
        synchronized (pingLock)
            {
            long now = System.nanoTime();
            int index = pingDetailsList.indexOf(pingDetails);
            if (index != -1)
                {
                pingTimes.remove(index);
                pingDetailsList.remove(index);
                }
            pingTimes.add(now);
            pingDetailsList.add(pingDetails);
            }
        }

    public boolean removeOldPings()
        {
        boolean needToUpdateActiveConnectionsUI = false;
        synchronized (pingLock)
            {
            long minimum = System.nanoTime() - TimeUnit.SECONDS.toNanos(EXPIRATION_DURATION_SECONDS);
            while (!pingTimes.isEmpty() && pingTimes.get(0) < minimum)
                {
                pingTimes.remove(0);
                pingDetailsList.remove(0);
                needToUpdateActiveConnectionsUI = true;
                }
            }
        return needToUpdateActiveConnectionsUI;
        }

    public List<PingDetails> sortedPingDetailsList()
        {
        synchronized (pingLock)
            {
            List<PingDetails> result = new ArrayList<PingDetails>(pingDetailsList);
            Collections.sort(result, COMPARE_PING_DETAILS);
            return result;
            }
        }

    public String toJson()
        {
        List<PingDetails> sorted = sortedPingDetailsList();
        return SimpleGson.getInstance().toJson(sorted);
        }
    }
