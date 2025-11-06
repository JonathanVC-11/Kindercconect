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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentNoticeListBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.parent.adapters.NoticeAdapter;

public class NoticeListFragment extends Fragment {
    private FragmentNoticeListBinding binding;
    private ParentViewModel viewModel;
    private NoticeAdapter adapter;
    private PreferencesManager preferencesManager;
    private String studentGroupName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNoticeListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        loadStudentGroup();
    }

    private void setupRecyclerView() {
        adapter = new NoticeAdapter(preferencesManager.getUserId());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(notice -> {
            viewModel.markNoticeAsRead(notice.getNoticeId(), preferencesManager.getUserId());
            Bundle args = new Bundle();
            args.putString("noticeId", notice.getNoticeId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_notices_to_detail, args);
        });
    }

    // --- LÓGICA DE ESTADO VACÍO MODIFICADA ---
    private void loadStudentGroup() {
        String parentId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE);

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    studentGroupName = resource.getData().get(0).getGroupName();
                    loadNotices();
                } else {
                    // No tiene alumnos
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                    binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
                    binding.emptyView.tvEmptyTitle.setText("Sin alumnos");
                    binding.emptyView.tvEmptySubtitle.setText("No hay alumnos asignados para ver avisos.");
                }
            } else if (resource != null) {
                // Error cargando alumno
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                binding.emptyView.tvEmptyTitle.setText("Error");
                binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
            }
        });
    }

    private void loadNotices() {
        viewModel.getNoticesByGroup(studentGroupName).observe(getViewLifecycleOwner(), resource -> {
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
                            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_notifications);
                            binding.emptyView.tvEmptyTitle.setText("No hay avisos");
                            binding.emptyView.tvEmptySubtitle.setText("Aún no hay avisos publicados para este grupo.");
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
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}