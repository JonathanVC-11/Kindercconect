package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentParentHomeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.DateUtils;

public class ParentHomeFragment extends Fragment {
    private FragmentParentHomeBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager;
    private Student currentStudent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentParentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupListeners();
        loadStudentData();
    }

    private void setupListeners() {
        binding.cardGrades.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.gradeViewFragment));

        binding.cardNotices.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.noticeListFragment));

        binding.cardGallery.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.parentGalleryFragment));
    }

    private void loadStudentData() {
        String parentId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                binding.progressBar.setVisibility(View.GONE);

                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    currentStudent = resource.getData().get(0);
                    displayStudentInfo();
                    loadAttendanceStats();
                    loadNoticesCount();
                }
            }
        });
    }

    private void displayStudentInfo() {
        binding.tvStudentName.setText(currentStudent.getFullName());
        binding.tvGroupName.setText("Grupo: " + currentStudent.getGroupName());

        if (currentStudent.getPhotoUrl() != null && !currentStudent.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentStudent.getPhotoUrl())
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop()
                    .into(binding.ivStudentPhoto);
        }
    }

    private void loadAttendanceStats() {
        if (currentStudent == null) return;

        viewModel.getAttendanceByStudent(
                currentStudent.getStudentId(),
                DateUtils.getStartOfMonth(),
                DateUtils.getEndOfMonth()
        ).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                if (resource.getData() != null) {
                    int total = resource.getData().size();
                    int present = 0;

                    for (var attendance : resource.getData()) {
                        if ("PRESENT".equals(attendance.getStatus())) {
                            present++;
                        }
                    }

                    int percentage = total > 0 ? (present * 100) / total : 0;
                    binding.tvAttendancePercentage.setText(percentage + "%");
                }
            }
        });
    }

    private void loadNoticesCount() {
        if (currentStudent == null) return;

        viewModel.getNoticesByGroup(currentStudent.getGroupName())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null && resource.getStatus() ==
                            com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                        if (resource.getData() != null) {
                            int unreadCount = 0;
                            String userId = preferencesManager.getUserId();

                            for (var notice : resource.getData()) {
                                if (!notice.isReadByUser(userId)) {
                                    unreadCount++;
                                }
                            }

                            binding.tvUnreadNotices.setText(String.valueOf(unreadCount));
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
