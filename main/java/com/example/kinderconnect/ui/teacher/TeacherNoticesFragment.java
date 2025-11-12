package com.example.kinderconnect.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherNoticesBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Notice;
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

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Clic normal para EDITAR
        adapter.setOnItemClickListener(notice -> {
            // Solo permitir editar si la maestra actual es la autora
            if (notice.getTeacherId() != null && notice.getTeacherId().equals(preferencesManager.getUserId())) {
                Bundle args = new Bundle();
                args.putString("noticeId", notice.getNoticeId());
                Navigation.findNavController(binding.getRoot())
                        .navigate(R.id.action_notices_to_publish, args);
            } else {
                Toast.makeText(requireContext(), "No puedes editar este aviso", Toast.LENGTH_SHORT).show();
            }
        });

        // Clic largo para ELIMINAR
        adapter.setOnItemLongClickListener(notice -> {
            // Solo permitir borrar si la maestra actual es la autora
            if (notice.getTeacherId() != null && notice.getTeacherId().equals(preferencesManager.getUserId())) {
                showDeleteNoticeDialog(notice);
            } else {
                Toast.makeText(requireContext(), "No puedes eliminar este aviso", Toast.LENGTH_SHORT).show();
            }
        });
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    private void setupListeners() {
        binding.fabPublish.setOnClickListener(v ->
                // Navegar sin argumentos (noticeId será null)
                Navigation.findNavController(v).navigate(
                        R.id.action_notices_to_publish));
    }

    // ... (loadTeacherGroup y loadNotices sin cambios) ...
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
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
                binding.emptyView.tvEmptyTitle.setText("No tienes alumnos");
                binding.emptyView.tvEmptySubtitle.setText("No puedes publicar avisos de grupo sin alumnos asignados.");
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotices() {
        if (teacherGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
            binding.emptyView.tvEmptyTitle.setText("No hay grupo");
            binding.emptyView.tvEmptySubtitle.setText("No se pudo determinar un grupo para cargar avisos.");
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
                            binding.emptyView.getRoot().setVisibility(View.GONE);
                            adapter.submitList(resource.getData());
                        } else {
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
                            binding.emptyView.tvEmptyTitle.setText("No hay avisos");
                            binding.emptyView.tvEmptySubtitle.setText("Aún no has publicado ningún aviso para este grupo.");
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                        binding.emptyView.tvEmptyTitle.setText("Error al cargar avisos");
                        binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
                        Toast.makeText(requireContext(), resource.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }


    // ... (showDeleteNoticeDialog y deleteNotice sin cambios) ...
    private void showDeleteNoticeDialog(Notice notice) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Aviso")
                .setMessage("¿Estás seguro que deseas eliminar este aviso? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteNotice(notice))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteNotice(Notice notice) {
        if (notice == null || notice.getNoticeId() == null) return;

        binding.progressBar.setVisibility(View.VISIBLE); // Mostrar progreso

        viewModel.deleteNotice(notice.getNoticeId()).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            // Solo actuar cuando deje de estar "LOADING"
            if (resource.getStatus() != Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.GONE);
            }

            switch (resource.getStatus()) {
                case LOADING:
                    break;
                case SUCCESS:
                    Toast.makeText(requireContext(), "Aviso eliminado", Toast.LENGTH_SHORT).show();
                    // La lista se actualizará sola gracias al SnapshotListener en loadNotices()
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), "Error: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}