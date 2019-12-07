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

package org.firstinspires.ftc.robotcore.external.android;

import static android.speech.tts.TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import java.util.Locale;

/**
 * A class that provides access to the Android TextToSpeech.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class AndroidTextToSpeech {
  private volatile TextToSpeech textToSpeech;
  private volatile Integer onInitStatus;

  // public methods

  /**
   * Initialize the TextToSpeech engine.
   */
  public void initialize() {
    Activity activity = AppUtil.getInstance().getRootActivity();
    textToSpeech = new TextToSpeech(activity, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        onInitStatus = status;
      }
    });
  }

  /**
   * Returns the TextToSpeech initialization status.
   */
  public String getStatus() {
    if (onInitStatus == null) {
      return "Not initialized";
    }
    int status = onInitStatus.intValue();
    if (status == TextToSpeech.SUCCESS) {
      return "Success";
    }
    return "Error code " + status;
  }

  /**
   * Returns the current language code.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  @SuppressWarnings("deprecation")
  public String getLanguageCode() {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    Locale locale = textToSpeech.getLanguage();
    if (locale != null) {
      return locale.getLanguage();
    }
    return "";
  }

  /**
   * Returns the current country code.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  @SuppressWarnings("deprecation")
  public String getCountryCode() {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    Locale locale = textToSpeech.getLanguage();
    if (locale != null) {
      return locale.getCountry();
    }
    return "";
  }

  /**
   * Returns true if the TextToSpeech engine is busy speaking.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public boolean isSpeaking() {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    return textToSpeech.isSpeaking();
  }

  /**
   * Sets the speech pitch. 1.0 is the normal pitch. Lower values will lower the tone of the
   * synthesized voice. Greater values will increase the tone of the synthesized voice.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void setPitch(float pitch) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    textToSpeech.setPitch(pitch);
  }

  /**
   * Sets the speech rate. 1.0 is the normal speech rate. Lower values will slow down the speech
   * (0.5 is half the normal speech rate). Greater values will accelerate the speech (2.0 is twice
   * the normal speech rate).
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void setSpeechRate(float speechRate) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    textToSpeech.setSpeechRate(speechRate);
  }

  /**
   * Returns true if the given language is supported. The languageCode must be an ISO 639 alpha-2
   * or alpha-3 language code, or a language subtag up to 8 characters in length.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public boolean isLanguageAvailable(String languageCode) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    Locale locale = new Locale(languageCode);
    return textToSpeech.isLanguageAvailable(locale) == LANG_COUNTRY_VAR_AVAILABLE;
  }

  /**
   * Returns true if the given language is supported. The languageCode must be an ISO 639 alpha-2
   * or alpha-3 language code, or a language subtag up to 8 characters in length. The countryCode
   * must be an ISO 3166 alpha-2 country code or a UN M.49 numeric-3 area code.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public boolean isLanguageAndCountryAvailable(String languageCode, String countryCode) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    Locale locale = new Locale(languageCode, countryCode);
    return textToSpeech.isLanguageAvailable(locale) == LANG_COUNTRY_VAR_AVAILABLE;
  }

  /**
   * Sets the language. The languageCode must be an ISO 639 alpha-2 or alpha-3 language code, or a
   * language subtag up to 8 characters in length.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void setLanguage(String languageCode) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    textToSpeech.setLanguage(new Locale(languageCode));
  }

  /**
   * Sets the language and country. The languageCode must be an ISO 639 alpha-2 or alpha-3 language
   * code, or a language subtag up to 8 characters in length. The countryCode must be an ISO 3166
   * alpha-2 country code or a UN M.49 numeric-3 area code.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  public void setLanguageAndCountry(String languageCode, String countryCode) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    textToSpeech.setLanguage(new Locale(languageCode, countryCode));
  }

  /**
   * Speaks the given text.
   *
   * @throws IllegalStateException if initialized has not been called yet.
   */
  @SuppressWarnings("deprecation")
  public void speak(String text) {
    if (textToSpeech == null) {
      throw new IllegalStateException("You forgot to call AndroidTextToSpeech.initialize!");
    }
    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null /* params */);
  }

  /**
   * Shuts down the TextToSpeech engine.
   */
  public void close() {
    if (textToSpeech != null) {
      textToSpeech.shutdown();
      onInitStatus = null;
      textToSpeech = null;
    }
  }
}
