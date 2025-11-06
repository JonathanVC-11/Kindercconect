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
import com.example.kinderconnect.utils.Resource;

public class TeacherNoticesFragment extends Fragment {
    private FragmentTeacherNoticesBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private NoticeAdapter adapter;
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
        loadTeacherGroup();
    }

    private void setupRecyclerView() {
        adapter = new NoticeAdapter(preferencesManager.getUserId());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(notice -> {
            Toast.makeText(requireContext(), notice.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        binding.fabPublish.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_notices_to_publish));
    }

    // --- LÓGICA DE ESTADO VACÍO MODIFICADA ---
    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return; // Vista destruida

            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                teacherGroupName = resource.getData().get(0).getGroupName();
                loadNotices(); // Cargar avisos AHORA que sabemos el grupo
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                // Maestra sin alumnos, no puede cargar avisos de grupo
                binding.progressBar.setVisibility(View.GONE);

                // --- CORRECCIÓN ---
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
                binding.emptyView.tvEmptyTitle.setText("No tienes alumnos");
                binding.emptyView.tvEmptySubtitle.setText("No puedes publicar avisos de grupo sin alumnos asignados.");
                // --- FIN CORRECCIÓN ---

            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotices() {
        if (teacherGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            // --- CORRECCIÓN ---
            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
            binding.emptyView.tvEmptyTitle.setText("No hay grupo");
            binding.emptyView.tvEmptySubtitle.setText("No se pudo determinar un grupo para cargar avisos.");
            // --- FIN CORRECCIÓN ---
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getNoticesByGroup(teacherGroupName).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return; // Vista destruida
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
                            // --- CORRECCIÓN ---
                            binding.emptyView.getRoot().setVisibility(View.GONE);
                            // --- FIN CORRECCIÓN ---
                            adapter.submitList(resource.getData());
                        } else {
                            binding.recyclerView.setVisibility(View.GONE);
                            // --- CORRECCIÓN ---
                            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
                            binding.emptyView.tvEmptyTitle.setText("No hay avisos");
                            binding.emptyView.tvEmptySubtitle.setText("Aún no has publicado ningún aviso para este grupo.");
                            // --- FIN CORRECCIÓN ---
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        // --- CORRECCIÓN ---
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                        binding.emptyView.tvEmptyTitle.setText("Error al cargar avisos");
                        binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
                        // --- FIN CORRECCIÓN ---
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