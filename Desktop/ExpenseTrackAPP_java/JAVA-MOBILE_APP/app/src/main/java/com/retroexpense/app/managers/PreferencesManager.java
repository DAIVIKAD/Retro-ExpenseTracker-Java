package com.retroexpense.app.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "RetroExpense";
    private static final String KEY_PIN = "user_pin";
    private static final String KEY_BUDGET = "monthly_budget";
    private static final String KEY_SOUND = "sound_effects";
    private static final String KEY_NOTIFICATIONS = "notifications";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void savePin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public String getPin() {
        return prefs.getString(KEY_PIN, "");
    }

    public boolean hasPin() {
        return !getPin().isEmpty();
    }

    public void saveMonthlyBudget(float budget) {
        prefs.edit().putFloat(KEY_BUDGET, budget).apply();
    }

    public float getMonthlyBudget() {
        return prefs.getFloat(KEY_BUDGET, 0f);
    }

    public void setSoundEffectsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public boolean isSoundEffectsEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public SharedPreferences getRawPreferences() {
        return prefs;
    }
}


