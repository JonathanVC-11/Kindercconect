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
import com.example.kinderconnect.utils.Resource;
import java.util.Calendar;

public class PublishNoticeFragment extends Fragment {
    private FragmentPublishNoticeBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String teacherGroupName = null;

    // --- AÑADIDO: Arrays para los menús ---
    private String[] categories;
    private String[] scopes;

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
        setupDropdowns(); // --- MÉTODO RENOMBRADO ---
        setupListeners();

        binding.btnPublish.setEnabled(false);
        loadTeacherGroup();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    // --- LÓGICA DE SPINNER REEMPLAZADA POR DROPDOWN ---
    private void setupDropdowns() {
        // Category dropdown
        categories = new String[]{"Tarea", "Evento", "Recordatorio", "Urgente"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line, // Usar layout de dropdown
                categories
        );
        binding.actCategory.setAdapter(categoryAdapter);
        binding.actCategory.setText(categories[0], false); // Poner valor por defecto

        // Scope dropdown
        scopes = new String[]{"Mi grupo", "Toda la escuela"};
        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                scopes
        );
        binding.actScope.setAdapter(scopeAdapter);
        binding.actScope.setText(scopes[0], false); // Poner valor por defecto
    }

    private void setupListeners() {
        binding.btnAttachImage.setOnClickListener(v -> selectImage());
        binding.btnPublish.setOnClickListener(v -> publishNotice());
    }

    private void loadTeacherGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null && !resource.getData().isEmpty()) {
                teacherGroupName = resource.getData().get(0).getGroupName();
                binding.btnPublish.setEnabled(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                teacherGroupName = null;
                binding.btnPublish.setEnabled(true);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), "Error al cargar datos del grupo", Toast.LENGTH_SHORT).show();
                binding.btnPublish.setEnabled(true);
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // --- LÓGICA DE PUBLICACIÓN MODIFICADA ---
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

        // Obtener valores desde el texto del AutoCompleteTextView
        String categoryString = binding.actCategory.getText().toString();
        String scopeString = binding.actScope.getText().toString();

        String category = getCategoryConstant(categoryString);
        String scope = scopeString.equals(scopes[0]) ? // Comparar con el array
                Constants.SCOPE_GROUP : Constants.SCOPE_SCHOOL;

        Notice notice = new Notice(teacherId, title, description, category, scope);
        notice.setTeacherName(teacherName);

        if (scope.equals(Constants.SCOPE_GROUP)) {
            if (teacherGroupName == null || teacherGroupName.isEmpty()) {
                Toast.makeText(requireContext(), "No tienes un grupo asignado para publicar", Toast.LENGTH_SHORT).show();
                return;
            }
            notice.setGroupName(teacherGroupName);
        }

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

    // --- LÓGICA DE CATEGORÍA MODIFICADA (acepta String) ---
    private String getCategoryConstant(String categoryText) {
        if (categoryText.equals(categories[0])) { // Tarea
            return Constants.NOTICE_TAREA;
        } else if (categoryText.equals(categories[1])) { // Evento
            return Constants.NOTICE_EVENTO;
        } else if (categoryText.equals(categories[2])) { // Recordatorio
            return Constants.NOTICE_RECORDATORIO;
        } else if (categoryText.equals(categories[3])) { // Urgente
            return Constants.NOTICE_URGENTE;
        }
        return Constants.NOTICE_RECORDATORIO; // Default
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}