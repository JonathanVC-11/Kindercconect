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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentProfileBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.auth.AuthViewModel;
import com.example.kinderconnect.ui.auth.LoginActivity;
import com.example.kinderconnect.utils.PermissionManager;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private PreferencesManager preferencesManager;
    private AuthViewModel authViewModel;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

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
        String photoUrl = preferencesManager.getUserPhoto();

        binding.tvUserName.setText(name);
        binding.tvUserEmail.setText(email);

        if ("TEACHER".equals(userType)) {
            binding.tvUserType.setText("Maestra");
            // MOSTRAR BOTÓN DE GRUPO SI ES MAESTRA
            binding.cardManageGroup.setVisibility(View.VISIBLE);
        } else {
            binding.tvUserType.setText("Padre/Madre de familia");
            // OCULTAR BOTÓN SI ES PADRE
            binding.cardManageGroup.setVisibility(View.GONE);
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop()
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

        binding.fabEditPhoto.setOnClickListener(v -> selectImage());
        binding.ivProfilePhoto.setOnClickListener(v -> selectImage());

        binding.cardManageGroup.setOnClickListener(v -> {
            // Navegar al nuevo fragmento (la acción se definirá en el nav_graph)
            // Esta acción solo existe en teacher_nav_graph, por eso funciona
            try {
                Navigation.findNavController(v).navigate(R.id.action_profile_to_manage_group);
            } catch (Exception e) {
                // Si falla (ej. estamos en parent_nav_graph), no hacer nada o mostrar toast
                Toast.makeText(requireContext(), "Error de navegación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectImage() {
        if (!PermissionManager.hasStoragePermission(requireContext())) {
            PermissionManager.requestStoragePermission(requireActivity());
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePicture() {
        String userId = preferencesManager.getUserId();
        if (userId == null || selectedImageUri == null) return;

        setLoading(true);

        authViewModel.uploadProfilePicture(requireContext(), userId, selectedImageUri)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null) return;
                    switch (resource.getStatus()) {
                        case LOADING:
                            break;
                        case SUCCESS:
                            String newPhotoUrl = resource.getData();
                            updateUserPhotoUrlInFirestore(userId, newPhotoUrl);
                            break;
                        case ERROR:
                            Toast.makeText(getContext(), "Error al subir: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                            setLoading(false);
                            break;
                    }
                });
    }

    private void updateUserPhotoUrlInFirestore(String userId, String newPhotoUrl) {
        authViewModel.updateUserPhotoUrl(userId, newPhotoUrl)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null) return;
                    switch (resource.getStatus()) {
                        case LOADING:
                            break;
                        case SUCCESS:
                            preferencesManager.saveUserPhoto(newPhotoUrl);
                            displayUserInfo();
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
        authViewModel.logout();
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