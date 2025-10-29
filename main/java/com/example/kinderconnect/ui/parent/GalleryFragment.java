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

public class GalleryFragment extends Fragment {
    private FragmentGalleryBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager;
    private GalleryAdapter adapter;
    private String studentId;

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
        loadStudentAndGallery();
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
    }

    private void loadStudentAndGallery() {
        String parentId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    studentId = resource.getData().get(0).getStudentId();
                    loadGallery();
                }
            }
        });
    }

    private void loadGallery() {
        viewModel.getGalleryByStudent(studentId).observe(getViewLifecycleOwner(), resource -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
