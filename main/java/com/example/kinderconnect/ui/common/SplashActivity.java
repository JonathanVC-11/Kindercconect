package com.example.kinderconnect.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kinderconnect.R;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.auth.LoginActivity;
import com.example.kinderconnect.ui.parent.ParentMainActivity;
import com.example.kinderconnect.ui.teacher.TeacherMainActivity;
import com.example.kinderconnect.utils.Constants;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        preferencesManager = new PreferencesManager(this);

        new Handler().postDelayed(() -> {
            checkUserSession();
        }, SPLASH_DELAY);
    }

    private void checkUserSession() {
        if (preferencesManager.isLoggedIn()) {
            String userType = preferencesManager.getUserType();

            if (Constants.USER_TYPE_TEACHER.equals(userType)) {
                startActivity(new Intent(this, TeacherMainActivity.class));
            } else if (Constants.USER_TYPE_PARENT.equals(userType)) {
                startActivity(new Intent(this, ParentMainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
