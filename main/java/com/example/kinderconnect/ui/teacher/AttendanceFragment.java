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

    // --- NUEVO ---
    // Controlar que el observer de guardado no se llame múltiples veces
    private List<String> savedCount = new ArrayList<>();
    // --- FIN NUEVO ---

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
            if (binding == null) return; // Vista destruida
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                binding.progressBar.setVisibility(View.GONE);
                if (resource.getData() != null) {
                    adapter.submitList(resource.getData());
                    loadExistingAttendance(); // Carga la asistencia para la fecha seleccionada (hoy)
                }
            } else if (resource != null && resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar alumnos: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---
    private void loadExistingAttendance() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        // Convertir selectedDate a String para ViewModel
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate);

        // 1. Limpia el mapa de la UI inmediatamente.
        // Esto soluciona el bug de "selecciones pegadas".
        attendanceMap.clear();
        // 2. Notifica al adaptador que el mapa está vacío AHORA.
        adapter.updateAttendance(attendanceMap);
        // 3. Actualiza las estadísticas (a 0).
        updateStatistics();

        // 4. Ahora, pide los datos del día seleccionado
        viewModel.getAttendanceByDate(teacherId, dateStr)
                .observe(getViewLifecycleOwner(), resource -> {
                    // Validar que el binding no sea nulo (el usuario pudo salir de la pantalla)
                    if (binding == null) return;

                    if (resource != null && resource.getStatus() ==
                            com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                        if (resource.getData() != null) {
                            // 5. Puebla el mapa con los datos (si existen)
                            for (Attendance attendance : resource.getData()) {
                                attendanceMap.put(attendance.getStudentId(), attendance.getStatus());
                            }
                            // 6. Actualiza el adaptador y las estadísticas
                            adapter.updateAttendance(attendanceMap);
                            updateStatistics();
                        }
                        // Si no hay datos, el mapa queda vacío y la UI limpia (correcto)
                    } else if (resource != null && resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.ERROR) {
                        // Si las reglas fallan, mostrar un error
                        Toast.makeText(requireContext(), "Error al cargar asistencia: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

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

        // Validar que el binding no sea nulo
        if(binding != null) {
            binding.tvPresent.setText(String.valueOf(present));
            binding.tvLate.setText(String.valueOf(late));
            binding.tvAbsent.setText(String.valueOf(absent));
        }
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---
    private void saveAttendance() {
        if (attendanceMap.isEmpty()) {
            Toast.makeText(requireContext(), "No hay asistencias para guardar",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveAndNotify.setEnabled(false);

        String teacherId = preferencesManager.getUserId();

        // Limpiamos la lista de conteo ANTES de empezar a guardar
        savedCount.clear();

        for (Map.Entry<String, String> entry : attendanceMap.entrySet()) {
            Attendance attendance = new Attendance(
                    entry.getKey(),
                    teacherId,
                    // new Date(),  // <-- ESTO ERA INCORRECTO
                    selectedDate,  // <-- ESTA ES LA CORRECCIÓN
                    entry.getValue()
            );

            // Usamos 'observe' en lugar de 'observeForever'
            viewModel.recordAttendance(attendance).observe(getViewLifecycleOwner(), resource -> {
                // Validar que el binding no sea nulo
                if (binding == null) return;

                // Solo nos importa la primera respuesta (SUCCESS o ERROR)
                if (resource == null || resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.LOADING) {
                    return; // Ignorar si está cargando
                }

                // Usamos el ID del alumno para contar, ya que es único en el map
                if (!savedCount.contains(entry.getKey())) {
                    if (resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.SUCCESS) {
                        savedCount.add(entry.getKey());
                    } else if (resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.ERROR) {
                        // Opcional: manejar error por cada alumno
                        savedCount.add(entry.getKey()); // Lo contamos igual para que el bucle termine
                        Toast.makeText(requireContext(), "Error guardando: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                // Cuando todos han sido procesados (exitosos o no)
                if (savedCount.size() == attendanceMap.size()) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveAndNotify.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Asistencia guardada correctamente",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}