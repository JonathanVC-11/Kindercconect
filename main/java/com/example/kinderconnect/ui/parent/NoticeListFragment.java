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
    private String studentName;

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

        // --- INICIO DE CÓDIGO MODIFICADO ---
        if (getArguments() != null && getArguments().getString("groupName") != null) {
            // Opción 1: Venimos desde los accesos rápidos (Home)
            studentGroupName = getArguments().getString("groupName");
            studentName = getArguments().getString("studentName");
        } else {
            // Opción 2: Venimos desde la barra de navegación inferior
            studentGroupName = preferencesManager.getCurrentGroupName();
            studentName = preferencesManager.getCurrentStudentName();
        }

        if (studentGroupName == null || studentName == null) {
            Toast.makeText(requireContext(), "Error: No se pudo cargar el grupo", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.toolbar.setTitle("Avisos de " + studentName);
        // --- FIN DE CÓDIGO MODIFICADO ---

        setupRecyclerView();
        loadNotices(); // Llamar a cargar avisos directamente
    }

    private void setupRecyclerView() {
        // ... (sin cambios) ...
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

    private void loadNotices() {
        // ... (sin cambios) ...
        if (studentGroupName == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
            binding.emptyView.tvEmptyTitle.setText("Error");
            binding.emptyView.tvEmptySubtitle.setText("No se especificó un grupo.");
            return;
        }

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