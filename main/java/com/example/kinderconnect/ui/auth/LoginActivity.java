package com.example.kinderconnect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // <-- AÑADIDO
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
import com.example.kinderconnect.data.model.User;
import com.google.firebase.messaging.FirebaseMessaging; // <-- AÑADIDO

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private PreferencesManager preferencesManager;
    private static final String TAG = "LoginActivity"; // <-- AÑADIDO

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
        // ... (sin cambios) ...
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void performLogin() {
        // ... (sin cambios en la validación) ...
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

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

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        authViewModel.login(email, password).observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        break;
                    case SUCCESS:
                        if (resource.getData() != null) {
                            String uid = resource.getData().getUid();
                            loadUserData(uid); // Esto ya estaba
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
                    case LOADING:
                        binding.progressBar.setVisibility(View.VISIBLE);
                        break;
                    case SUCCESS:
                        if (resource.getData() != null) {

                            // --- INICIO DE CÓDIGO AÑADIDO ---
                            // 1. Guardar el token en Firestore
                            getAndSaveFcmToken(uid);
                            // --- FIN DE CÓDIGO AÑADIDO ---

                            // 2. Guardar sesión y navegar
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

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private void getAndSaveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        // Enviar token a Firestore
                        authViewModel.updateUserToken(uid, token).observe(this, resource -> {
                            // Opcional: manejar la respuesta, pero para el login no es crítico
                            if (resource.getStatus() == Resource.Status.SUCCESS) {
                                Log.d(TAG, "FCM Token guardado en Firestore exitosamente.");
                            } else if (resource.getStatus() == Resource.Status.ERROR) {
                                Log.e(TAG, "Error al guardar FCM Token: " + resource.getMessage());
                            }
                        });
                    } else {
                        Log.e(TAG, "No se pudo obtener el FCM Token (es nulo).");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener FCM Token", e);
                });
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    private void saveUserSession(User user) {
        // ... (sin cambios) ...
        preferencesManager.setLoggedIn(true);
        preferencesManager.saveUserId(user.getUid());
        preferencesManager.saveUserType(user.getUserType());
        preferencesManager.saveUserName(user.getFullName());
        preferencesManager.saveUserEmail(user.getEmail());
        preferencesManager.saveUserPhoto(user.getPhotoUrl());
    }

    private void navigateToMainScreen(String userType) {
        // ... (sin cambios) ...
        Intent intent;
        if (Constants.USER_TYPE_TEACHER.equals(userType)) {
            intent = new Intent(this, TeacherMainActivity.class);
        } else {
            intent = new Intent(this, ParentMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}