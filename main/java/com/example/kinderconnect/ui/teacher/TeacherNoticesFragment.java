package com.example.kinderconnect.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherNoticesBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.parent.adapters.NoticeAdapter;
import com.example.kinderconnect.utils.Resource; // <-- AÑADIDO

public class TeacherNoticesFragment extends Fragment {
    private FragmentTeacherNoticesBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private NoticeAdapter adapter;

    // --- AÑADIR VARIABLE ---
    private String teacherGroupName = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTeacherNoticesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        setupListeners();

        // --- MODIFICADO ---
        loadTeacherGroup();
    }

    private void setupRecyclerView() {
        adapter = new NoticeAdapter(preferencesManager.getUserId());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(notice -> {
            // Ver detalle o editar
            Toast.makeText(requireContext(), notice.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        binding.fabPublish.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_notices_to_publish));
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                teacherGroupName = resource.getData().get(0).getGroupName();
                loadNotices(); // Cargar avisos AHORA que sabemos el grupo
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                // Maestra sin alumnos, no puede cargar avisos de grupo
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("No tienes alumnos asignados");
                binding.tvEmpty.setVisibility(View.VISIBLE);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotices() {
        // --- VALIDACIÓN AÑADIDA ---
        if (teacherGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // --- CORRECCIÓN DE "Grupo A" ---
        // viewModel.getNoticesByGroup("Grupo A").observe(getViewLifecycleOwner(), resource -> { // <-- ERROR
        viewModel.getNoticesByGroup(teacherGroupName).observe(getViewLifecycleOwner(), resource -> { // <-- CORREGIDO
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