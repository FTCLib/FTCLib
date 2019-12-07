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

package com.qualcomm.robotcore.util;

import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Various utility methods
 */
public class Util {

  public static String ASCII_RECORD_SEPARATOR = "\u001e";

  public static final String LOWERCASE_ALPHA_NUM_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";
  /**
   * Get a random string of characters of specified length from a specified character set.
   * @param stringLength how many characters to randomly choose
   * @param charSet which characters to choose from, given as a string
   * @return a string
   */
  public static String getRandomString(final int stringLength, final String charSet)
  {
    final Random random=new Random();
    final StringBuilder sb=new StringBuilder();
    for(int i=0;i<stringLength;++i)
    {
      sb.append(charSet.charAt(random.nextInt(charSet.length())));
    }
    return sb.toString();
  }

  /**
   * Sort an array of File objects, by filename
   * @param files array of File objects
   */
  public static void sortFilesByName(File[] files){
    Arrays.sort(files, new Comparator<File>(){
      @Override
      public int compare(File f1, File f2){
        return f1.getName().compareTo(f2.getName());
      }
    });
  }

  public static void updateTextView(final TextView textView, final String msg) {
    if (textView != null) {
      textView.post(new Runnable() {
        public void run() {
          textView.setText(msg);
        }
      });
    }
  }

  /**
   * Creates a new byte array long enough to hold both byte arrays, then fills
   * them.
   * @param first byte array
   * @param second byte array
   * @return new byte array, which contains a concatenation of both byte arrays
   */
  public static byte[] concatenateByteArrays(byte[] first, byte[] second) {
    byte[] concatenated = new byte[first.length + second.length];
    System.arraycopy(first, 0, concatenated, 0, first.length);
    System.arraycopy(second, 0, concatenated, first.length, second.length);
    return concatenated;
  }

  public static byte[] concatenateByteArrays(byte[] first, byte[] second, byte[] third) {
    byte[] concatenated = new byte[first.length + second.length + third.length];
    System.arraycopy(first,  0, concatenated, 0,                            first.length);
    System.arraycopy(second, 0, concatenated, first.length,                 second.length);
    System.arraycopy(third,  0, concatenated, first.length + second.length, third.length);
    return concatenated;
  }

  /**
   * Is 'prefix' an initial substring of 'target'?
   * @param prefix the string prefix to compare
   * @param target the target to compare the prefix against.
   * @return true if the prefix is at the front of target, false otherwise
   * */
  public static boolean isPrefixOf(String prefix, String target) {
    if (prefix == null) {
      return true;
    } else if (target == null) {
      return false;
    } else {
      if (prefix.length() <= target.length()) {
        for (int ich = 0; ich < prefix.length(); ich++) {
          if (prefix.charAt(ich) != target.charAt(ich)) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }

  public static boolean isGoodString(String string) {
    if (string == null)
      return false;
    if (!string.trim().equals(string))
      return false;
    if (string.length() == 0)
      return false;
    return true;
  }
}
