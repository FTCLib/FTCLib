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

package org.firstinspires.ftc.robotcore.external;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that provides utility methods used in FTC Java code generated from blocks.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@SuppressWarnings("unchecked")
public class JavaUtil {
  // Utilities for text blocks

  public enum AtMode {
    FIRST,
    LAST,
    FROM_START,
    FROM_END,
    RANDOM
  }

  private static int getIndex(String str, AtMode atMode, int i) {
    switch (atMode) {
      case FIRST:
        return 0;
      case LAST:
        return str.length() - 1;
      case FROM_START:
        return i;
      case FROM_END:
        return str.length() - 1 - i;
      case RANDOM:
        return (int) Math.floor(Math.random() * str.length());
    }
    throw new IllegalArgumentException("Unknown AtMode " + atMode);
  }

  public static String inTextGetLetter(String str, AtMode atMode, int i) {
    return "" + str.charAt(getIndex(str, atMode, i));
  }

  public static String inTextGetSubstring(String str, AtMode atMode1, int i1, AtMode atMode2, int i2) {
    return str.substring(getIndex(str, atMode1, i1), getIndex(str, atMode2, i2) + 1);
  }

  public static String toTitleCase(String str) {
    String[] words = str.split("((?<=\\s+)|(?=\\s+))");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      String w = words[i];
      char ch = w.charAt(0);
      if (!Character.isWhitespace(ch)) {
        w = Character.toTitleCase(ch) + w.substring(1).toLowerCase();
      }
      sb.append(w);
    }
    return sb.toString();
  }

  public enum TrimMode {
    LEFT,
    RIGHT,
    BOTH
  }

  public static String textTrim(String str, TrimMode trimMode) {
    switch (trimMode) {
      case LEFT:
        for (int i = 0; i < str.length(); i++) {
          if (str.codePointAt(i) > 0x0020) {
            str = str.substring(i);
            break;
          }
        }
        return str;
      case RIGHT:
        for (int i = str.length() - 1; i >= 0; i--) {
          if (str.codePointAt(i) > 0x0020) {
            str = str.substring(0, i + 1);
            break;
          }
        }
        return str;
      case BOTH:
        return str.trim();
    }
    throw new IllegalArgumentException("Unknown TrimMode " + trimMode);
  }

  // Utilities for math blocks

  public static String formatNumber(double number, int precision) {
    precision = Math.max(0, precision);
    // Fix for cases like 1.0005 formatted to precision 3. The result should be 1.001, but without
    // this it is 1.000.
    number += Math.pow(10, - precision - 1);
    int width = (precision == 0) ? 1 : precision;
    String format = "%" + width + "." + precision + "f";
    return String.format(format, number);
  }

  public static boolean isPrime(double d) {
    // https://en.wikipedia.org/wiki/Primality_test#Naive_methods
    // False if Nan, not whole, or < 2.
    if (Double.isNaN(d) || d % 1 != 0 || d < 2) {
      return false;
    }
    long n = (long) d;
    if (n == 2 || n == 3) {
      return true;
    }
    // False if n is divisible by 2 or 3.
    if (n % 2 == 0 || n % 3 == 0) {
      return false;
    }
    // Check all the numbers of form 6k +/- 1, up to sqrt(n).
    for (int x = 6; x <= Math.sqrt(n) + 1; x += 6) {
      if (n % (x - 1) == 0 || n % (x + 1) == 0) {
        return false;
      }
    }
    return true;
  }

  public static double sumOfList(List list) {
    double sum = 0;
    if (list != null) {
      for (Object o : list) {
        if (o instanceof Number) {
          double n = ((Number) o).doubleValue();
          sum += n;
        }
      }
    }
    return sum;
  }

  public static double minOfList(List list) {
    double min = Long.MAX_VALUE;
    if (list != null) {
      for (Object o : list) {
        if (o instanceof Number) {
          double n = ((Number) o).doubleValue();
          if (n < min) {
            min = n;
          }
        }
      }
    }
    return min;
  }

  public static double maxOfList(List list) {
    double max = Long.MIN_VALUE;
    if (list != null) {
      for (Object o : list) {
        if (o instanceof Number) {
          double n = ((Number) o).doubleValue();
          if (n > max) {
            max = n;
          }
        }
      }
    }
    return max;
  }

  public static double averageOfList(List list) {
    double mean = 0;
    if (list != null && !list.isEmpty()) {
      for (Object o : list) {
        if (o instanceof Number) {
          double n = ((Number) o).doubleValue();
          mean += n;
        }
      }
      mean /= list.size();
    }
    return mean;
  }

  public static double medianOfList(List list) {
    if (list == null) {
      return 0;
    }
    List<Double> localList = new ArrayList<>();
    for (Object o : list) {
      if (o instanceof Number) {
        double n = ((Number) o).doubleValue();
        localList.add(n);
      }
    }
    if (localList.isEmpty()) {
      return 0;
    }
    Collections.sort(localList);
    int size = localList.size();
    if (size % 2 == 0) {
      return (localList.get(size / 2 - 1) + localList.get(size / 2)) / 2;
    } else {
      return localList.get((size - 1) / 2);
    }
  }

  public static List modesOfList(List list) {
    List modes = new ArrayList<>();
    if (list != null && !list.isEmpty()) {
      Map<Object, Integer> counts = new HashMap<>();
      int maxCount = 0;
      for (Object o : list) {
        Integer boxedCount = counts.get(o);
        int count = (boxedCount != null)
            ? boxedCount + 1
            : 1;
        counts.put(o, count);
        if (count > maxCount) {
          maxCount = count;
        }
      }
      for (Map.Entry<Object, Integer> entry : counts.entrySet()) {
        if (entry.getValue() == maxCount) {
            modes.add(entry.getKey());
        }
      }
    }
    return modes;
  }

  public static double standardDeviationOfList(List list) {
    double variance = 0;
    if (list != null && !list.isEmpty()) {
      double mean = averageOfList(list);
      for (Object o : list) {
        if (o instanceof Number) {
          double n = ((Number) o).doubleValue();
          variance += Math.pow(n - mean, 2);
        }
      }
      variance /= list.size();
    }
    return Math.sqrt(variance);
  }

  public static Object randomItemOfList(List list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    int i = (int) Math.floor(Math.random() * list.size());
    return list.get(i);
  }

  public static int randomInt(double a, double b) {
    if (a > b) {
      // Swap a and b to ensure a is smaller.
      double swap = a;
      a = b;
      b = swap;
    }
    return (int) Math.floor(Math.random() * (b - a + 1) + a);
  }

  // Utilities for list blocks

  public static List createListWith(Object ... elements) {
    List list = new ArrayList();
    Collections.addAll(list, elements);
    return list;
  }

  public static List createListWithItemRepeated(Object element, int n) {
    List list = new ArrayList();
    for (int i = 0; i < n; i++) {
      list.add(element);
    }
    return list;
  }

  private static int getIndex(List list, AtMode atMode, int i) {
    switch (atMode) {
      case FIRST:
        return 0;
      case LAST:
        return list.size() - 1;
      case FROM_START:
        return i;
      case FROM_END:
        return list.size() - 1 - i;
      case RANDOM:
        return (int) Math.floor(Math.random() * list.size());
    }
    throw new IllegalArgumentException("Unknown AtMode " + atMode);
  }

  public static Object inListGet(List list, AtMode atMode, int i, boolean remove) {
    if (list == null) {
      return null;
    }
    i = getIndex(list, atMode, i);
    if (remove) {
      return list.remove(i);
    } else {
      return list.get(i);
    }
  }

  public static void inListSet(List list, AtMode atMode, int i, boolean insert,
      Object value) {
    if (list == null) {
      return;
    }
    i = getIndex(list, atMode, i);
    if (insert) {
      list.add(i, value);
    } else {
      list.set(i, value);
    }
  }

  public static List inListGetSublist(List list, AtMode atMode1, int i1, AtMode atMode2, int i2) {
    if (list == null) {
      return null;
    }
    return new ArrayList(list.subList(getIndex(list, atMode1, i1), getIndex(list, atMode2, i2) + 1));
  }

  public enum SortType {
    NUMERIC,
    TEXT,
    IGNORE_CASE
  }

  public enum SortDirection {
    ASCENDING,
    DESCENDING
  }

  public static List sort(List list, SortType sortType, final SortDirection sortDirection) {
    if (list == null) {
      return null;
    }
    List copy = new ArrayList(list);
    Comparator comparator;
    switch (sortType) {
      default:
      case NUMERIC:
        comparator = new Comparator() {
          @Override
          public int compare (Object o1, Object o2) {
            double d1 = 0;
            if (o1 instanceof Number) {
              d1 = ((Number) o1).doubleValue();
            } else if (o1 != null) {
              try {
                d1 = Double.parseDouble(o1.toString());
              } catch (NumberFormatException e) {
                // ignored, d1 is still zero.
              }
            }
            double d2 = 0;
            if (o2 instanceof Number) {
              d2 = ((Number) o2).doubleValue();
            } else if (o2 != null) {
              try {
                d2 = Double.parseDouble(o2.toString());
              } catch (NumberFormatException e) {
                // ignored, d2 is still zero.
              }
            }
            return (sortDirection == SortDirection.ASCENDING)
                ? (int) Math.signum(d1 - d2)
                : (int) Math.signum(d2 - d1);
          }
        };
        break;
      case TEXT:
        comparator = new Comparator() {
          @Override
          public int compare (Object o1, Object o2) {
            String s1 = o1.toString();
            String s2 = o2.toString();
            return (sortDirection == SortDirection.ASCENDING)
                ? s1.compareTo(s2)
                : s2.compareTo(s1);
          }
        };
        break;
      case IGNORE_CASE:
        comparator = new Comparator() {
          @Override
          public int compare (Object o1, Object o2) {
            String s1 = o1.toString();
            String s2 = o2.toString();
            return (sortDirection == SortDirection.ASCENDING)
                ? s1.compareToIgnoreCase(s2)
                : s2.compareToIgnoreCase(s1);
          }
        };
        break;
    }
    Collections.sort(copy, comparator);
    return copy;
  }

  public static String makeTextFromList(List list, String delimiter) {
    StringBuilder sb = new StringBuilder();
    if (list != null && delimiter != null) {
      String d = ""; // No delimiter before first item.
      for (Object o : list) {
        sb.append(d).append(o);
        d = delimiter;
      }
    }
    return sb.toString();
  }

  public static List makeListFromText(String text, String delimiter) {
    // Make list from text.
    List list = new ArrayList();
    if (text != null && delimiter != null) {
      int delimiterLength = delimiter.length();
      int i = 0;
      while (i < text.length()) {
        int d = text.indexOf(delimiter, i);
        list.add(text.substring(i, d));
        i = d + delimiterLength;
      }
    }
    return list;
  }

  // Utilities for FTC color blocks

  private static float[] colorToHSV(int color) {
    float[] array = new float[3];
    Color.colorToHSV(color, array);
    return array;
  }

  public static float colorToHue(int color) {
    return colorToHSV(color)[0];
  }

  public static float colorToSaturation(int color) {
    return colorToHSV(color)[1];
  }

  public static float colorToValue(int color) {
    return colorToHSV(color)[2];
  }

  public static int hsvToColor(float hue, float saturation, float value) {
    float[] array = new float[3];
    array[0] = hue;
    array[1] = saturation;
    array[2] = value;
    return Color.HSVToColor(array);
  }

  public static int ahsvToColor(int alpha, float hue, float saturation, float value) {
    float[] array = new float[3];
    array[0] = hue;
    array[1] = saturation;
    array[2] = value;
    return Color.HSVToColor(alpha, array);
  }
}
