// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.util;

public enum ToolboxFolder {
  ACTUATORS("Actuators"),
  SENSORS("Sensors"),
  OTHER("Other Devices");

  public final String label;

  ToolboxFolder(String label) {
    this.label = label;
  }
}
