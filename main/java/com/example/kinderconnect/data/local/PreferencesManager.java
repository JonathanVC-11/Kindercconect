package com.example.kinderconnect.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.kinderconnect.utils.Constants;

public class PreferencesManager {
    private final SharedPreferences preferences;

    // --- NUEVA CLAVE AÑADIDA ---
    private static final String PREF_USER_PHOTO = "user_photo";

    public PreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(
                Constants.PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveUserId(String userId) {
        preferences.edit().putString(Constants.PREF_USER_ID, userId).apply();
    }

    public String getUserId() {
        return preferences.getString(Constants.PREF_USER_ID, null);
    }

    public void saveUserType(String userType) {
        preferences.edit().putString(Constants.PREF_USER_TYPE, userType).apply();
    }

    public String getUserType() {
        return preferences.getString(Constants.PREF_USER_TYPE, null);
    }

    public void saveUserName(String userName) {
        preferences.edit().putString(Constants.PREF_USER_NAME, userName).apply();
    }

    public String getUserName() {
        return preferences.getString(Constants.PREF_USER_NAME, "");
    }

    public void saveUserEmail(String email) {
        preferences.edit().putString(Constants.PREF_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return preferences.getString(Constants.PREF_USER_EMAIL, "");
    }

    // --- NUEVOS MÉTODOS AÑADIDOS ---
    public void saveUserPhoto(String photoUrl) {
        preferences.edit().putString(PREF_USER_PHOTO, photoUrl).apply();
    }

    public String getUserPhoto() {
        return preferences.getString(PREF_USER_PHOTO, null);
    }
    // -------------------------------

    public void setLoggedIn(boolean isLoggedIn) {
        preferences.edit().putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }

    public boolean isTeacher() {
        return Constants.USER_TYPE_TEACHER.equals(getUserType());
    }

    public boolean isParent() {
        return Constants.USER_TYPE_PARENT.equals(getUserType());
    }
}