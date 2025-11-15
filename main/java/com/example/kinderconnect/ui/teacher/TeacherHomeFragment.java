package com.example.kinderconnect.ui.teacher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherHomeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.services.LocationService;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.PermissionManager;
import com.example.kinderconnect.utils.Resource;

// --- INICIO DE IMPORTACIONES AÑADIDAS ---
import com.example.kinderconnect.utils.DateUtils;
import java.text.SimpleDateFormat;
import java.util.Locale;
// --- FIN DE IMPORTACIONES AÑADIDAS ---

public class TeacherHomeFragment extends Fragment {
    private FragmentTeacherHomeBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private static final String TAG = "TeacherHomeFragment"; // Tag para Logs

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
        setupNavigationListeners();
        setupBusActionListeners();
        loadDashboardData();
        observeBusStatus();
    }

    private void setupUI() {
        String teacherName = preferencesManager.getUserName();
        if (binding != null) {
            binding.tvGreeting.setText(getString(R.string.hello_teacher, teacherName));
            binding.progressBusStatus.setVisibility(View.VISIBLE);
            binding.btnStartBusRoute.setVisibility(View.GONE);
            binding.btnFinishBusRoute.setVisibility(View.GONE);
            binding.btnStartBusRoute.setEnabled(false);
            binding.btnFinishBusRoute.setEnabled(false);
        }
    }

    private void setupNavigationListeners() {
        if (binding == null) return;

        binding.cardAttendance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_attendance));

        binding.cardGrades.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.studentListFragment));

        binding.cardNotices.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.teacherNoticesFragment));

        binding.cardGallery.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.teacherGalleryFragment));
    }

    // --- SECCIÓN DEL BUS (Sin cambios) ---
    private void setupBusActionListeners() {
        if (binding == null) return;
        binding.btnStartBusRoute.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Iniciar Recorrido' presionado.");
            if (!PermissionManager.hasLocationPermission(requireContext())) {
                Log.w(TAG, "Permiso de ubicación no concedido. Solicitando...");
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        Constants.REQUEST_LOCATION_PERMISSION
                );
                Toast.makeText(getContext(), "Se necesitan permisos de ubicación para iniciar la ruta", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Permiso de ubicación ya concedido. Procediendo a iniciar ruta...");
                updateBusStatus("ACTIVE");
            }
        });
        binding.btnFinishBusRoute.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Finalizar Recorrido' presionado.");
            updateBusStatus("FINISHED");
        });
    }

    private void observeBusStatus() {
        if (binding == null) return;
        Log.d(TAG, "Observando estado inicial del bus...");
        binding.progressBusStatus.setVisibility(View.VISIBLE);
        binding.btnStartBusRoute.setVisibility(View.GONE);
        binding.btnFinishBusRoute.setVisibility(View.GONE);
        binding.btnStartBusRoute.setEnabled(false);
        binding.btnFinishBusRoute.setEnabled(false);


        viewModel.getCurrentBusStatus().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || getContext() == null) {
                Log.w(TAG, "Observer de estado del bus: Binding o Context nulo, saliendo.");
                return;
            }

            binding.progressBusStatus.setVisibility(View.GONE);

            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        String status = resource.getData();
                        Log.i(TAG, "Estado actual del bus obtenido desde Firestore: " + status);
                        updateBusButtonsUI(status);
                        if ("ACTIVE".equals(status)) {
                            if (PermissionManager.hasLocationPermission(requireContext())) {
                                Log.d(TAG, "Estado es ACTIVE y hay permisos, asegurando que el servicio esté iniciado.");
                                startLocationService();
                            } else {
                                Log.w(TAG, "Estado es ACTIVE pero faltan permisos de ubicación para iniciar el servicio automáticamente.");
                            }
                        } else {
                            Log.d(TAG, "Estado no es ACTIVE, asegurando que el servicio esté detenido.");
                            stopLocationService();
                        }
                        break;
                    case ERROR:
                        Log.e(TAG, "Error al obtener estado inicial del bus: " + resource.getMessage());
                        Toast.makeText(getContext(), "Error al cargar estado: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                        updateBusButtonsUI("STOPPED");
                        stopLocationService();
                        break;
                    case LOADING:
                        Log.d(TAG, "Cargando estado inicial del bus...");
                        binding.progressBusStatus.setVisibility(View.VISIBLE);
                        binding.btnStartBusRoute.setEnabled(false);
                        binding.btnFinishBusRoute.setEnabled(false);
                        break;
                }
            } else {
                Log.e(TAG, "Respuesta nula al obtener estado inicial del bus.");
                Toast.makeText(getContext(), "No se pudo obtener el estado del bus", Toast.LENGTH_SHORT).show();
                updateBusButtonsUI("STOPPED");
                stopLocationService();
            }
        });
    }

    private void updateBusButtonsUI(String status) {
        if (binding == null) return;
        if ("ACTIVE".equals(status)) {
            binding.btnStartBusRoute.setVisibility(View.GONE);
            binding.btnFinishBusRoute.setVisibility(View.VISIBLE);
        } else {
            binding.btnStartBusRoute.setVisibility(View.VISIBLE);
            binding.btnFinishBusRoute.setVisibility(View.GONE);
        }
        binding.btnStartBusRoute.setEnabled(true);
        binding.btnFinishBusRoute.setEnabled(true);
        Log.d(TAG, "UI de botones actualizada para estado: " + status);
    }


    private void updateBusStatus(String newStatus) {
        if (binding == null || getContext() == null) return;

        Log.d(TAG, "Intentando actualizar estado del bus a: " + newStatus + " en Firestore...");
        binding.progressBusStatus.setVisibility(View.VISIBLE);
        binding.btnStartBusRoute.setEnabled(false);
        binding.btnFinishBusRoute.setEnabled(false);

        LiveData<Resource<Void>> action = "ACTIVE".equals(newStatus) ?
                viewModel.startBusRoute() : viewModel.finishBusRoute();

        action.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Resource<Void>>() {
            @Override
            public void onChanged(Resource<Void> resource) {
                if (binding == null || getContext() == null) {
                    action.removeObserver(this);
                    return;
                }

                if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                    action.removeObserver(this);

                    binding.progressBusStatus.setVisibility(View.GONE);

                    if (resource.getStatus() == Resource.Status.SUCCESS) {
                        Log.i(TAG, "Estado del bus actualizado exitosamente a " + newStatus + " en Firestore.");
                        String message = "ACTIVE".equals(newStatus) ? "Recorrido iniciado" : "Recorrido finalizado";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                        updateBusButtonsUI(newStatus);
                        if ("ACTIVE".equals(newStatus)) {
                            startLocationService();
                        } else {
                            stopLocationService();
                        }

                    } else { // ERROR
                        Log.e(TAG, "Error al actualizar estado del bus en Firestore: " + resource.getMessage());
                        Toast.makeText(getContext(), "Error al actualizar: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                        binding.btnStartBusRoute.setEnabled(true);
                        binding.btnFinishBusRoute.setEnabled(true);
                        observeBusStatus();
                    }
                } else if (resource != null && resource.getStatus() == Resource.Status.LOADING) {
                    Log.d(TAG, "Actualizando estado del bus... (LOADING)");
                } else { // resource == null
                    Log.e(TAG, "Error desconocido (recurso nulo) al actualizar estado del bus.");
                    binding.progressBusStatus.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error desconocido al actualizar", Toast.LENGTH_SHORT).show();
                    binding.btnStartBusRoute.setEnabled(true);
                    binding.btnFinishBusRoute.setEnabled(true);
                    observeBusStatus();
                }
            }
        });
    }

    private void startLocationService() {
        if (getContext() == null) {
            Log.e(TAG, "Intento de iniciar servicio con contexto nulo.");
            return;
        }
        if (!PermissionManager.hasLocationPermission(requireContext())) {
            Log.e(TAG, "Intento de iniciar servicio SIN permisos de ubicación.");
            Toast.makeText(getContext(), "Se necesitan permisos de ubicación.", Toast.LENGTH_SHORT).show();
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        Intent serviceIntent = new Intent(getContext(), LocationService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(getContext(), serviceIntent);
                Log.i(TAG, "Iniciando LocationService (Foreground)...");
            } else {
                getContext().startService(serviceIntent);
                Log.i(TAG, "Iniciando LocationService (Background)...");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al intentar iniciar LocationService", e);
            Toast.makeText(getContext(), "No se pudo iniciar el seguimiento de ubicación.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (getContext() == null) {
            Log.e(TAG, "Intento de detener servicio con contexto nulo.");
            return;
        }
        Intent serviceIntent = new Intent(getContext(), LocationService.class);
        boolean stopped = getContext().stopService(serviceIntent);
        Log.i(TAG, "Intentando detener LocationService... ¿Exitoso? " + stopped);
    }
    // --- FIN SECCIÓN DEL BUS ---


    // --- INICIO DE CÓDIGO MODIFICADO ---
    private void loadDashboardData() {
        if (binding == null) return;
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) return;

        Log.d(TAG, "Cargando datos del dashboard...");

        // 1. Cargar total de alumnos
        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource != null && resource.getStatus() == Resource.Status.SUCCESS) {
                int studentCount = (resource.getData() != null) ? resource.getData().size() : 0;
                binding.tvTotalStudents.setText(String.valueOf(studentCount));
                Log.d(TAG, "Total de alumnos cargado: " + studentCount);
            } else if (resource != null && resource.getStatus() == Resource.Status.ERROR){
                binding.tvTotalStudents.setText("-");
                Log.e(TAG, "Error al cargar lista de estudiantes: " + resource.getMessage());
            }
        });

        // 2. Cargar asistencia de HOY
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(DateUtils.getToday());

        viewModel.getAttendanceByDate(teacherId, todayStr).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource != null && resource.getStatus() == Resource.Status.SUCCESS) {
                // Contamos solo los que asistieron (Presente o Tarde)
                int attendanceCount = 0;
                if (resource.getData() != null) {
                    for (com.example.kinderconnect.data.model.Attendance attendance : resource.getData()) {
                        if (Constants.ATTENDANCE_PRESENT.equals(attendance.getStatus()) ||
                                Constants.ATTENDANCE_LATE.equals(attendance.getStatus())) {
                            attendanceCount++;
                        }
                    }
                }
                binding.tvTodayAttendance.setText(String.valueOf(attendanceCount));
                Log.d(TAG, "Asistencia de hoy cargada: " + attendanceCount);
            } else if (resource != null && resource.getStatus() == Resource.Status.ERROR) {
                binding.tvTodayAttendance.setText("-");
                Log.e(TAG, "Error al cargar asistencia del día: " + resource.getMessage());
            }
        });
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Limpiando binding.");
        binding = null;
    }

    // --- MANEJAR RESULTADO DE PERMISOS (Sin cambios) ---
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);
        if (requestCode == Constants.REQUEST_LOCATION_PERMISSION) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                Log.i(TAG, "Permiso de ubicación CONCEDIDO por el usuario.");
                Toast.makeText(getContext(), "Permiso concedido. Ahora puedes iniciar la ruta.", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Permiso de ubicación DENEGADO por el usuario.");
                Toast.makeText(getContext(), "Permiso denegado. No se puede iniciar el seguimiento de ubicación.", Toast.LENGTH_LONG).show();
            }
        }
    }
}