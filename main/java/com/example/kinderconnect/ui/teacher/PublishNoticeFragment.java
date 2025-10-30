package com.example.kinderconnect.ui.teacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.databinding.FragmentPublishNoticeBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.Resource; // <-- AÑADIDO
import java.util.Calendar;
import java.util.Date;

public class PublishNoticeFragment extends Fragment {
    private FragmentPublishNoticeBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // --- AÑADIR VARIABLE ---
    private String teacherGroupName = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.tvImageSelected.setVisibility(View.VISIBLE);
                        binding.tvImageSelected.setText("Imagen seleccionada");
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPublishNoticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupToolbar();
        setupSpinners();
        setupListeners();

        // --- MODIFICADO ---
        // Deshabilitar el botón hasta que sepamos el grupo
        binding.btnPublish.setEnabled(false);
        loadTeacherGroup();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupSpinners() {
        // Category spinner
        String[] categories = {"Tarea", "Evento", "Recordatorio", "Urgente"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);

        // Scope spinner
        String[] scopes = {"Mi grupo", "Toda la escuela"};
        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                scopes
        );
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerScope.setAdapter(scopeAdapter);
    }

    private void setupListeners() {
        binding.btnAttachImage.setOnClickListener(v -> selectImage());
        binding.btnPublish.setOnClickListener(v -> publishNotice());
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                // Asumimos que la maestra solo tiene un grupo.
                // Obtenemos el nombre del grupo del primer estudiante.
                teacherGroupName = resource.getData().get(0).getGroupName();
                binding.btnPublish.setEnabled(true); // Habilitar botón
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                // La maestra no tiene alumnos, pero aún puede publicar en "Toda la escuela"
                teacherGroupName = null;
                binding.btnPublish.setEnabled(true); // Habilitar botón
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
                binding.btnPublish.setEnabled(true); // Habilitar de todos modos
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void publishNotice() {
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            binding.tilTitle.setError("Ingresa el título");
            return;
        }
        binding.tilTitle.setError(null);

        if (description.isEmpty()) {
            binding.tilDescription.setError("Ingresa la descripción");
            return;
        }
        binding.tilDescription.setError(null);

        String teacherId = preferencesManager.getUserId();
        String teacherName = preferencesManager.getUserName();

        String category = getCategoryConstant(binding.spinnerCategory.getSelectedItemPosition());
        String scope = binding.spinnerScope.getSelectedItemPosition() == 0 ?
                Constants.SCOPE_GROUP : Constants.SCOPE_SCHOOL;

        Notice notice = new Notice(teacherId, title, description, category, scope);
        notice.setTeacherName(teacherName);


        // --- CORRECCIÓN DE LÓGICA DE GRUPO ---
        if (scope.equals(Constants.SCOPE_GROUP)) {
            // Validar que la maestra tenga un grupo
            if (teacherGroupName == null || teacherGroupName.isEmpty()) {
                Toast.makeText(requireContext(), "No tienes un grupo asignado para publicar", Toast.LENGTH_SHORT).show();
                return;
            }
            // Asignar el grupo real de la maestra
            notice.setGroupName(teacherGroupName); // <-- CORREGIDO
        }
        // Si el scope es "SCHOOL", no establecemos groupName (se queda null).

        // Fecha de vigencia (7 días por defecto)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        notice.setValidUntil(calendar.getTime());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnPublish.setEnabled(false);

        viewModel.publishNotice(notice, selectedImageUri).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Aviso publicado correctamente",
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).navigateUp();
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnPublish.setEnabled(true);
                        Toast.makeText(requireContext(), resource.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private String getCategoryConstant(int position) {
        switch (position) {
            case 0: return Constants.NOTICE_TAREA;
            case 1: return Constants.NOTICE_EVENTO;
            case 2: return Constants.NOTICE_RECORDATORIO;
            case 3: return Constants.NOTICE_URGENTE;
            default: return Constants.NOTICE_RECORDATORIO;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}