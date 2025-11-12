package com.example.kinderconnect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // <-- AÑADIDO
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ActivityRegisterBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.parent.ParentMainActivity;
import com.example.kinderconnect.ui.teacher.TeacherMainActivity;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource; // <-- AÑADIDO
import com.example.kinderconnect.utils.ValidationUtils;

import java.util.Locale;
import com.example.kinderconnect.data.model.User;
import com.google.firebase.messaging.FirebaseMessaging; // <-- AÑADIDO

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private PreferencesManager preferencesManager;
    private String selectedUserType = "";
    private static final String TAG = "RegisterActivity"; // <-- AÑADIDO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (sin cambios) ...
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        preferencesManager = new PreferencesManager(this);

        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        // ... (sin cambios) ...
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.create_account);
        }
    }

    private void setupListeners() {
        // ... (sin cambios) ...
        binding.rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTeacher) {
                selectedUserType = Constants.USER_TYPE_TEACHER;
            } else if (checkedId == R.id.rbParent) {
                selectedUserType = Constants.USER_TYPE_PARENT;
            }
        });

        binding.btnRegister.setOnClickListener(v -> performRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        // ... (sin cambios en la validación) ...
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!ValidationUtils.isValidName(fullName)) {
            binding.tilFullName.setError("Ingresa tu nombre completo");
            return;
        }
        binding.tilFullName.setError(null);

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Ingresa un correo válido");
            return;
        }
        binding.tilEmail.setError(null);

        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.setError("Ingresa un teléfono válido");
            return;
        }
        binding.tilPhone.setError(null);

        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }
        binding.tilPassword.setError(null);

        if (!ValidationUtils.passwordsMatch(password, confirmPassword)) {
            binding.tilConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }
        binding.tilConfirmPassword.setError(null);

        if (selectedUserType.isEmpty()) {
            Toast.makeText(this, "Selecciona tipo de usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        authViewModel.register(email, password, fullName, phone, selectedUserType)
                .observe(this, resource -> {
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
                                binding.btnRegister.setEnabled(true);
                                Toast.makeText(this, resource.getMessage(),
                                        Toast.LENGTH_SHORT).show();
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
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(this, "Error al cargar datos del usuario",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // (Método idéntico al de LoginActivity)
    private void getAndSaveFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        // Enviar token a Firestore
                        authViewModel.updateUserToken(uid, token).observe(this, resource -> {
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}