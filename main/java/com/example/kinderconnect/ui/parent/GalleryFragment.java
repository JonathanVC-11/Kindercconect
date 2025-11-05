package com.example.kinderconnect.ui.parent;

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
import com.example.kinderconnect.databinding.FragmentGalleryBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.adapters.GalleryAdapter;
import com.example.kinderconnect.utils.Resource; // Asegúrate que esté importado

public class GalleryFragment extends Fragment {
    private FragmentGalleryBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager;
    private GalleryAdapter adapter;
    private String studentGroupName; // Usaremos esta

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
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        loadStudentAndGallery(); // Este método ahora carga el grupo y luego la galería
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter();
        // Asegúrate de que el ID del RecyclerView en fragment_gallery.xml sea "recyclerView"
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            // Acción al hacer clic (ej: abrir a pantalla completa)
            // Por ahora, solo un Toast
            Toast.makeText(requireContext(), "Ver: " + (item.getDescription() != null ? item.getDescription() : "Media"),
                    Toast.LENGTH_SHORT).show();
            // Aquí podrías navegar a otro fragmento o actividad para ver la imagen/video
        });
    }

    // --- MÉTODO CORREGIDO ---
    private void loadStudentAndGallery() {
        String parentId = preferencesManager.getUserId();
        if (parentId == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setText("Error: No se pudo identificar al usuario.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (!isAdded() || binding == null) return; // Verificar si el fragmento sigue vivo

            if (resource != null && resource.getStatus() == Resource.Status.SUCCESS) {
                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    // Obtenemos el nombre del grupo del primer alumno
                    studentGroupName = resource.getData().get(0).getGroupName();
                    if (studentGroupName != null && !studentGroupName.isEmpty()) {
                        loadGallery(); // <-- Llamamos a cargar galería SOLO si tenemos grupo
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setText("El alumno no tiene un grupo asignado.");
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    // El padre no tiene hijos asignados
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvEmpty.setText("No hay alumnos asignados a esta cuenta.");
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            } else if (resource != null && resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("Error al cargar datos del alumno: " + resource.getMessage());
                binding.tvEmpty.setVisibility(View.VISIBLE);
                //Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show(); // Opcional
            }
            // No hacer nada en LOADING, ya está visible el progress bar
        });
    }

    // --- MÉTODO CORREGIDO ---
    private void loadGallery() {
        if (studentGroupName == null) { // Doble chequeo por si acaso
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setText("No se pudo determinar el grupo del alumno.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // El progressBar ya debería estar visible desde loadStudentAndGallery
        //binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);


        viewModel.getGalleryByGroup(studentGroupName).observe(getViewLifecycleOwner(), resource -> {
            if (!isAdded() || binding == null) return; // Verificar si el fragmento sigue vivo

            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        // Mantener progressBar visible
                        break;
                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        if (resource.getData() != null && !resource.getData().isEmpty()) {
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            binding.tvEmpty.setVisibility(View.GONE);
                            adapter.submitList(resource.getData());
                        } else {
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.tvEmpty.setText("No hay fotos o videos en la galería del grupo.");
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                        }
                        break;
                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.tvEmpty.setText("Error al cargar la galería: " + resource.getMessage());
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        //Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show(); // Opcional
                        break;
                }
            } else {
                // Caso inesperado: resource es null
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("Ocurrió un error inesperado al cargar la galería.");
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // ¡Importante! Limpiar la referencia al binding
    }
}