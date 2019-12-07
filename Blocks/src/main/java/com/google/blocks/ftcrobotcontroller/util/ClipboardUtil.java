// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.util;

import static org.firstinspires.ftc.robotcore.internal.system.AppUtil.BLOCK_OPMODES_DIR;

import java.io.File;
import java.io.IOException;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

/**
 * A class that provides utility methods related to blocks clipboard.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class ClipboardUtil {
  private static final File CLIPBOARD_FILE = new File(BLOCK_OPMODES_DIR, "clipboard.xml");

  // Prevent instantiation of utility class.
  private ClipboardUtil() {
  }

  /**
   * Saves the clipboard content.
   *
   * @param clipboardContent the clipboard content to write.
   */
  public static void saveClipboardContent(String clipboardContent) throws IOException {
    AppUtil.getInstance().ensureDirectoryExists(BLOCK_OPMODES_DIR);
    FileUtil.writeFile(CLIPBOARD_FILE, clipboardContent);
  }

  /**
   * Reads the clipboard content.
   */
  public static String fetchClipboardContent() throws IOException {
    return FileUtil.readFile(CLIPBOARD_FILE);
  }
}
