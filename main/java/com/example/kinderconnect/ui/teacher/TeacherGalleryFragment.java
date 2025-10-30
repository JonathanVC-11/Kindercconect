package com.example.kinderconnect.ui.teacher;

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
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.kinderconnect.databinding.FragmentTeacherGalleryBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.ui.adapters.GalleryAdapter;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.PermissionManager;
import com.example.kinderconnect.utils.Resource; // <-- AÑADIDO

public class TeacherGalleryFragment extends Fragment {
    private FragmentTeacherGalleryBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private GalleryAdapter adapter;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri selectedMediaUri;
    private Uri capturedPhotoUri;
    private String selectedMediaType;

    // --- AÑADIR VARIABLE ---
    private String teacherGroupName = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        selectedMediaUri = result.getData().getData();
                        uploadMedia();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        selectedMediaUri = capturedPhotoUri;
                        uploadMedia();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTeacherGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        setupListeners();

        // --- MODIFICAR CARGA DE DATOS ---
        binding.fabAddMedia.setEnabled(false); // Deshabilitar FAB hasta cargar grupo
        loadTeacherGroup();
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            // Abrir imagen/video en pantalla completa
            Toast.makeText(requireContext(), "Ver: " + item.getDescription(),
                    Toast.LENGTH_SHORT).show();
        });

        adapter.setOnItemLongClickListener(item -> {
            showDeleteDialog(item);
        });
    }

    private void setupListeners() {
        binding.fabAddMedia.setOnClickListener(v -> showMediaOptions());
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                // Asumimos que la maestra solo tiene un grupo.
                teacherGroupName = resource.getData().get(0).getGroupName();
                loadGallery(); // Cargar galería AHORA que sabemos el grupo
                binding.fabAddMedia.setEnabled(true); // Habilitar FAB
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                // Maestra sin alumnos
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("No tienes alumnos asignados");
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.fabAddMedia.setEnabled(false); // No puede subir fotos si no tiene grupo
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMediaOptions() {
        String[] options = {"Tomar foto", "Seleccionar foto", "Seleccionar video"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Agregar contenido")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectedMediaType = Constants.MEDIA_IMAGE;
                            takePhoto();
                            break;
                        case 1:
                            selectedMediaType = Constants.MEDIA_IMAGE;
                            selectImage();
                            break;
                        case 2:
                            selectedMediaType = Constants.MEDIA_VIDEO;
                            selectVideo();
                            break;
                    }
                })
                .show();
    }

    private void takePhoto() {
        if (!PermissionManager.hasCameraPermission(requireContext())) {
            PermissionManager.requestCameraPermission(requireActivity());
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void selectImage() {
        if (!PermissionManager.hasStoragePermission(requireContext())) {
            PermissionManager.requestStoragePermission(requireActivity());
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mediaPickerLauncher.launch(intent);
    }

    private void selectVideo() {
        if (!PermissionManager.hasStoragePermission(requireContext())) {
            PermissionManager.requestStoragePermission(requireActivity());
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        mediaPickerLauncher.launch(intent);
    }

    private void uploadMedia() {
        if (selectedMediaUri == null) return;

        // --- AÑADIR VALIDACIÓN ---
        if (teacherGroupName == null) {
            Toast.makeText(requireContext(), "No se pudo determinar tu grupo para subir la foto", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = preferencesManager.getUserId();
        GalleryItem galleryItem = new GalleryItem(
                teacherId,
                "",
                selectedMediaType,
                "Actividad escolar"
        );

        // --- CORRECCIÓN DE "Grupo A" ---
        // galleryItem.setGroupName("Grupo A"); // <-- ESTE ES EL ERROR
        galleryItem.setGroupName(teacherGroupName); // <-- CORREGIDO

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.fabAddMedia.setEnabled(false);

        viewModel.uploadMedia(galleryItem, selectedMediaUri)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null) {
                        switch (resource.getStatus()) {
                            case LOADING:
                                // Ya está mostrando el progress
                                break;
                            case SUCCESS:
                                binding.progressBar.setVisibility(View.GONE);
                                binding.fabAddMedia.setEnabled(true);
                                Toast.makeText(requireContext(),
                                        "Contenido subido correctamente",
                                        Toast.LENGTH_SHORT).show();
                                // loadGallery(); // No es necesario, el listener lo actualiza
                                break;

                            case ERROR:
                                binding.progressBar.setVisibility(View.GONE);
                                binding.fabAddMedia.setEnabled(true);
                                Toast.makeText(requireContext(), resource.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    private void loadGallery() {
        // --- AÑADIR VALIDACIÓN ---
        if (teacherGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return; // No cargar galería si no hay grupo
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // --- CORRECCIÓN DE "Grupo A" ---
        // viewModel.getGalleryByGroup("Grupo A").observe(getViewLifecycleOwner(), resource -> { // <-- ERROR
        viewModel.getGalleryByGroup(teacherGroupName).observe(getViewLifecycleOwner(), resource -> { // <-- CORREGIDO
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        binding.progressBar.setVisibility(View.VISIBLE);
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        if (resource.getData() != null && !resource.getData().isEmpty()) {
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            binding.tvEmpty.setVisibility(View.GONE);
                            adapter.submitList(resource.getData());
                        } else {
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), resource.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void showDeleteDialog(GalleryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar contenido")
                .setMessage("¿Deseas eliminar este elemento?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteItem(item))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteItem(GalleryItem item) {
        viewModel.deleteGalleryItem(item.getItemId())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null && resource.getStatus() ==
                            com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                        Toast.makeText(requireContext(),
                                "Elemento eliminado",
                                Toast.LENGTH_SHORT).show();
                        // loadGallery(); // No es necesario, el listener lo actualiza
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}