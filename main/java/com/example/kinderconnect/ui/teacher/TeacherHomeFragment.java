package com.example.kinderconnect.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // ¡Añadir!
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData; // ¡Añadir!
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentTeacherHomeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.utils.Resource; // ¡Añadir!

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
        setupListeners(); // Tus listeners existentes para las tarjetas
        setupBusButtons(); // ¡Nuevo! Añadir listeners para botones del bus
        loadData(); // Tus cargas de datos existentes (total alumnos, etc.)
        observeBusStatus(); // ¡Nuevo! Observar estado inicial del bus
    }

    private void setupUI() {
        String teacherName = preferencesManager.getUserName();
        // Asegurarse que el binding no sea null antes de usarlo
        if (binding != null) {
            binding.tvGreeting.setText(getString(R.string.hello_teacher, teacherName));
        }
    }

    // Combina todos los listeners aquí
    private void setupListeners() {
        if (binding == null) return; // Evitar NPE si la vista se destruye rápido

        // Listeners existentes para las tarjetas
        binding.cardAttendance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_attendance));

        binding.cardGrades.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.studentListFragment)); // Navega a la lista para elegir alumno

        binding.cardNotices.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.teacherNoticesFragment));

        binding.cardGallery.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.teacherGalleryFragment));
    }


    // --- NUEVAS FUNCIONES PARA EL BUS ---
    private void setupBusButtons() {
        if (binding == null) return;
        binding.btnStartBusRoute.setOnClickListener(v -> updateBusStatus("ACTIVE"));
        binding.btnFinishBusRoute.setOnClickListener(v -> updateBusStatus("FINISHED")); // O "INACTIVE"
    }

    // Observa el estado actual del bus para mostrar el botón correcto
    private void observeBusStatus() {
        if (binding == null) return;
        binding.progressBusStatus.setVisibility(View.VISIBLE); // Mostrar carga inicial
        binding.btnStartBusRoute.setVisibility(View.GONE);
        binding.btnFinishBusRoute.setVisibility(View.GONE);

        // Usar getViewLifecycleOwner() para que el observador se limpie automáticamente
        viewModel.getCurrentBusStatus().observe(getViewLifecycleOwner(), resource -> {
            // Comprobar si el binding sigue siendo válido
            if (binding == null || getContext() == null) return;

            binding.progressBusStatus.setVisibility(View.GONE); // Ocultar carga al recibir respuesta
            if (resource != null) {
                switch (resource.getStatus()) {
                    case SUCCESS:
                        String status = resource.getData();
                        if ("ACTIVE".equals(status)) {
                            binding.btnStartBusRoute.setVisibility(View.GONE);
                            binding.btnFinishBusRoute.setVisibility(View.VISIBLE);
                        } else { // Incluye INACTIVE, FINISHED, o si no existe el doc/campo
                            binding.btnStartBusRoute.setVisibility(View.VISIBLE);
                            binding.btnFinishBusRoute.setVisibility(View.GONE);
                        }
                        // Habilitar botones después de cargar estado
                        binding.btnStartBusRoute.setEnabled(true);
                        binding.btnFinishBusRoute.setEnabled(true);
                        break;
                    case ERROR:
                        // En caso de error, mostramos el botón de iniciar por defecto
                        binding.btnStartBusRoute.setVisibility(View.VISIBLE);
                        binding.btnFinishBusRoute.setVisibility(View.GONE);
                        binding.btnStartBusRoute.setEnabled(true); // Permitir intentar de nuevo
                        Toast.makeText(getContext(), "Error al obtener estado del bus: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                        break;
                    case LOADING:
                        // Opcional: Volver a mostrar ProgressBar si el estado tarda en cargar
                        binding.progressBusStatus.setVisibility(View.VISIBLE);
                        binding.btnStartBusRoute.setEnabled(false);
                        binding.btnFinishBusRoute.setEnabled(false);
                        break;
                }
            } else {
                // Manejar caso nulo si es necesario, mostrar botón iniciar por defecto
                binding.btnStartBusRoute.setVisibility(View.VISIBLE);
                binding.btnFinishBusRoute.setVisibility(View.GONE);
                binding.btnStartBusRoute.setEnabled(true);
                Toast.makeText(getContext(), "Respuesta nula al obtener estado del bus", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Envía la actualización de estado a Firestore
    private void updateBusStatus(String newStatus) {
        if (binding == null || getContext() == null) return; // Salir si la vista no está disponible

        // Mostrar progreso y deshabilitar botones mientras se actualiza
        binding.progressBusStatus.setVisibility(View.VISIBLE);
        binding.btnStartBusRoute.setEnabled(false);
        binding.btnFinishBusRoute.setEnabled(false);

        // Determinar qué acción llamar en el ViewModel
        LiveData<Resource<Void>> action = "ACTIVE".equals(newStatus) ?
                viewModel.startBusRoute() : viewModel.finishBusRoute();

        // Observar el resultado de la acción (solo una vez)
        action.observe(getViewLifecycleOwner(), resource -> {
            // Comprobar si el binding sigue siendo válido DENTRO del observer
            if (binding == null || getContext() == null) return;

            // Esta lambda se ejecutará cuando la operación termine (SUCCESS o ERROR)
            if (resource != null && resource.getStatus() != Resource.Status.LOADING) {
                // Quitar el observer para no reaccionar a cambios futuros aquí
                action.removeObservers(getViewLifecycleOwner());

                binding.progressBusStatus.setVisibility(View.GONE); // Ocultar progreso

                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    // Éxito: Mostrar el mensaje y actualizar la UI de botones
                    String message = "ACTIVE".equals(newStatus) ? "Recorrido iniciado" : "Recorrido finalizado";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    // Actualizar botones según el nuevo estado
                    if ("ACTIVE".equals(newStatus)) {
                        binding.btnStartBusRoute.setVisibility(View.GONE);
                        binding.btnFinishBusRoute.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnStartBusRoute.setVisibility(View.VISIBLE);
                        binding.btnFinishBusRoute.setVisibility(View.GONE);
                    }
                    binding.btnFinishBusRoute.setEnabled(true); // Habilitar el botón correspondiente
                    binding.btnStartBusRoute.setEnabled(true); // Habilitar el botón correspondiente

                } else { // ERROR
                    Toast.makeText(getContext(), "Error: " + resource.getMessage(), Toast.LENGTH_LONG).show();
                    // Re-habilitar ambos botones para permitir reintento y refrescar estado
                    binding.btnStartBusRoute.setEnabled(true);
                    binding.btnFinishBusRoute.setEnabled(true);
                    observeBusStatus(); // Volver a consultar el estado real por si falló la escritura
                }
            } else if (resource != null && resource.getStatus() == Resource.Status.LOADING) {
                // Aún cargando, no hacer nada aquí, ya mostramos el ProgressBar
            } else {
                // Caso inesperado (resource es null)
                binding.progressBusStatus.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error desconocido al actualizar estado", Toast.LENGTH_SHORT).show();
                binding.btnStartBusRoute.setEnabled(true);
                binding.btnFinishBusRoute.setEnabled(true);
                observeBusStatus(); // Refrescar UI
            }
        });
    }
    // --- FIN NUEVAS FUNCIONES ---


    // Carga inicial de datos (ej. total de alumnos)
    private void loadData() {
        if (binding == null) return;
        String teacherId = preferencesManager.getUserId();

        // Cargar estadísticas
        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return; // Comprobar binding dentro del observer
            if (resource != null && resource.getStatus() == com.example.kinderconnect.utils.Resource.Status.SUCCESS) {
                if (resource.getData() != null) {
                    binding.tvTotalStudents.setText(String.valueOf(resource.getData().size()));
                } else {
                    binding.tvTotalStudents.setText("0");
                }
            } else if (resource != null && resource.getStatus() == Resource.Status.ERROR){
                binding.tvTotalStudents.setText("-"); // Indicar error
                // Podrías mostrar un Toast aquí también
            }
        });
        // Aquí podrías cargar otras estadísticas si las necesitas
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // ¡Importante! Limpiar la referencia al binding
    }
}