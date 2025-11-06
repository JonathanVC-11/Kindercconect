package com.example.kinderconnect.ui.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // <-- AÑADIDO
import com.bumptech.glide.Glide; // <-- AÑADIDO
import com.example.kinderconnect.R; // <-- AÑADIDO
import com.example.kinderconnect.databinding.FragmentProfileBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.auth.AuthViewModel; // <-- AÑADIDO
import com.example.kinderconnect.ui.auth.LoginActivity;
import com.example.kinderconnect.utils.PermissionManager; // <-- AÑADIDO

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private PreferencesManager preferencesManager;
    private AuthViewModel authViewModel; // <-- AÑADIDO
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    // --- NUEVO MÉTODO ONCREATE ---
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Registrar el launcher para seleccionar imagen
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadProfilePicture();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferencesManager = new PreferencesManager(requireContext());
        // Inicializar el ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        displayUserInfo();
        setupListeners();
    }

    private void displayUserInfo() {
        String name = preferencesManager.getUserName();
        String email = preferencesManager.getUserEmail();
        String userType = preferencesManager.getUserType();
        String photoUrl = preferencesManager.getUserPhoto(); // <-- AÑADIDO

        binding.tvUserName.setText(name);
        binding.tvUserEmail.setText(email);

        if ("TEACHER".equals(userType)) {
            binding.tvUserType.setText("Maestra");
        } else {
            binding.tvUserType.setText("Padre/Madre de familia");
        }

        // --- AÑADIDO: Cargar foto con Glide ---
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop() // <-- Asegura que sea circular si la imagen no lo es
                    .into(binding.ivProfilePhoto);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_logo)
                    .circleCrop()
                    .into(binding.ivProfilePhoto);
        }
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        // --- AÑADIDO: Listener para cambiar foto ---
        binding.fabEditPhoto.setOnClickListener(v -> selectImage());
        binding.ivProfilePhoto.setOnClickListener(v -> selectImage()); // También en la foto
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void selectImage() {
        if (!PermissionManager.hasStoragePermission(requireContext())) {
            PermissionManager.requestStoragePermission(requireActivity());
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void uploadProfilePicture() {
        String userId = preferencesManager.getUserId();
        if (userId == null || selectedImageUri == null) return;

        setLoading(true);

        // 1. Subir la imagen a Storage
        authViewModel.uploadProfilePicture(requireContext(), userId, selectedImageUri)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null) return;
                    switch (resource.getStatus()) {
                        case LOADING:
                            break;
                        case SUCCESS:
                            String newPhotoUrl = resource.getData();
                            // 2. Actualizar la URL en Firestore
                            updateUserPhotoUrlInFirestore(userId, newPhotoUrl);
                            break;
                        case ERROR:
                            Toast.makeText(getContext(), "Error al subir: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            break;
                    }
                });
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void updateUserPhotoUrlInFirestore(String userId, String newPhotoUrl) {
        authViewModel.updateUserPhotoUrl(userId, newPhotoUrl)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null) return;
                    switch (resource.getStatus()) {
                        case LOADING:
                            break;
                        case SUCCESS:
                            // 3. Guardar en SharedPreferences y recargar la imagen
                            preferencesManager.saveUserPhoto(newPhotoUrl);
                            displayUserInfo(); // Recargar la info (incluida la foto)
                            setLoading(false);
                            Toast.makeText(getContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                            break;
                        case ERROR:
                            Toast.makeText(getContext(), "Error al actualizar: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            break;
                    }
                });
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void setLoading(boolean isLoading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.fabEditPhoto.setEnabled(!isLoading);
        binding.btnLogout.setEnabled(!isLoading);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        // Limpiar AuthViewModel (si es necesario, aunque el repo maneja el estado)
        authViewModel.logout();

        // Limpiar preferencias
        preferencesManager.clearAll();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}