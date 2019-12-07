// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.hardware;

import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A class that identifies a single hardware item.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class HardwareItem {
  /**
   * The parent of this hardware item.
   */
  public final HardwareItem parent;
  /**
   * The HardwareType of this hardware item.
   */
  public final HardwareType hardwareType;
  /**
   * The deviceName is the value of the name attribute in the configuration xml file and can be
   * used to get a {@link HardwareDevice} from the {@link HardwareMap}.
   */
  public final String deviceName;
  /**
   * The identifier used in blocks and generated JavaScript. This identifier includes the
   * hardwareType.identifierSuffixForJavaScript.
   */
  public final String identifier;
  /**
   * The name shown on the blocks.
   */
  public final String visibleName;

  /**
   * Constructs a {@link HardwareItem} with the given {@link HardwareType} and device name.
   */
  public HardwareItem(HardwareItem parent, HardwareType hardwareType, String deviceName) {
    if (hardwareType == null || deviceName == null) {
      throw new NullPointerException();
    }
    this.parent = parent;
    this.hardwareType = hardwareType;
    this.deviceName = deviceName;
    identifier = makeIdentifier(deviceName) + hardwareType.identifierSuffixForJavaScript;
    visibleName = HardwareUtil.makeVisibleNameForDropdownItem(deviceName);
  }

  static String makeIdentifier(String deviceName) {
    int length = deviceName.length();
    StringBuilder identifier = new StringBuilder();

    char ch = deviceName.charAt(0);
    if (Character.isJavaIdentifierStart(ch)) {
      identifier.append(ch);
    } else if (Character.isJavaIdentifierPart(ch)) {
      identifier.append('_').append(ch);
    }
    for (int i = 1; i < length; i++) {
      ch = deviceName.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        identifier.append(ch);
      }
    }
    return identifier.toString();
  }

  boolean hasAncestor(HardwareType hardwareType) {
    for (HardwareItem p = parent; p != null; p = p.parent) {
      if (p.hardwareType == hardwareType) {
        return true;
      }
    }
    return false;
  }

  // java.lang.Object methods

  @Override
  public boolean equals(Object o) {
    if (o instanceof HardwareItem) {
      HardwareItem that = (HardwareItem) o;
      return this.hardwareType.equals(that.hardwareType)
          && this.deviceName.equals(that.deviceName)
          && this.identifier.equals(that.identifier)
          && this.visibleName.equals(that.visibleName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hardwareType.hashCode()
        + deviceName.hashCode()
        + identifier.hashCode()
        + visibleName.hashCode();
  }
}
