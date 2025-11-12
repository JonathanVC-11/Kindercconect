package com.example.kinderconnect.ui.parent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentGalleryBinding;
import com.example.kinderconnect.data.local.PreferencesManager; // <-- AÑADIDO
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.ui.adapters.GalleryAdapter;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource;

public class GalleryFragment extends Fragment {
    private FragmentGalleryBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager; // <-- AÑADIDO
    private GalleryAdapter adapter;

    private String studentGroupName;
    private String studentName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        preferencesManager = new PreferencesManager(requireContext()); // <-- AÑADIDO

        // --- INICIO DE CÓDIGO MODIFICADO ---
        if (getArguments() != null && getArguments().getString("groupName") != null) {
            // Opción 1: Venimos desde los accesos rápidos (Home)
            studentGroupName = getArguments().getString("groupName");
            studentName = getArguments().getString("studentName");
        } else {
            // Opción 2: Venimos desde la barra de navegación (si se añadiera)
            studentGroupName = preferencesManager.getCurrentGroupName();
            studentName = preferencesManager.getCurrentStudentName();
        }

        if (studentGroupName == null || studentName == null) {
            Toast.makeText(requireContext(), "Error: No se pudo cargar el grupo", Toast.LENGTH_SHORT).show();
            return;
        }
        // --- FIN DE CÓDIGO MODIFICADO ---

        setupRecyclerView();
        loadGallery(); // Llamar a cargar galería directamente
    }

    private void setupRecyclerView() {
        // ... (sin cambios) ...
        adapter = new GalleryAdapter();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            openMediaViewer(item);
        });
    }

    private void openMediaViewer(GalleryItem item) {
        // ... (sin cambios) ...
        if (item == null || item.getMediaUrl() == null || item.getMediaUrl().isEmpty()) {
            Toast.makeText(requireContext(), "No se pudo encontrar el archivo", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void loadGallery() {
        // ... (sin cambios) ...
        if (studentGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
            binding.emptyView.tvEmptyTitle.setText("Error");
            binding.emptyView.tvEmptySubtitle.setText("No se pudo determinar el grupo del alumno.");
            return;
        }

        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);


        viewModel.getGalleryByGroup(studentGroupName).observe(getViewLifecycleOwner(), resource -> {
            if (!isAdded() || binding == null) return;

            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
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
                            binding.emptyView.tvEmptyTitle.setText("Galería vacía");
                            binding.emptyView.tvEmptySubtitle.setText("No hay fotos o videos en la galería del grupo.");
                        }
                        break;
                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                        binding.emptyView.tvEmptyTitle.setText("Error");
                        binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
                        break;
                }
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                binding.emptyView.tvEmptyTitle.setText("Error");
                binding.emptyView.tvEmptySubtitle.setText("Ocurrió un error inesperado al cargar la galería.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}