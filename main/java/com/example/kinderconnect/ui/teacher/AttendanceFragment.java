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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kinderconnect.databinding.FragmentAttendanceBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Attendance;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.ui.teacher.adapters.AttendanceAdapter;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.DateUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceFragment extends Fragment {
    private FragmentAttendanceBinding binding;
    private TeacherViewModel viewModel;
    private AttendanceAdapter adapter;
    private PreferencesManager preferencesManager;
    private Date selectedDate;
    private Map<String, String> attendanceMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());
        selectedDate = DateUtils.getToday();

        setupUI();
        setupRecyclerView();
        setupListeners();
        loadStudents();
    }

    private void setupUI() {
        binding.tvDate.setText(DateUtils.formatDate(selectedDate));
    }

    private void setupRecyclerView() {
        adapter = new AttendanceAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnStatusChangeListener((studentId, status) -> {
            attendanceMap.put(studentId, status);
            updateStatistics();
        });
    }

    private void setupListeners() {
        binding.btnPrevious.setOnClickListener(v -> {
            // Día anterior
            selectedDate = new Date(selectedDate.getTime() - 24 * 60 * 60 * 1000);
            binding.tvDate.setText(DateUtils.formatDate(selectedDate));
            loadExistingAttendance();
        });

        binding.btnNext.setOnClickListener(v -> {
            // Día siguiente
            selectedDate = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000);
            binding.tvDate.setText(DateUtils.formatDate(selectedDate));
            loadExistingAttendance();
        });

        binding.btnSaveAndNotify.setOnClickListener(v -> saveAttendance());
    }

    private void loadStudents() {
        String teacherId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                binding.progressBar.setVisibility(View.GONE);
                if (resource.getData() != null) {
                    adapter.submitList(resource.getData());
                    loadExistingAttendance();
                }
            }
        });
    }

    private void loadExistingAttendance() {
        String teacherId = preferencesManager.getUserId();

        // Convertir selectedDate a String para ViewModel
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate);

        viewModel.getAttendanceByDate(teacherId, dateStr)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null && resource.getStatus() ==
                            com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                        if (resource.getData() != null) {
                            attendanceMap.clear();
                            for (Attendance attendance : resource.getData()) {
                                attendanceMap.put(attendance.getStudentId(), attendance.getStatus());
                            }
                            adapter.updateAttendance(attendanceMap);
                            updateStatistics();
                        }
                    }
                });
    }

    private void updateStatistics() {
        int present = 0, late = 0, absent = 0;

        for (String status : attendanceMap.values()) {
            switch (status) {
                case Constants.ATTENDANCE_PRESENT:
                    present++;
                    break;
                case Constants.ATTENDANCE_LATE:
                    late++;
                    break;
                case Constants.ATTENDANCE_ABSENT:
                    absent++;
                    break;
            }
        }

        binding.tvPresent.setText(String.valueOf(present));
        binding.tvLate.setText(String.valueOf(late));
        binding.tvAbsent.setText(String.valueOf(absent));
    }

    private void saveAttendance() {
        if (attendanceMap.isEmpty()) {
            Toast.makeText(requireContext(), "No hay asistencias para guardar",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveAndNotify.setEnabled(false);

        String teacherId = preferencesManager.getUserId();
        List<String> savedCount = new ArrayList<>();

        for (Map.Entry<String, String> entry : attendanceMap.entrySet()) {
            Attendance attendance = new Attendance(
                    entry.getKey(),
                    teacherId,
                    new Date(),  // Usamos now para guardar fecha actual
                    entry.getValue()
            );

            viewModel.recordAttendance(attendance).observe(getViewLifecycleOwner(), resource -> {
                if (resource != null && resource.getStatus() ==
                        com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                    savedCount.add(entry.getKey());

                    if (savedCount.size() == attendanceMap.size()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSaveAndNotify.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Asistencia guardada correctamente",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
