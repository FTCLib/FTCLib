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

package com.qualcomm.robotcore.hardware.configuration;

import android.app.Activity;
import android.app.AlertDialog;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.firstinspires.ftc.robotcore.internal.system.Assert;

/**
 * {@link Utility} is an <em>old</em> general purpose utility class.
 */
public class Utility {

  private Activity activity;

  public Utility(Activity activity) {
    this.activity = activity;
  }

  public Activity getActivity() {
    return activity;
  }

  //------------------------------------------------------------------------------------------------
  // UI utilities
  //------------------------------------------------------------------------------------------------

  public void setFeedbackText(CharSequence[] array, int info_id, int layout_id, int orange0, int orange1) {
    setFeedbackText(array[0], array[1], info_id, layout_id, orange0, orange1);
  }

  public void setFeedbackText(CharSequence title, CharSequence message, @IdRes int idParent, @LayoutRes int layout_id, @IdRes int idTitle, @IdRes int idMessage) {
    setFeedbackText(title, message, idParent, layout_id, idTitle, idMessage, 0);
  }

  public void setFeedbackText(CharSequence title, CharSequence message, final @IdRes int idParent, @LayoutRes int layout_id, @IdRes int idTitle, @IdRes int idMessage, @IdRes int idButtonDismiss) {
    LinearLayout parent = (LinearLayout) activity.findViewById(idParent);
    parent.setVisibility(View.VISIBLE);
    parent.removeAllViews();
    LayoutInflater inflater = activity.getLayoutInflater();
    inflater.inflate(layout_id, parent, true);
    TextView text0 = (TextView) parent.findViewById(idTitle);
    TextView text1 = (TextView) parent.findViewById(idMessage);
    Button buttonDismiss = (Button) parent.findViewById(idButtonDismiss);

    if (text0 != null) text0.setText(title);
    if (text1 != null) text1.setText(message);
    if (buttonDismiss != null) {
      buttonDismiss.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
        hideFeedbackText(idParent);
        }}
      );
      buttonDismiss.setVisibility(View.VISIBLE);
    }
  }

  public void hideFeedbackText(int idParent) {
    LinearLayout parent = (LinearLayout) activity.findViewById(idParent);
    parent.removeAllViews();
    parent.setVisibility(View.GONE);
  }

  public @Nullable CharSequence[] getFeedbackText(@IdRes int info_id, @LayoutRes int layout_id, @IdRes int feedback0, @IdRes int feedback1) {
    LinearLayout layout = (LinearLayout) activity.findViewById(info_id);
    Assert.assertTrue(layout != null);
    TextView text0 = (TextView) layout.findViewById(feedback0);
    TextView text1 = (TextView) layout.findViewById(feedback1);

    boolean text0Null = text0==null || text0.getText().length()==0;
    boolean text1Null = text1==null || text1.getText().length()==0;
    if (text0Null && text1Null) return  null;

    return new CharSequence[] { text0==null?"":text0.getText(), text1==null?"":text1.getText() };
  }

  //********************** Alert Dialog helpers ***********************//

  public AlertDialog.Builder buildBuilder(String title, String message){
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle(title)
        .setMessage(message);
        //.setView(input);
    return builder;
  }

}
