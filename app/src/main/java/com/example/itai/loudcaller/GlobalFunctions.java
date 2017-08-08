package com.example.itai.loudcaller;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Set;

/**
 * Proudly written by Itai on 05/08/2017.
 */

public class GlobalFunctions {
    public static String SETTINGS_NUMBERS = "SETTINGS_NUMBERS";
    public static String SETTINGS_IS_ON = "SETTINGS_IS_ON";
    public static final String PREFS_NAME = "MySettings";

    public static ArrayList<String> loadNumbers(Context context) {
        ArrayList<String> allAddedPhoneNumbers = new ArrayList<>();
        SharedPreferences settings = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        Set<String> insults = settings.getStringSet(SETTINGS_NUMBERS, null);
        if (insults != null && !insults.isEmpty()) {
            for (String insult : insults) {
                allAddedPhoneNumbers.add(insult);
            }
        }
        return allAddedPhoneNumbers;
    }

    public static void saveToSettings(String settingString, Object data, Context context) {
        SharedPreferences settings = context.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if (data instanceof Integer) {
            editor.putInt(settingString, (int) data);
        } else if (data instanceof Float) {
            editor.putFloat(settingString, (float) data);
        } else if (data instanceof String) {
            editor.putString(settingString, (String) data);
        } else if (data instanceof Boolean) {
            editor.putBoolean(settingString, (Boolean) data);
        } else if (data instanceof Set) {
            editor.putStringSet(settingString, (Set) data);
        }
        editor.commit();
    }

}
