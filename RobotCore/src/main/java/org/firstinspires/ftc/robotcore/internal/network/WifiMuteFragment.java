/*
 * Copyright (c) 2018 Craig MacFarlane
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
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
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

package org.firstinspires.ftc.robotcore.internal.network;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qualcomm.robotcore.R;
import com.qualcomm.robotcore.util.RobotLog;

public class WifiMuteFragment extends Fragment {

    TextView timer;
    TextView nofication;
    TextView description;
    WifiMuteStateMachine stateMachine;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_wifi_mute, container, false);
        timer = (TextView)view.findViewById(R.id.countdownNumber);
        description = (TextView)view.findViewById(R.id.countdownDescription);
        nofication = (TextView)view.findViewById(R.id.wifiDisabledNotification);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    stateMachine.consumeEvent(WifiMuteEvent.USER_ACTIVITY);
                }
                /*
                 * Intentionally consume all other events on this fragment to prevent clicks from falling
                 * through to the driver station activity.  No unintentional clicks on Init for example.
                 */
                return true;
            }
        });

        return view;
    }

    public void setStateMachine(WifiMuteStateMachine stateMachine)
    {
        this.stateMachine = stateMachine;
    }

    public void setCountdownNumber(final long num)
    {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                timer.setText(String.valueOf(num));
            }
        });
    }

    public void displayDisabledMessage()
    {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                description.setVisibility(View.GONE);
                timer.setVisibility(View.GONE);
                nofication.setVisibility(View.VISIBLE);
            }
        });
    }

    public void reset()
    {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                description.setVisibility(View.VISIBLE);
                timer.setVisibility(View.VISIBLE);
                nofication.setVisibility(View.GONE);
            }
        });
    }
}
