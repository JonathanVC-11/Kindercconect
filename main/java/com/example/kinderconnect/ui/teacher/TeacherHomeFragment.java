package com.example.kinderconnect.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherHomeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;

public class TeacherHomeFragment extends Fragment {
    private FragmentTeacherHomeBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTeacherHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupUI();
        setupListeners();
        loadData();
    }

    private void setupUI() {
        String teacherName = preferencesManager.getUserName();
        binding.tvGreeting.setText(getString(R.string.hello_teacher, teacherName));
    }

    private void setupListeners() {
        binding.cardAttendance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_home_to_attendance));

        binding.cardGrades.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.studentListFragment));

        binding.cardNotices.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.teacherNoticesFragment));

        binding.cardGallery.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.teacherGalleryFragment));
    }

    private void loadData() {
        String teacherId = preferencesManager.getUserId();

        // Cargar estadísticas
        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.SUCCESS) {
                if (resource.getData() != null) {
                    binding.tvTotalStudents.setText(String.valueOf(resource.getData().size()));
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
