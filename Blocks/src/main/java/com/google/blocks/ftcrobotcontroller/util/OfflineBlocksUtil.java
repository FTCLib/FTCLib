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

package com.google.blocks.ftcrobotcontroller.util;

import static com.google.blocks.ftcrobotcontroller.util.ProjectsUtil.escapeSingleQuotes;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.text.Html;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareItemMap;
import com.google.blocks.ftcrobotcontroller.hardware.HardwareUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotserver.internal.webserver.AppThemeColors;

/**
 * A class that provides utility methods related to offline blocks editor.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class OfflineBlocksUtil {
  public static InputStream fetchOfflineBlocksEditor() throws IOException {
    String configName = HardwareUtil.getConfigurationName();
    HardwareItemMap hardwareItemMap = HardwareItemMap.newHardwareItemMap();
    AssetManager assetManager = AppUtil.getDefContext().getAssets();

    Set<String> assetsToInclude = new HashSet<>();

    assetsToInclude.add("js/split.min.js");

    assetsToInclude.add("blocks/images.css");
    for (String blocksImagesFile : assetManager.list("blocks/images")) {
      assetsToInclude.add("blocks/images/" + blocksImagesFile);
    }

    assetsToInclude.add("css/blocks_offline.css");
    assetsToInclude.add("css/blocks_common.css");

    assetsToInclude.add("blockly/blockly_compressed.js");
    for (String blocklyMediaFile : assetManager.list("blockly/media")) {
      assetsToInclude.add("blockly/media/" + blocklyMediaFile);
    }
    assetsToInclude.add("blockly/msg/messages.js");
    assetsToInclude.add("blockly/blocks_compressed.js");
    assetsToInclude.add("blockly/javascript_compressed.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/lists.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/logic.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/loops.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/math.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/procedures.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/text.js");
    assetsToInclude.add("ftcblockly/generators/ftcjava/variables.js");

    assetsToInclude.add("blocks/FtcBlocks_common.js");
    assetsToInclude.add("blocks/FtcBlocksProjects_common.js");

    assetsToInclude.add("blocks/acceleration.js");
    assetsToInclude.add("blocks/acceleration_sensor.js");
    assetsToInclude.add("blocks/analog_input.js");
    assetsToInclude.add("blocks/analog_output.js");
    assetsToInclude.add("blocks/android_accelerometer.js");
    assetsToInclude.add("blocks/android_gyroscope.js");
    assetsToInclude.add("blocks/android_orientation.js");
    assetsToInclude.add("blocks/android_sound_pool.js");
    assetsToInclude.add("blocks/android_text_to_speech.js");
    assetsToInclude.add("blocks/angular_velocity.js");
    assetsToInclude.add("blocks/bno055imu.js");
    assetsToInclude.add("blocks/bno055imu_parameters.js");
    assetsToInclude.add("blocks/clipboard_util.js");
    assetsToInclude.add("blocks/color.js");
    assetsToInclude.add("blocks/color_sensor.js");
    assetsToInclude.add("blocks/compass_sensor.js");
    assetsToInclude.add("blocks/cr_servo.js");
    assetsToInclude.add("blocks/dbg_log.js");
    assetsToInclude.add("blocks/dc_motor.js");
    assetsToInclude.add("blocks/digital_channel.js");
    assetsToInclude.add("blocks/distance_sensor.js");
    assetsToInclude.add("blocks/elapsed_time.js");
    assetsToInclude.add("blocks/elapsed_time2.js");
    assetsToInclude.add("blocks/gamepad.js");
    assetsToInclude.add("blocks/gyro_sensor.js");
    assetsToInclude.add("blocks/hardware_util.js");
    assetsToInclude.add("blocks/ir_seeker_sensor.js");
    assetsToInclude.add("blocks/led.js");
    assetsToInclude.add("blocks/light_sensor.js");
    assetsToInclude.add("blocks/linear_op_mode.js");
    assetsToInclude.add("blocks/lynx_i2c_color_range_sensor.js");
    assetsToInclude.add("blocks/magnetic_flux.js");
    assetsToInclude.add("blocks/matrix_f.js");
    assetsToInclude.add("blocks/misc.js");
    assetsToInclude.add("blocks/mr_i2c_compass_sensor.js");
    assetsToInclude.add("blocks/mr_i2c_range_sensor.js");
    assetsToInclude.add("blocks/navigation.js");
    assetsToInclude.add("blocks/open_gl_matrix.js");
    assetsToInclude.add("blocks/optical_distance_sensor.js");
    assetsToInclude.add("blocks/orientation.js");
    assetsToInclude.add("blocks/pidf_coefficients.js");
    assetsToInclude.add("blocks/position.js");
    assetsToInclude.add("blocks/project_util.js");
    assetsToInclude.add("blocks/quaternion.js");
    assetsToInclude.add("blocks/range.js");
    assetsToInclude.add("blocks/rev_blinkin_led_driver.js");
    assetsToInclude.add("blocks/servo.js");
    assetsToInclude.add("blocks/servo_controller.js");
    // sound_util.js is not needed
    assetsToInclude.add("blocks/system.js");
    assetsToInclude.add("blocks/telemetry.js");
    assetsToInclude.add("blocks/temperature.js");
    assetsToInclude.add("blocks/tfod.js");
    assetsToInclude.add("blocks/tfod_recognition.js");
    assetsToInclude.add("blocks/tfod_rover_ruckus.js");
    assetsToInclude.add("blocks/tfod_sky_stone.js");
    assetsToInclude.add("blocks/toolbox_util.js");
    assetsToInclude.add("blocks/touch_sensor.js");
    assetsToInclude.add("blocks/ultrasonic_sensor.js");
    assetsToInclude.add("blocks/vars.js");
    assetsToInclude.add("blocks/vector_f.js");
    assetsToInclude.add("blocks/velocity.js");
    assetsToInclude.add("blocks/voltage_sensor.js");
    assetsToInclude.add("blocks/vuforia.js");
    assetsToInclude.add("blocks/vuforia_localizer.js");
    assetsToInclude.add("blocks/vuforia_localizer_parameters.js");
    assetsToInclude.add("blocks/vuforia_relic_recovery.js");
    assetsToInclude.add("blocks/vuforia_rover_ruckus.js");
    assetsToInclude.add("blocks/vuforia_sky_stone.js");
    assetsToInclude.add("blocks/vuforia_trackable.js");
    assetsToInclude.add("blocks/vuforia_trackable_default_listener.js");
    assetsToInclude.add("blocks/vuforia_trackables.js");

    assetsToInclude.add("FtcOfflineBlocksProjects.html");
    assetsToInclude.add("FtcOfflineBlocks.html");
    assetsToInclude.add("favicon.ico");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("index.html"));
      copyAsset(assetManager, "FtcOfflineFrame.html", zos);
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("css/blocks_common_less.css"));
      zos.write(convertLessToCss(assetManager, "css/blocks_common.less").getBytes());
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("css/frame_offline_less.css"));
      zos.write(convertLessToCss(assetManager, "css/frame_offline.less").getBytes());
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("js/FtcOfflineBlocks.js"));
      zos.write(getFtcOfflineBlocksJs(configName, hardwareItemMap).getBytes());
      zos.closeEntry();

      for (String assetPath : assetsToInclude) {
        String dest = assetPath;
        zos.putNextEntry(new ZipEntry(dest));
        copyAsset(assetManager, assetPath, zos);
        zos.closeEntry();
      }
    }

    return new ByteArrayInputStream(baos.toByteArray());
  }

  private static void copyAsset(AssetManager assetManager, String assetPath, OutputStream outputStream) throws IOException {
    try (InputStream inputStream = assetManager.open(assetPath)) {
      byte[] buffer = new byte[4096];
      int n;
      while ((n = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, n);
      }
    }
  }

  private static String convertLessToCss(AssetManager assetManager, String assetPath) throws IOException {
    AppThemeColors colors = AppThemeColors.fromTheme();
    StringBuilder cssStringBuilder = new StringBuilder();
    FileUtil.readAsset(cssStringBuilder, assetManager, assetPath);
    float[] hsvTextBright = new float[3];
    Color.colorToHSV(colors.textBright, hsvTextBright);
    return cssStringBuilder.toString()
        .replace("@import \"/css/core.less\";", "")
        .replace("hue(@textBright)", String.format("%d", (int) Math.round(hsvTextBright[0])))
        .replace("saturation(@textBright)", String.format("%d%%", (int) Math.round(100 * hsvTextBright[1])))
        .replace("darken(@backgroundMedium, 5%)", String.format("#%06x", (darken(colors.backgroundMedium, 0.05f) & 0xFFFFFF)))
        .replace("@textError", String.format("#%06x", (colors.textError & 0xFFFFFF)))
        .replace("@textWarning", String.format("#%06x", (colors.textWarning & 0xFFFFFF)))
        .replace("@textOkay", String.format("#%06x", (colors.textOkay & 0xFFFFFF)))
        .replace("@textBright", String.format("#%06x", (colors.textBright & 0xFFFFFF)))
        .replace("@textLight", String.format("#%06x", (colors.textLight & 0xFFFFFF)))
        .replace("@textMediumDark", String.format("#%06x", (colors.textMediumDark & 0xFFFFFF)))
        .replace("@textMedium", String.format("#%06x", (colors.textMedium & 0xFFFFFF)))
        .replace("@textVeryDark", String.format("#%06x", (colors.textVeryDark & 0xFFFFFF)))
        .replace("@textVeryVeryDark", String.format("#%06x", (colors.textVeryVeryDark & 0xFFFFFF)))
        .replace("@backgroundLight", String.format("#%06x", (colors.backgroundLight & 0xFFFFFF)))
        .replace("@backgroundMediumLight", String.format("#%06x", (colors.backgroundMediumLight & 0xFFFFFF)))
        .replace("@backgroundMediumMedium", String.format("#%06x", (colors.backgroundMediumMedium & 0xFFFFFF)))
        .replace("@backgroundMediumDark", String.format("#%06x", (colors.backgroundMediumDark & 0xFFFFFF)))
        .replace("@backgroundMedium", String.format("#%06x", (colors.backgroundMedium & 0xFFFFFF)))
        .replace("@backgroundAlmostDark", String.format("#%06x", (colors.backgroundAlmostDark & 0xFFFFFF)))
        .replace("@backgroundDark", String.format("#%06x", (colors.backgroundDark & 0xFFFFFF)))
        .replace("@backgroundVeryDark", String.format("#%06x", (colors.backgroundVeryDark & 0xFFFFFF)))
        .replace("@backgroundVeryVeryDark", String.format("#%06x", (colors.backgroundVeryVeryDark & 0xFFFFFF)))
        .replace("@lineBright", String.format("#%06x", (colors.lineBright & 0xFFFFFF)))
        .replace("@lineLight", String.format("#%06x", (colors.lineLight & 0xFFFFFF)))
        .replace("@feedbackBackground", String.format("#%06x", (colors.feedbackBackground & 0xFFFFFF)))
        .replace("@feedbackBorder", String.format("#%06x", (colors.feedbackBorder & 0xFFFFFF)));
  }

  private static int darken(int color, float amount) {
    float[] hsl = new float[3];
    colorToHSL(color, hsl);
    hsl[2] -= amount;
    return hslToColor(hsl);
  }

  /**
   * Convert the given color to hsl, where h, s, and l are between 0 and 1.
   */
  private static void colorToHSL(int color, float[] hsl) {
    int r255 = Color.red(color);
    int g255 = Color.green(color);
    int b255 = Color.blue(color);
    int max255 = Math.max(Math.max(r255, g255), b255);
    int min255 = Math.min(Math.min(r255, g255), b255);

    float max = max255 / 255f;
    float min = min255 / 255f;

    hsl[2] = (max + min) / 2;

    if (max255 == min255) {
      hsl[0] = hsl[1] = 0;
    } else {
      hsl[1] = (hsl[2] > 0.5)
          ? (max - min) / (2 - max - min)
          : (max - min) / (max + min);

      hsl[0] = hue(r255, g255, b255);
    }
  }

  /**
   * Convert the given hsl, where h, s, and l are between 0 and 1, to a color.
   */
  private static int hslToColor(float[] hsl) {
    float r, g, b;

    if (hsl[1] == 0) {
      r = g = b = hsl[2]; // achromatic
    } else {

      float q = (hsl[2] < 0.5f)
          ? hsl[2] * (hsl[1] + 1)
          : hsl[2] + hsl[1] - hsl[2] * hsl[1];
      float p = 2f * hsl[2] - q;
      r = hue2rgb(p, q, hsl[0] + 1/3f);
      g = hue2rgb(p, q, hsl[0]);
      b = hue2rgb(p, q, hsl[0] - 1/3f);
    }

    int r255 = (int) Math.round(r * 255f);
    int g255 = (int) Math.round(g * 255f);
    int b255 = (int) Math.round(b * 255f);
    return Color.rgb(r255, g255, b255);
  }

  private static float hue2rgb(float p, float q, float t) {
    if (t < 0) {
      t += 1;
    }
    if (t > 1) {
      t -= 1;
    }
    if (t < 1/6f) {
      return p + (q - p) * 6 * t;
    }
    if (t < 1/2f) {
      return q;
    }
    if (t < 2/3f) {
      return p + (q - p) * (2/3f - t) * 6;
    }
    return p;
  }

  private static float hue(int r255, int g255, int b255) {
    float[] hsv = new float[3];
    Color.RGBToHSV(r255, g255, b255, hsv);
    return hsv[0] / 360;
  }

  private static String getFtcOfflineBlocksJs(String configName, HardwareItemMap hardwareItemMap) throws IOException {
    StringBuilder jsStringBuilder = new StringBuilder();
    jsStringBuilder
        .append("function getBlkFiles() {\n")
        .append("  var BLK_FILES = [\n");
    List<OfflineBlocksProject> offlineBlocksProjects = new ArrayList<>();
    ProjectsUtil.fetchProjectsForOfflineBlocksEditor(offlineBlocksProjects);
    String delimiter = "";
    for (OfflineBlocksProject offlineBlocksProject : offlineBlocksProjects) {
      jsStringBuilder.append(delimiter)
          .append("    {\n")
          .append("      'FileName': '").append(escapeSingleQuotes(offlineBlocksProject.fileName)).append("',\n")
          .append("      'Content': '").append(escapeSingleQuotes(offlineBlocksProject.content)).append("',\n")
          .append("      'name': '").append(escapeSingleQuotes(offlineBlocksProject.name)).append("',\n")
          .append("      'escapedName' : '").append(escapeSingleQuotes(Html.escapeHtml(offlineBlocksProject.name))).append("',\n")
          .append("      'dateModifiedMillis': ").append(offlineBlocksProject.dateModifiedMillis).append(",\n")
          .append("      'enabled': ").append(offlineBlocksProject.enabled).append("\n")
          .append("    }");
      delimiter = ",\n";
    }
    offlineBlocksProjects = null;

    jsStringBuilder
        .append("\n")
        .append("  ];\n")
        .append("  return BLK_FILES;\n")
        .append("}\n\n")
        .append("function getOfflineConfigurationName() {\n")
        .append("  return '").append(escapeSingleQuotes(configName)).append("';\n")
        .append("}\n\n")
        .append("function getSampleNamesJson() {\n")
        .append("  var SAMPLE_NAMES = '").append(ProjectsUtil.fetchSampleNames()).append("';\n")
        .append("  return SAMPLE_NAMES;\n")
        .append("}\n\n")
        .append("function getSampleBlkFileContent(sampleName) {\n")
        .append("  switch (sampleName) {\n");
    for (Map.Entry<String, String> entry : ProjectsUtil.getSamples(hardwareItemMap).entrySet()) {
      String sampleName = entry.getKey();
      String blkFileContent = entry.getValue().replace("\n", " ").replaceAll("\\> +\\<", "><");
      if (sampleName.isEmpty()) {
        jsStringBuilder
            .append("    default:\n");
      }
      jsStringBuilder
          .append("    case '").append(escapeSingleQuotes(sampleName)).append("':\n")
          .append("      return '").append(escapeSingleQuotes(blkFileContent)).append("';\n");
    }
    jsStringBuilder
        .append("  }\n")
        .append("}\n")
        .append(HardwareUtil.fetchJavaScriptForHardware(hardwareItemMap));
    return jsStringBuilder.toString();
  }
}
