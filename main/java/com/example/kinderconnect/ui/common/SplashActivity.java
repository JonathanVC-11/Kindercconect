package com.example.kinderconnect.ui.common;

import android.content.Intent;
import android.os.Bundle;
// Importa la nueva clase de Splash Screen
import androidx.core.splashscreen.SplashScreen;
import androidx.appcompat.app.AppCompatActivity;
// Se eliminan los imports de Handler y R
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.auth.LoginActivity;
import com.example.kinderconnect.ui.parent.ParentMainActivity;
import com.example.kinderconnect.ui.teacher.TeacherMainActivity;
import com.example.kinderconnect.utils.Constants;

public class SplashActivity extends AppCompatActivity {
    // Se eliminan SPLASH_DELAY y el Handler
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Instala la nueva Splash Screen ANTES de super.onCreate()
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Ya no se necesita setContentView()
        // setContentView(R.layout.activity_splash);

        preferencesManager = new PreferencesManager(this);

        // Ya no se necesita el Handler, la navegación es inmediata.
        // La API de Splash Screen mantendrá la pantalla visible
        // hasta que la primera vista de la siguiente actividad se dibuje.
        checkUserSession();
    }

    private void checkUserSession() {
        Intent intent;
        if (preferencesManager.isLoggedIn()) {
            String userType = preferencesManager.getUserType();

            if (Constants.USER_TYPE_TEACHER.equals(userType)) {
                intent = new Intent(this, TeacherMainActivity.class);
            } else if (Constants.USER_TYPE_PARENT.equals(userType)) {
                intent = new Intent(this, ParentMainActivity.class);
            } else {
                // Caso de seguridad (logueado pero sin tipo)
                intent = new Intent(this, LoginActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        // Añadir flags para limpiar la pila de navegación
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cierra la SplashActivity
    }
}