package com.example.kinderconnect.ui.teacher;

import android.content.Context;
import android.content.Intent; // <-- AÑADIDO
import android.net.Uri; // <-- AÑADIDO
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherGalleryBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.ui.adapters.GalleryAdapter;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.PermissionManager;
import com.example.kinderconnect.utils.Resource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
                    } else {
                        Toast.makeText(requireContext(), "Captura cancelada", Toast.LENGTH_SHORT).show();
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

        binding.fabAddMedia.setEnabled(false);
        loadTeacherGroup();
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerView.setAdapter(adapter);

        // --- CLICK LISTENER MODIFICADO ---
        adapter.setOnItemClickListener(item -> {
            // Antes: Toast.makeText(requireContext(), "Ver: " + item.getDescription(), Toast.LENGTH_SHORT).show();
            openMediaViewer(item); // Ahora llama al visor
        });
        // -----------------------------------

        adapter.setOnItemLongClickListener(item -> {
            showDeleteDialog(item);
        });
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    /**
     * Abre un visor nativo (Galería, Video Player) para el item seleccionado.
     */
    private void openMediaViewer(GalleryItem item) {
        if (item == null || item.getMediaUrl() == null || item.getMediaUrl().isEmpty()) {
            Toast.makeText(requireContext(), "No se pudo encontrar el archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usamos la URL original (mediaUrl), no el thumbnail
        Uri mediaUri = Uri.parse(item.getMediaUrl());
        Intent intent = new Intent(Intent.ACTION_VIEW);

        String mimeType;
        if (Constants.MEDIA_VIDEO.equals(item.getMediaType())) {
            mimeType = "video/*";
        } else {
            mimeType = "image/*";
        }

        intent.setDataAndType(mediaUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No hay ninguna aplicación para abrir este archivo", Toast.LENGTH_SHORT).show();
        }
    }
    // ----------------------------


    private void setupListeners() {
        binding.fabAddMedia.setOnClickListener(v -> showMediaOptions());
    }

    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;

            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                teacherGroupName = resource.getData().get(0).getGroupName();
                loadGallery();
                binding.fabAddMedia.setEnabled(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
                binding.emptyView.tvEmptyTitle.setText("No tienes alumnos");
                binding.emptyView.tvEmptySubtitle.setText("No puedes subir fotos a la galería sin un grupo asignado.");
                binding.fabAddMedia.setEnabled(false);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                binding.emptyView.tvEmptyTitle.setText("Error");
                binding.emptyView.tvEmptySubtitle.setText("Error al cargar datos del grupo.");
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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    private void takePhoto() {
        if (!PermissionManager.hasCameraPermission(requireContext())) {
            PermissionManager.requestCameraPermission(requireActivity());
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(requireContext(), "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                capturedPhotoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile
                );

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedPhotoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
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
        if (selectedMediaUri == null) {
            Toast.makeText(requireContext(), "No se seleccionó ningún archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (teacherGroupName == null) {
            Toast.makeText(requireContext(), "No se pudo determinar tu grupo", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = preferencesManager.getUserId();
        GalleryItem galleryItem = new GalleryItem(
                teacherId,
                "",
                selectedMediaType,
                "Actividad escolar"
        );
        galleryItem.setGroupName(teacherGroupName);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.fabAddMedia.setEnabled(false);

        viewModel.uploadMedia(galleryItem, selectedMediaUri, requireContext())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null) {
                        switch (resource.getStatus()) {
                            case LOADING:
                                break;
                            case SUCCESS:
                                binding.progressBar.setVisibility(View.GONE);
                                binding.fabAddMedia.setEnabled(true);
                                Toast.makeText(requireContext(),
                                        "Contenido subido correctamente",
                                        Toast.LENGTH_SHORT).show();
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
        if (teacherGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
            binding.emptyView.tvEmptyTitle.setText("Error");
            binding.emptyView.tvEmptySubtitle.setText("No se pudo determinar el grupo.");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE);

        viewModel.getGalleryByGroup(teacherGroupName).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.GONE);
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        if (resource.getData() != null && !resource.getData().isEmpty()) {
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            binding.emptyView.getRoot().setVisibility(View.GONE);
                            adapter.submitList(resource.getData());
                        } else {
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_gallery);
                            binding.emptyView.tvEmptyTitle.setText("Galería Vacía");
                            binding.emptyView.tvEmptySubtitle.setText("Presiona el botón '+' para subir la primera foto o video.");
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                        binding.emptyView.tvEmptyTitle.setText("Error");
                        binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
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
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}