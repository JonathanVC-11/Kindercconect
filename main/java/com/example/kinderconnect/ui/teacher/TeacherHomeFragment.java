package com.example.kinderconnect.ui.teacher;

import android.Manifest;
import android.content.Intent; // ¡Importante!
import android.content.pm.PackageManager; // ¡Importante!
import android.os.Build;     // ¡Importante!
import android.os.Bundle;
import android.util.Log;      // ¡Importante!
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // ¡Importante!
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherHomeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.services.LocationService; // ¡Importante!
import com.example.kinderconnect.utils.Constants; // ¡Importante! Para REQUEST_CODE
import com.example.kinderconnect.utils.PermissionManager; // ¡Importante!
import com.example.kinderconnect.utils.Resource;

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
        setupNavigationListeners(); // Listeners de navegación
        setupBusActionListeners(); // Listeners específicos del bus
        loadDashboardData();       // Cargar datos como total alumnos
        observeBusStatus();        // Observar estado inicial del bus
    }

    private void setupUI() {
        String teacherName = preferencesManager.getUserName();
        if (binding != null) {
            binding.tvGreeting.setText(getString(R.string.hello_teacher, teacherName));
            // Asegurarse de que inicialmente los botones de acción del bus estén ocultos
            // y el progreso visible hasta que se cargue el estado.
            binding.progressBusStatus.setVisibility(View.VISIBLE);
            binding.btnStartBusRoute.setVisibility(View.GONE);
            binding.btnFinishBusRoute.setVisibility(View.GONE);
            binding.btnStartBusRoute.setEnabled(false); // Deshabilitados inicialmente
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

    // --- SECCIÓN DEL BUS ---
    private void setupBusActionListeners() {
        if (binding == null) return;
        binding.btnStartBusRoute.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Iniciar Recorrido' presionado.");
            // Verificar permisos antes de intentar iniciar
            if (!PermissionManager.hasLocationPermission(requireContext())) {
                Log.w(TAG, "Permiso de ubicación no concedido. Solicitando...");
                // Usar requestPermissions directamente desde Fragment
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        Constants.REQUEST_LOCATION_PERMISSION // Usar constante definida
                );
                Toast.makeText(getContext(), "Se necesitan permisos de ubicación para iniciar la ruta", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Permiso de ubicación ya concedido. Procediendo a iniciar ruta...");
                updateBusStatus("ACTIVE"); // Solo inicia si hay permisos
            }
        });
        binding.btnFinishBusRoute.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Finalizar Recorrido' presionado.");
            updateBusStatus("FINISHED"); // O "STOPPED" si prefieres
        });
    }

    private void observeBusStatus() {
        if (binding == null) return;
        Log.d(TAG, "Observando estado inicial del bus...");
        // Mostrar progreso y ocultar botones mientras se carga
        binding.progressBusStatus.setVisibility(View.VISIBLE);
        binding.btnStartBusRoute.setVisibility(View.GONE);
        binding.btnFinishBusRoute.setVisibility(View.GONE);
        binding.btnStartBusRoute.setEnabled(false);
        binding.btnFinishBusRoute.setEnabled(false);


        viewModel.getCurrentBusStatus().observe(getViewLifecycleOwner(), resource -> {
            // Comprobar binding y contexto OTRA VEZ dentro del observer
            if (binding == null || getContext() == null) {
                Log.w(TAG, "Observer de estado del bus: Binding o Context nulo, saliendo.");
                return;
            }

            binding.progressBusStatus.setVisibility(View.GONE); // Ocultar progreso al recibir respuesta

            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        String status = resource.getData();
                        Log.i(TAG, "Estado actual del bus obtenido desde Firestore: " + status);
                        updateBusButtonsUI(status); // Actualizar UI de botones basado en estado
                        // Sincronizar estado del servicio de ubicación con el estado de Firestore
                        if ("ACTIVE".equals(status)) {
                            // Si el estado es ACTIVO pero el servicio no corre (ej. tras reiniciar app), iniciarlo
                            // Podríamos necesitar una forma de verificar si el servicio ya está corriendo
                            // Por ahora, lo iniciamos si el estado es ACTIVE y tenemos permisos
                            if (PermissionManager.hasLocationPermission(requireContext())) {
                                Log.d(TAG, "Estado es ACTIVE y hay permisos, asegurando que el servicio esté iniciado.");
                                startLocationService();
                            } else {
                                Log.w(TAG, "Estado es ACTIVE pero faltan permisos de ubicación para iniciar el servicio automáticamente.");
                            }
                        } else {
                            Log.d(TAG, "Estado no es ACTIVE, asegurando que el servicio esté detenido.");
                            stopLocationService(); // Asegurarse de detener el servicio si no está activo
                        }
                        break;
                    case ERROR:
                        Log.e(TAG, "Error al obtener estado inicial del bus: " + resource.getMessage());
                        Toast.makeText(getContext(), "Error al cargar estado: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                        updateBusButtonsUI("STOPPED"); // Mostrar botón de iniciar por defecto en caso de error
                        stopLocationService(); // Detener servicio si hay error
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
                updateBusButtonsUI("STOPPED"); // Mostrar botón iniciar por defecto
                stopLocationService(); // Detener servicio
            }
        });
    }

    // Método auxiliar para actualizar la visibilidad y habilitación de los botones del bus
    private void updateBusButtonsUI(String status) {
        if (binding == null) return;
        if ("ACTIVE".equals(status)) {
            binding.btnStartBusRoute.setVisibility(View.GONE);
            binding.btnFinishBusRoute.setVisibility(View.VISIBLE);
        } else { // Incluye STOPPED, FINISHED, null, etc.
            binding.btnStartBusRoute.setVisibility(View.VISIBLE);
            binding.btnFinishBusRoute.setVisibility(View.GONE);
        }
        // Habilitar ambos botones después de determinar la visibilidad
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

        // Usar observe en lugar de observeForever y quitar manualmente el observer
        action.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Resource<Void>>() {
            @Override
            public void onChanged(Resource<Void> resource) {
                if (binding == null || getContext() == null) {
                    action.removeObserver(this); // Remover si la vista se destruyó
                    return;
                }

                // Solo actuar cuando ya no esté LOADING
                if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                    action.removeObserver(this); // ¡Importante! Remover observer después de la primera respuesta no LOADING

                    binding.progressBusStatus.setVisibility(View.GONE);

                    if (resource.getStatus() == Resource.Status.SUCCESS) {
                        Log.i(TAG, "Estado del bus actualizado exitosamente a " + newStatus + " en Firestore.");
                        String message = "ACTIVE".equals(newStatus) ? "Recorrido iniciado" : "Recorrido finalizado";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                        // Actualizar UI de botones y controlar servicio
                        updateBusButtonsUI(newStatus);
                        if ("ACTIVE".equals(newStatus)) {
                            startLocationService();
                        } else { // FINISHED o STOPPED
                            stopLocationService();
                        }

                    } else { // ERROR
                        Log.e(TAG, "Error al actualizar estado del bus en Firestore: " + resource.getMessage());
                        Toast.makeText(getContext(), "Error al actualizar: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                        // Re-habilitar botones y re-observar estado actual por si acaso
                        binding.btnStartBusRoute.setEnabled(true);
                        binding.btnFinishBusRoute.setEnabled(true);
                        observeBusStatus(); // Re-sincronizar UI con el estado real
                    }
                } else if (resource != null && resource.getStatus() == Resource.Status.LOADING) {
                    Log.d(TAG, "Actualizando estado del bus... (LOADING)");
                    // El ProgressBar ya está visible
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
        // Doble check de permisos por si acaso
        if (!PermissionManager.hasLocationPermission(requireContext())) {
            Log.e(TAG, "Intento de iniciar servicio SIN permisos de ubicación.");
            Toast.makeText(getContext(), "Se necesitan permisos de ubicación.", Toast.LENGTH_SHORT).show();
            // Podrías solicitar permisos aquí de nuevo si es necesario
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


    private void loadDashboardData() {
        if (binding == null) return;
        String teacherId = preferencesManager.getUserId();
        Log.d(TAG, "Cargando datos del dashboard...");

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource != null && resource.getStatus() == Resource.Status.SUCCESS) {
                int studentCount = (resource.getData() != null) ? resource.getData().size() : 0;
                binding.tvTotalStudents.setText(String.valueOf(studentCount));
                Log.d(TAG, "Total de alumnos cargado: " + studentCount);
            } else if (resource != null && resource.getStatus() == Resource.Status.ERROR){
                binding.tvTotalStudents.setText("-");
                Log.e(TAG, "Error al cargar lista de estudiantes: " + resource.getMessage());
                // Podrías mostrar un Toast aquí
            }
            // Cargar otras estadísticas aquí si es necesario (ej. asistencia hoy)
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Limpiando binding.");
        // Considerar si detener el servicio aquí es apropiado,
        // podría ser mejor dejarlo corriendo si la ruta está activa
        // stopLocationService(); // Descomentar si quieres detenerlo siempre al salir del fragmento
        binding = null;
    }

    // --- MANEJAR RESULTADO DE PERMISOS ---
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);
        if (requestCode == Constants.REQUEST_LOCATION_PERMISSION) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break; // Basta con que uno (fine o coarse) esté concedido
                }
            }

            if (granted) {
                Log.i(TAG, "Permiso de ubicación CONCEDIDO por el usuario.");
                Toast.makeText(getContext(), "Permiso concedido. Ahora puedes iniciar la ruta.", Toast.LENGTH_SHORT).show();
                // Ahora que tenemos permiso, si el estado del bus es ACTIVE, iniciar servicio
                // Opcional: Podrías llamar a updateBusStatus("ACTIVE") aquí si quieres iniciarla inmediatamente
                // updateBusStatus("ACTIVE");
            } else {
                Log.w(TAG, "Permiso de ubicación DENEGADO por el usuario.");
                Toast.makeText(getContext(), "Permiso denegado. No se puede iniciar el seguimiento de ubicación.", Toast.LENGTH_LONG).show();
                // Podrías mostrar un diálogo explicando por qué necesitas el permiso
            }
        }
    }
}