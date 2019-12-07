// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller.runtime;

import android.webkit.JavascriptInterface;
import org.firstinspires.ftc.robotcore.external.android.AndroidTextToSpeech;

/**
 * A class that provides JavaScript access to the Android TextToSpeech.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
class AndroidTextToSpeechAccess extends Access {
  private final AndroidTextToSpeech androidTextToSpeech;

  AndroidTextToSpeechAccess(BlocksOpMode blocksOpMode, String identifier) {
    super(blocksOpMode, identifier, "AndroidTextToSpeech");
    androidTextToSpeech = new AndroidTextToSpeech();
  }

  // Access methods

  @Override
  void close() {
    androidTextToSpeech.close();
  }

  // Javascript methods

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void initialize() {
    startBlockExecution(BlockType.FUNCTION, ".initialize");
    androidTextToSpeech.initialize();
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public String getStatus() {
    startBlockExecution(BlockType.GETTER, ".Status");
    return androidTextToSpeech.getStatus();
  }

  @SuppressWarnings({"unused", "deprecation"})
  @JavascriptInterface
  public String getLanguageCode() {
    startBlockExecution(BlockType.GETTER, ".LanguageCode");
    try {
      return androidTextToSpeech.getLanguageCode();
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return "";
  }

  @SuppressWarnings({"unused", "deprecation"})
  @JavascriptInterface
  public String getCountryCode() {
    startBlockExecution(BlockType.GETTER, ".CountryCode");
    try {
      return androidTextToSpeech.getCountryCode();
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return "";
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean getIsSpeaking() {
    startBlockExecution(BlockType.GETTER, ".IsSpeaking");
    try {
      return androidTextToSpeech.isSpeaking();
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setPitch(float pitch) {
    startBlockExecution(BlockType.SETTER, ".Pitch");
    try {
      androidTextToSpeech.setPitch(pitch);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setSpeechRate(float speechRate) {
    startBlockExecution(BlockType.SETTER, ".SpeechRate");
    try {
      androidTextToSpeech.setSpeechRate(speechRate);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isLanguageAvailable(String languageCode) {
    startBlockExecution(BlockType.FUNCTION, ".isLanguageAvailable");
    try {
      return androidTextToSpeech.isLanguageAvailable(languageCode);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public boolean isLanguageAndCountryAvailable(String languageCode, String countryCode) {
    startBlockExecution(BlockType.FUNCTION, ".isLanguageAndCountryAvailable");
    try {
      return androidTextToSpeech.isLanguageAndCountryAvailable(languageCode, countryCode);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return false;
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setLanguage(String languageCode) {
    startBlockExecution(BlockType.FUNCTION, ".setLanguage");
    try {
      androidTextToSpeech.setLanguage(languageCode);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
  }

  @SuppressWarnings("unused")
  @JavascriptInterface
  public void setLanguageAndCountry(String languageCode, String countryCode) {
    startBlockExecution(BlockType.FUNCTION, ".setLanguageAndCountry");
    try {
      androidTextToSpeech.setLanguageAndCountry(languageCode, countryCode);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
  }

  @SuppressWarnings({"unused", "deprecation"})
  @JavascriptInterface
  public void speak(String text) {
    startBlockExecution(BlockType.FUNCTION, ".speak");
    try {
      androidTextToSpeech.speak(text);
    } catch (IllegalStateException e) {
      reportWarning("You forgot to call AndroidTextToSpeech.initialize!");
    }
  }
}
