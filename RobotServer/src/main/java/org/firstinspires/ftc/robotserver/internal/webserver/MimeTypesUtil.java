// Copyright 2016 Google Inc.

package org.firstinspires.ftc.robotserver.internal.webserver;

import android.support.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that provides utility methods related to mime types.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@SuppressWarnings("WeakerAccess")
public class MimeTypesUtil {
  public static final String MIME_JSON = "application/json";
  public static final String MIME_JAVASCRIPT = "application/javascript";
  public static final String MIME_CSS = "text/css";
  private static final Map<String, String> MIME_TYPES_BY_EXT = new HashMap<String, String>();
  static {
    MIME_TYPES_BY_EXT.put("asc", "text/plain");
    MIME_TYPES_BY_EXT.put("bin", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("class", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("css", MIME_CSS);
    MIME_TYPES_BY_EXT.put("cur", "image/x-win-bitmap");
    MIME_TYPES_BY_EXT.put("doc", "application/msword");
    MIME_TYPES_BY_EXT.put("exe", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("flv", "video/x-flv");
    MIME_TYPES_BY_EXT.put("gif", "image/gif");
    MIME_TYPES_BY_EXT.put("gz", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("gzip", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("html", "text/html");
    MIME_TYPES_BY_EXT.put("htm", "text/html");
    MIME_TYPES_BY_EXT.put("ico", "image/x-icon");
    MIME_TYPES_BY_EXT.put("java", "text/x-java-source, text/java");
    MIME_TYPES_BY_EXT.put("jpeg", "image/jpeg");
    MIME_TYPES_BY_EXT.put("jpg", "image/jpeg");
    MIME_TYPES_BY_EXT.put("js", MIME_JAVASCRIPT);
    MIME_TYPES_BY_EXT.put("json", MIME_JSON);
    MIME_TYPES_BY_EXT.put("less", MIME_CSS);
    MIME_TYPES_BY_EXT.put("logcat", "text/plain");
    MIME_TYPES_BY_EXT.put("m3u8", "application/vnd.apple.mpegurl");
    MIME_TYPES_BY_EXT.put("m3u", "audio/mpeg-url");
    MIME_TYPES_BY_EXT.put("md", "text/plain");
    MIME_TYPES_BY_EXT.put("mov", "video/quicktime");
    MIME_TYPES_BY_EXT.put("mp3", "audio/mpeg");
    MIME_TYPES_BY_EXT.put("mp4", "video/mp4");
    MIME_TYPES_BY_EXT.put("ogg", "application/x-ogg");
    MIME_TYPES_BY_EXT.put("ogv", "video/ogg");
    MIME_TYPES_BY_EXT.put("pdf", "application/pdf");
    MIME_TYPES_BY_EXT.put("png", "image/png");
    MIME_TYPES_BY_EXT.put("svg", "image/svg+xml");
    MIME_TYPES_BY_EXT.put("swf", "application/x-shockwave-flash");
    MIME_TYPES_BY_EXT.put("ts", "video/mp2t");
    MIME_TYPES_BY_EXT.put("txt", "text/plain");
    MIME_TYPES_BY_EXT.put("wav", "audio/wav");
    MIME_TYPES_BY_EXT.put("xml", "text/xml");
    MIME_TYPES_BY_EXT.put("zip", "application/octet-stream");
    // Java Editor MIME types
    MIME_TYPES_BY_EXT.put("map", "application/json map");
    MIME_TYPES_BY_EXT.put("jar", "application/octet-stream");
    MIME_TYPES_BY_EXT.put("log", "text/plain");
    MIME_TYPES_BY_EXT.put("ttf", "application/x-font-truetype");
    MIME_TYPES_BY_EXT.put("otf", "application/x-font-opentype");
    MIME_TYPES_BY_EXT.put("woff", "application/font-woff");
    MIME_TYPES_BY_EXT.put("woff2", "application/font-woff2");
    MIME_TYPES_BY_EXT.put("eot", "application/vnd.ms-fontobject");
    MIME_TYPES_BY_EXT.put("sfnt", "application/font-sfnt");
  }

  // Prevent instantiation of util class.
  private MimeTypesUtil() {
  }

  /**
   * Returns the mime type for the given file extension.
   * Should we enhance this to use {@link android.webkit.MimeTypeMap}?
   */
  public static String getMimeType(String extension) {
    // If there's a leading dot, remove it.
    if (extension.startsWith(".")) {
      extension = extension.substring(1);
    }
    return MIME_TYPES_BY_EXT.get(extension);
  }

  /**
   * Determines the mime type for the given path. Returns null if it cannot be determined.
   */
  @Nullable
  public static String determineMimeType(String path) {
    String mimeType = null;
    int lastDot = path.lastIndexOf(".");
    if (lastDot != -1) {
      String ext = path.substring(lastDot + 1);
      mimeType = getMimeType(ext);
    }
    return mimeType;
  }

  /** Adds the ability to match by whole paths as well as extension */
  public static class TypedPaths {
    private static final Map<String, String> mimeTypesByPath = new HashMap<String, String>();

    public void setMimeType(String path, String mimeType) {
      mimeTypesByPath.put(path, mimeType);
    }

    @Nullable String determineMimeType(String path) {
      String mimeType = mimeTypesByPath.get(path);
      if (mimeType == null) {
        mimeType = MimeTypesUtil.determineMimeType(path);
      }
      return mimeType;
    }
  }
}
