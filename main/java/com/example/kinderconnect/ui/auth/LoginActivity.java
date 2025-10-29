package com.example.kinderconnect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.kinderconnect.databinding.ActivityLoginBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.parent.ParentMainActivity;
import com.example.kinderconnect.ui.teacher.TeacherMainActivity;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;
import com.example.kinderconnect.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        preferencesManager = new PreferencesManager(this);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validar campos
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Ingresa un correo válido");
            return;
        }
        binding.tilEmail.setError(null);

        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }
        binding.tilPassword.setError(null);

        // Mostrar loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        // Realizar login
        authViewModel.login(email, password).observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        // Ya está mostrando el progress
                        break;

                    case SUCCESS:
                        if (resource.getData() != null) {
                            String uid = resource.getData().getUid();
                            loadUserData(uid);
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void loadUserData(String uid) {
        authViewModel.getUserData(uid).observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        if (resource.getData() != null) {
                            saveUserSession(resource.getData());
                            navigateToMainScreen(resource.getData().getUserType());
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(this, "Error al cargar datos del usuario",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void saveUserSession(com.example.kinderconnect.data.model.User user) {
        preferencesManager.setLoggedIn(true);
        preferencesManager.saveUserId(user.getUid());
        preferencesManager.saveUserType(user.getUserType());
        preferencesManager.saveUserName(user.getFullName());
        preferencesManager.saveUserEmail(user.getEmail());
    }

    private void navigateToMainScreen(String userType) {
        Intent intent;
        if (Constants.USER_TYPE_TEACHER.equals(userType)) {
            intent = new Intent(this, TeacherMainActivity.class);
        } else {
            intent = new Intent(this, ParentMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
