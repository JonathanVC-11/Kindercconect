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
import com.example.kinderconnect.databinding.FragmentAddStudentBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.PermissionManager;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupListeners() {
        binding.ivStudentPhoto.setOnClickListener(v -> selectImage());
        binding.etBirthDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> saveStudent());
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedBirthDate = calendar.getTime();
                    String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    binding.etBirthDate.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveStudent() {
        String fullName = binding.etFullName.getText().toString().trim();
        String groupName = binding.etGroupName.getText().toString().trim();
        String parentEmail = binding.etParentEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String emergencyContact = binding.etEmergencyContact.getText().toString().trim();
        String allergies = binding.etAllergies.getText().toString().trim();
        String medicalNotes = binding.etMedicalNotes.getText().toString().trim();

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
        Student student = new Student(fullName, null, teacherId, groupName);
        student.setBirthDate(selectedBirthDate);
        student.setEmergencyContact(emergencyContact);
        student.setAllergies(allergies);
        student.setMedicalNotes(medicalNotes);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        viewModel.addStudent(student, parentEmail, selectedImageUri)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null) {
                        switch (resource.getStatus()) {
                            case LOADING:
                                break;
                            case SUCCESS:
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(),
                                        "Alumno agregado correctamente",
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
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
