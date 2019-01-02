package com.amalbit.animationongooglemap.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceHelper {

  public final static String UPDATE_INTERVAL = "UPDATE_INTERVAL";

  private final static int UPDATE_INTERVAL_DEFAULT = 25;

  private final static String PREF_FILE = "PREF";

  /**
   * Set a string shared preference
   * @param key - Key to set shared preference
   * @param value - Value for the key
   */
  static void setSharedPreferenceString(Context context, String key, String value){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(key, value);
    editor.apply();
  }

  /**
   * Set a integer shared preference
   * @param value - Value for the key
   */
  public static void setSharedPreferenceInterval(Context context, int value){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putInt(UPDATE_INTERVAL, value);
    editor.apply();
  }

  /**
   * Set a Boolean shared preference
   * @param key - Key to set shared preference
   * @param value - Value for the key
   */
  static void setSharedPreferenceBoolean(Context context, String key, boolean value){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putBoolean(key, value);
    editor.apply();
  }

  /**
   * Get a string shared preference
   * @param key - Key to look up in shared preferences.
   * @param defValue - Default value to be returned if shared preference isn't found.
   * @return value - String containing value of the shared preference if found.
   */
  static String getSharedPreferenceString(Context context, String key, String defValue){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    return settings.getString(key, defValue);
  }

  /**
   * Get a integer shared preference
   * @return value - String containing value of the shared preference if found.
   */
  public static int getSharedPreferenceInterval(Context context){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    return settings.getInt(UPDATE_INTERVAL, UPDATE_INTERVAL_DEFAULT);
  }

  /**
   * Get a boolean shared preference
   * @param key - Key to look up in shared preferences.
   * @param defValue - Default value to be returned if shared preference isn't found.
   * @return value - String containing value of the shared preference if found.
   */
  static boolean getSharedPreferenceBoolean(Context context, String key, boolean defValue){
    SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
    return settings.getBoolean(key, defValue);
  }
}
