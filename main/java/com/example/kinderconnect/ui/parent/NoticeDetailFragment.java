package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ¡Importante!
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.databinding.FragmentNoticeDetailBinding;
import com.example.kinderconnect.utils.DateUtils;
import com.example.kinderconnect.data.model.Notice; // ¡Importante!

public class NoticeDetailFragment extends Fragment {
    private FragmentNoticeDetailBinding binding;
    private String noticeId;
    private ParentViewModel viewModel; // ¡Añadido!

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNoticeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        if (getArguments() != null) {
            noticeId = getArguments().getString("noticeId");
        }

        setupToolbar();

        // ¡Llamar al método para cargar los datos!
        if (noticeId != null) {
            loadNoticeDetails();
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    // --- ¡MÉTODO NUEVO AÑADIDO! ---
    private void loadNoticeDetails() {
        viewModel.getNoticeById(noticeId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.getStatus()) {
                case LOADING:
                    // Opcional: mostrar un ProgressBar
                    binding.tvTitle.setText("Cargando...");
                    binding.tvDescription.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    binding.tvDescription.setVisibility(View.VISIBLE);
                    if (resource.getData() != null) {
                        populateViews(resource.getData());
                    }
                    break;
                case ERROR:
                    // Opcional: mostrar error
                    binding.tvDescription.setVisibility(View.VISIBLE);
                    binding.tvTitle.setText("Error");
                    binding.tvDescription.setText(resource.getMessage());
                    break;
            }
        });
    }

    // --- ¡MÉTODO NUEVO AÑADIDO! ---
    private void populateViews(Notice notice) {
        binding.tvTitle.setText(notice.getTitle());
        binding.tvDescription.setText(notice.getDescription());
        binding.tvTeacherName.setText("Por: " + notice.getTeacherName());

        if (notice.getPublishedAt() != null) {
            binding.tvDate.setText(DateUtils.formatDate(notice.getPublishedAt()));
        }

        // Cargar la imagen si existe
        if (notice.getImageUrl() != null && !notice.getImageUrl().isEmpty()) {
            binding.ivNoticeImage.setVisibility(View.VISIBLE);
            Glide.with(requireContext())
                    .load(notice.getImageUrl())
                    .into(binding.ivNoticeImage);
        } else {
            binding.ivNoticeImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}