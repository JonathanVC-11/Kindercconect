package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.stream.Collectors;

public class ParentHomeFragment extends Fragment {
    private FragmentParentHomeBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager; // Lo mantenemos para el ID de padre

    private List<Student> studentList;
    private Student currentStudent;

    private static final String TAG = "ParentHomeFragment";

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
        binding.fabAddStudent.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_home_to_register_student);
        });

        // --- INICIO DE CÓDIGO AÑADIDO ---
        binding.btnEditStudent.setOnClickListener(v -> {
            if (currentStudent == null) return;
            Bundle args = new Bundle();
            args.putString("studentId", currentStudent.getStudentId());
            Navigation.findNavController(v).navigate(R.id.action_home_to_edit_student, args);
        });
        // --- FIN DE CÓDIGO AÑADIDO ---

        // (Listeners de tarjetas sin cambios)
        binding.cardGrades.setOnClickListener(v -> {
            if (currentStudent == null) return;
            Bundle args = new Bundle();
            args.putString("studentId", currentStudent.getStudentId());
            args.putString("studentName", currentStudent.getFullName());
            Navigation.findNavController(v).navigate(R.id.gradeViewFragment, args);
        });

        binding.cardNotices.setOnClickListener(v -> {
            if (currentStudent == null) return;
            Bundle args = new Bundle();
            args.putString("groupName", currentStudent.getGroupName());
            args.putString("studentName", currentStudent.getFullName());
            Navigation.findNavController(v).navigate(R.id.noticeListFragment, args);
        });

        binding.cardGallery.setOnClickListener(v -> {
            if (currentStudent == null) return;
            Bundle args = new Bundle();
            args.putString("groupName", currentStudent.getGroupName());
            args.putString("studentName", currentStudent.getFullName());
            Navigation.findNavController(v).navigate(R.id.parentGalleryFragment, args);
        });
    }

    private void loadStudentData() {
        String parentId = preferencesManager.getUserId();
        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return; // Vista destruida
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {
                binding.progressBar.setVisibility(View.GONE);

                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    this.studentList = resource.getData();

                    String lastStudentId = preferencesManager.getCurrentStudentId();
                    this.currentStudent = findStudentById(lastStudentId);

                    if (this.currentStudent == null) {
                        this.currentStudent = this.studentList.get(0);
                    }

                    setupStudentSelector();
                    updateDashboardForStudent();

                    // --- INICIO DE CÓDIGO AÑADIDO ---
                    // Mostrar el botón de editar
                    binding.btnEditStudent.setVisibility(View.VISIBLE);
                    // --- FIN DE CÓDIGO AÑADIDO ---

                } else {
                    // --- INICIO DE CÓDIGO AÑADIDO ---
                    // No hay alumnos, ocultar el botón de editar
                    binding.btnEditStudent.setVisibility(View.GONE);
                    // Opcional: Mostrar un "empty state" para registrar alumnos
                    // --- FIN DE CÓDIGO AÑADIDO ---
                }
            } else if (resource != null && resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.ERROR) {
                if (binding != null) binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    // ... (El resto de la clase: findStudentById, setupStudentSelector, updateDashboardForStudent, etc. sin cambios) ...

    private Student findStudentById(String studentId) {
        if (studentId == null || studentList == null) {
            return null;
        }
        for (Student s : studentList) {
            if (s.getStudentId().equals(studentId)) {
                return s;
            }
        }
        return null;
    }

    private void setupStudentSelector() {
        if (studentList == null || studentList.size() <= 1) {
            binding.tilStudentSelector.setVisibility(View.GONE);
            return;
        }

        List<String> studentNames = studentList.stream()
                .map(Student::getFullName)
                .collect(Collectors.toList());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                studentNames
        );
        binding.actStudentSelector.setAdapter(adapter);

        // Poner el valor actual
        binding.actStudentSelector.setText(currentStudent.getFullName(), false);
        binding.tilStudentSelector.setVisibility(View.VISIBLE);

        binding.actStudentSelector.setOnItemClickListener((parent, view, position, id) -> {
            this.currentStudent = studentList.get(position);
            updateDashboardForStudent();
        });
    }

    private void updateDashboardForStudent() {
        if (currentStudent == null) return;

        preferencesManager.saveCurrentStudent(
                currentStudent.getStudentId(),
                currentStudent.getFullName(),
                currentStudent.getGroupName()
        );

        displayStudentInfo();
        loadAttendanceStats();
        loadNoticesCount();
        subscribeToTopics(currentStudent.getGroupName());
    }

    private void subscribeToTopics(String groupName) {
        // ... (código existente) ...
        FirebaseMessaging.getInstance().subscribeToTopic("notices_SCHOOL")
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Suscrito al tema: notices_SCHOOL"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al suscribir a notices_SCHOOL", e));

        if (groupName != null && !groupName.isEmpty()) {
            String cleanGroupName = groupName.replaceAll("[^a-zA-Z0-9]", "_");
            String groupTopic = "notices_GROUP_" + cleanGroupName;

            FirebaseMessaging.getInstance().subscribeToTopic(groupTopic)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Suscrito al tema: " + groupTopic))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al suscribir a " + groupTopic, e));
        }

        FirebaseMessaging.getInstance().subscribeToTopic("bus_route")
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Suscrito al tema: bus_route"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al suscribir a bus_route", e));
    }

    private void displayStudentInfo() {
        if (currentStudent == null) return;

        binding.tvStudentName.setText(currentStudent.getFullName());
        binding.tvGroupName.setText("Grupo: " + currentStudent.getGroupName());

        if (currentStudent.getPhotoUrl() != null && !currentStudent.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentStudent.getPhotoUrl())
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop()
                    .into(binding.ivStudentPhoto);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_logo)
                    .circleCrop()
                    .into(binding.ivStudentPhoto);
        }
    }

    private void loadAttendanceStats() {
        if (currentStudent == null) return;

        binding.tvAttendancePercentage.setText("0%");

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

        binding.tvUnreadNotices.setText("0");

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