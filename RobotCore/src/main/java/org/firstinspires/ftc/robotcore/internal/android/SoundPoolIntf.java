/*
Copyright 2018 Google LLC.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.firstinspires.ftc.robotcore.internal.android;

import android.content.Context;
import androidx.annotation.RawRes;
import java.io.File;

/**
 * Interface used by {@link org.firstinspires.ftc.robotcore.external.android.AndroidSoundPool}
 * and implemented by {@link com.qualcomm.ftccommon.SoundPlayer}.
 */
public interface SoundPoolIntf {
  boolean preload(Context context, @RawRes int resourceId);
  boolean preload(Context context, File file);

  void play(Context context, @RawRes int resourceId, float volume, int loop, float rate);
  void play(Context context, File file, float volume, int loop, float rate);

  void stopPlayingAll();

  void close();
}
