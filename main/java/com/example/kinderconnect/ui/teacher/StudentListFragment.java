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
import com.example.kinderconnect.databinding.FragmentStudentListBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.ui.teacher.adapters.StudentAdapter;

public class StudentListFragment extends Fragment {
    private FragmentStudentListBinding binding;
    private TeacherViewModel viewModel;
    private StudentAdapter adapter;
    private PreferencesManager preferencesManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStudentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        setupListeners();
        loadStudents();
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(student -> {
            // Navegar a detalles del estudiante o registro de calificaciones
            Bundle args = new Bundle();
            args.putString("studentId", student.getStudentId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_students_to_grade_registration, args);
        });
    }

    private void setupListeners() {
        binding.fabAddStudent.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_students_to_add_student));
    }

    private void loadStudents() {
        String teacherId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.GONE);
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
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
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
