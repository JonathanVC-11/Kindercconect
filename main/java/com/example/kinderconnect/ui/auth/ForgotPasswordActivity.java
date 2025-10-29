package com.example.kinderconnect.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ActivityForgotPasswordBinding;
import com.example.kinderconnect.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recuperar contraseña");
        }
    }

    private void setupListeners() {
        binding.btnSendEmail.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = binding.etEmail.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Ingresa un correo válido");
            return;
        }
        binding.tilEmail.setError(null);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSendEmail.setEnabled(false);

        authViewModel.resetPassword(email).observe(this, resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Se ha enviado un correo para restablecer tu contraseña",
                                Toast.LENGTH_LONG).show();
                        finish();
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSendEmail.setEnabled(true);
                        Toast.makeText(this, resource.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
