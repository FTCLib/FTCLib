/*
 * Copyright (c) 2015 Qualcomm Technologies Inc
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

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;

/**
 * ImmersiveMode
 * set up and maintain immersive mode for an Android app.
 */
public class ImmersiveMode {

  // Used by the activity to correctly set the view
  // that should be using immersive mode.
  View decorView;

  public ImmersiveMode(View decorView){
    this.decorView = decorView;
  }

  // A handler that hides the system UI stuff.
  // This helps us be able to cancel a "hide" in the case of a
  // dialog popup or similar.
  Handler hideSystemUiHandler = new Handler() {
    @Override
    public void handleMessage(Message msg){
      hideSystemUI();
    }
  };

  public void cancelSystemUIHide(){
    hideSystemUiHandler.removeMessages(0);
  }

  public void hideSystemUI() {
    // Set the IMMERSIVE flag.
    // Set the content to appear under the system bars so that the content
    // doesn't resize when the nav bar hides and shows.

    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }

  // Immersive sticky flag only works on API >= 19
  public static boolean apiOver19(){
    int currentApiVersion = Build.VERSION.SDK_INT;
    return currentApiVersion >= Build.VERSION_CODES.KITKAT;
  }
}
