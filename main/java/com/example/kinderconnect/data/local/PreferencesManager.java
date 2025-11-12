package com.example.kinderconnect.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.kinderconnect.utils.Constants;

public class PreferencesManager {
    private final SharedPreferences preferences;

    private static final String PREF_USER_PHOTO = "user_photo";

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Claves para guardar el último alumno seleccionado
    private static final String PREF_CURRENT_STUDENT_ID = "current_student_id";
    private static final String PREF_CURRENT_STUDENT_NAME = "current_student_name";
    private static final String PREF_CURRENT_GROUP_NAME = "current_group_name";
    // --- FIN DE CÓDIGO AÑADIDO ---

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

    public void saveUserPhoto(String photoUrl) {
        preferences.edit().putString(PREF_USER_PHOTO, photoUrl).apply();
    }

    public String getUserPhoto() {
        return preferences.getString(PREF_USER_PHOTO, null);
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Métodos para gestionar el alumno activo
    public void saveCurrentStudent(String id, String name, String groupName) {
        preferences.edit()
                .putString(PREF_CURRENT_STUDENT_ID, id)
                .putString(PREF_CURRENT_STUDENT_NAME, name)
                .putString(PREF_CURRENT_GROUP_NAME, groupName)
                .apply();
    }

    public String getCurrentStudentId() {
        return preferences.getString(PREF_CURRENT_STUDENT_ID, null);
    }

    public String getCurrentStudentName() {
        return preferences.getString(PREF_CURRENT_STUDENT_NAME, null);
    }

    public String getCurrentGroupName() {
        return preferences.getString(PREF_CURRENT_GROUP_NAME, null);
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    public void setLoggedIn(boolean isLoggedIn) {
        preferences.edit().putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public void clearAll() {
        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Al cerrar sesión, también borramos al alumno guardado
        preferences.edit().clear().apply();
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    public boolean isTeacher() {
        return Constants.USER_TYPE_TEACHER.equals(getUserType());
    }

    public boolean isParent() {
        return Constants.USER_TYPE_PARENT.equals(getUserType());
    }
}