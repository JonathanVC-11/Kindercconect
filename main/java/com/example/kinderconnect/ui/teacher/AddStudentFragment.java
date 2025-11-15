package com.example.kinderconnect.ui.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R; // <-- AÑADIDO
import com.example.kinderconnect.databinding.FragmentAddStudentBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.DateUtils;
import com.example.kinderconnect.utils.PermissionManager;
import com.example.kinderconnect.utils.Resource; // <-- AÑADIDO
import java.text.SimpleDateFormat; // <-- AÑADIDO
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddStudentFragment extends Fragment {
    private FragmentAddStudentBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private Uri selectedImageUri;
    private Date selectedBirthDate;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private boolean isEditMode = false;
    private String studentIdToEdit = null;
    private Student studentToEdit = null;
    // --- FIN DE CÓDIGO AÑADIDO ---


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- INICIO DE CÓDIGO AÑADIDO ---
        if (getArguments() != null) {
            studentIdToEdit = getArguments().getString("studentId");
            isEditMode = (studentIdToEdit != null);
        }
        // --- FIN DE CÓDIGO AÑADIDO ---

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        displaySelectedImage();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupToolbar();
        setupListeners();

        // --- INICIO DE CÓDIGO AÑADIDO ---
        if (isEditMode) {
            setupEditMode();
        }
        // --- FIN DE CÓDIGO AÑADIDO ---
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private void setupEditMode() {
        binding.toolbar.setTitle("Editar Alumno");
        binding.btnSave.setText("Actualizar Alumno");
        // Deshabilitar la edición del correo del padre
        binding.etParentEmail.setEnabled(false);
        binding.tilParentEmail.setHint("Correo del padre (No editable)");

        loadStudentData();
    }

    private void loadStudentData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getStudentById(studentIdToEdit).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                studentToEdit = resource.getData();
                populateUi(studentToEdit);
                // Ahora, buscamos el email del padre
                loadParentEmail(studentToEdit.getParentId());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error al cargar alumno: " + resource.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadParentEmail(String parentId) {
        if (parentId == null) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        viewModel.getParentData(parentId).observe(getViewLifecycleOwner(), resource -> {
            binding.progressBar.setVisibility(View.GONE);
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                binding.etParentEmail.setText(resource.getData().getEmail());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.etParentEmail.setText("Error al cargar email");
            }
        });
    }

    private void populateUi(Student student) {
        binding.etFullName.setText(student.getFullName());
        binding.etGroupName.setText(student.getGroupName());
        binding.etEmergencyContact.setText(student.getEmergencyContact());
        binding.etAllergies.setText(student.getAllergies());
        binding.etMedicalNotes.setText(student.getMedicalNotes());

        if (student.getBirthDate() != null) {
            selectedBirthDate = student.getBirthDate();
            binding.etBirthDate.setText(DateUtils.formatDate(selectedBirthDate));
        }

        if (student.getPhotoUrl() != null && !student.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(student.getPhotoUrl())
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop()
                    .into(binding.ivStudentPhoto);
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    private void setupListeners() {
        binding.ivStudentPhoto.setOnClickListener(v -> selectImage());
        binding.etBirthDate.setOnClickListener(v -> showDatePicker());

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // El botón ahora decide si crear o actualizar
        binding.btnSave.setOnClickListener(v -> validateAndSaveStudent());
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    private void selectImage() {
        if (!PermissionManager.hasStoragePermission(requireContext())) {
            PermissionManager.requestStoragePermission(requireActivity());
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(binding.ivStudentPhoto);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Si estamos editando, usar la fecha del alumno como default
        if (isEditMode && selectedBirthDate != null) {
            calendar.setTime(selectedBirthDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedBirthDate = calendar.getTime();
                    // Usar un formato consistente
                    binding.etBirthDate.setText(DateUtils.formatDate(selectedBirthDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // --- MÉTODO 'saveStudent' RENOMBRADO Y MODIFICADO ---
    private void validateAndSaveStudent() {
        String fullName = binding.etFullName.getText().toString().trim();
        String groupName = binding.etGroupName.getText().toString().trim();
        String parentEmail = binding.etParentEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String emergencyContact = binding.etEmergencyContact.getText().toString().trim();
        String allergies = binding.etAllergies.getText().toString().trim();
        String medicalNotes = binding.etMedicalNotes.getText().toString().trim();

        // (Validaciones - sin cambios)
        if (fullName.isEmpty()) {
            binding.tilFullName.setError("Ingresa el nombre del alumno");
            return;
        }
        binding.tilFullName.setError(null);

        if (groupName.isEmpty()) {
            binding.tilGroupName.setError("Ingresa el grupo");
            return;
        }
        binding.tilGroupName.setError(null);

        if (parentEmail.isEmpty()) {
            binding.tilParentEmail.setError("Ingresa el correo del padre/madre");
            return;
        }
        binding.tilParentEmail.setError(null);

        if (selectedBirthDate == null) {
            Toast.makeText(requireContext(), "Selecciona la fecha de nacimiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        // --- INICIO DE CÓDIGO MODIFICADO (Lógica de Guardar/Actualizar) ---
        if (isEditMode) {
            // MODO ACTUALIZAR
            // Poblamos el objeto que ya cargamos con los nuevos datos
            studentToEdit.setFullName(fullName);
            studentToEdit.setGroupName(groupName);
            studentToEdit.setBirthDate(selectedBirthDate);
            studentToEdit.setEmergencyContact(emergencyContact);
            studentToEdit.setAllergies(allergies);
            studentToEdit.setMedicalNotes(medicalNotes);
            // El 'teacherId' y 'parentId' (que está en studentToEdit) no cambian

            // selectedImageUri solo será != null si el usuario eligió una FOTO NUEVA
            // Ya no pasamos el 'parentEmail'
            viewModel.updateStudent(studentToEdit, selectedImageUri)
                    .observe(getViewLifecycleOwner(), resource -> {
                        handleSaveResponse(resource, "Alumno actualizado correctamente");
                    });

        } else {
            // MODO CREAR (lógica original)
            Student student = new Student(fullName, null, teacherId, groupName);
            student.setBirthDate(selectedBirthDate);
            student.setEmergencyContact(emergencyContact);
            student.setAllergies(allergies);
            student.setMedicalNotes(medicalNotes);

            viewModel.addStudent(student, parentEmail, selectedImageUri)
                    .observe(getViewLifecycleOwner(), resource -> {
                        handleSaveResponse(resource, "Alumno agregado correctamente");
                    });
        }
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Helper para manejar la respuesta del ViewModel (crear o actualizar)
    private void handleSaveResponse(Resource<Student> resource, String successMessage) {
        if (resource != null) {
            switch (resource.getStatus()) {
                case LOADING:
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            successMessage,
                            Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(binding.getRoot()).navigateUp();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(requireContext(), resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}