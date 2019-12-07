// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.util;

import java.io.IOException;

public class CorruptFileException extends IOException {
  protected CorruptFileException(String message) {
    super(message);
  }

  protected CorruptFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
