/*
 * Copyright (c) 2019 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc.openrc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import com.qualcomm.robotcore.eventloop.opmode.AnnotatedOpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.concurrent.CountDownLatch;

public class LegalityNotification
{
    private static boolean alreadyShownWarningThisSession = false;

    @OpModeRegistrar
    public static void showLegalityNotification(Context context, AnnotatedOpModeManager manager)
    {
        if(LynxConstants.isRevControlHub() || Utils.hasAcknowledgedLegalityStatus() || alreadyShownWarningThisSession)
        {
            return;
        }

        String msg = "<font color='red'>In its default configuration, OpenRC is <b>illegal</b> for competition use.</font><br><br>Make sure to switch to the stock build variant before going to competition!";

        Activity activity = AppUtil.getInstance().getActivity();

        final CountDownLatch latch = new CountDownLatch(1);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Competition Legality")
                .setMessage(Html.fromHtml(msg))
                .setPositiveButton("Acknowledged", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        Utils.setLegalityAcknowledgementStatus(true);
                        latch.countDown();
                    }
                })
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        latch.countDown();
                    }
                })
                .setCancelable(false);

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                builder.show();
            }
        });

        alreadyShownWarningThisSession = true;

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
