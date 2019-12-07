// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.graphics.Color;
import android.webkit.JavascriptInterface;

/**
 * A class that provides JavaScript access to {@link Color}.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class ColorAccess extends Access {

  ColorAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "Color");
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getRed(int color) {
    startBlockExecution(BlockType.GETTER, ".Red");
    return Color.red(color);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getGreen(int color) {
    startBlockExecution(BlockType.GETTER, ".Green");
    return Color.green(color);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getBlue(int color) {
    startBlockExecution(BlockType.GETTER, ".Blue");
    return Color.blue(color);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public double getAlpha(int color) {
    startBlockExecution(BlockType.GETTER, ".Alpha");
    return Color.alpha(color);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getHue(int color) {
    startBlockExecution(BlockType.GETTER, ".Hue");
    float[] array = new float[3];
    Color.colorToHSV(color, array);
    return array[0];
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getSaturation(int color) {
    startBlockExecution(BlockType.GETTER, ".Saturation");
    float[] array = new float[3];
    Color.colorToHSV(color, array);
    return array[1];
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public float getValue(int color) {
    startBlockExecution(BlockType.GETTER, ".Value");
    float[] array = new float[3];
    Color.colorToHSV(color, array);
    return array[2];
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int rgbToColor(int red, int green, int blue) {
    startBlockExecution(BlockType.FUNCTION, ".rgbToColor");
    return Color.rgb(red, green, blue);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int argbToColor(int alpha, int red, int green, int blue) {
    startBlockExecution(BlockType.FUNCTION, ".argbToColor");
    return Color.argb(alpha, red, green, blue);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int hsvToColor(float hue, float saturation, float value) {
    startBlockExecution(BlockType.FUNCTION, ".hsvToColor");
    float[] array = new float[3];
    array[0] = hue;
    array[1] = saturation;
    array[2] = value;
    return Color.HSVToColor(array);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int ahsvToColor(int alpha, float hue, float saturation, float value) {
    startBlockExecution(BlockType.FUNCTION, ".ahsvToColor");
    float[] array = new float[3];
    array[0] = hue;
    array[1] = saturation;
    array[2] = value;
    return Color.HSVToColor(alpha, array);
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public int textToColor(String text) {
    startBlockExecution(BlockType.FUNCTION, ".textToColor");
    return Color.parseColor(text);
  }
}
