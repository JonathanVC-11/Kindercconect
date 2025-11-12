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

    private String[] categories;
    private String[] scopes;

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private String existingNoticeId = null;
    private Notice noticeToEdit = null;
    private boolean isEditMode = false;
    // --- FIN DE CÓDIGO AÑADIDO ---


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- INICIO DE CÓDIGO AÑADIDO ---
        // Recuperar el ID del aviso, si se pasó
        if (getArguments() != null) {
            existingNoticeId = getArguments().getString("noticeId");
            isEditMode = (existingNoticeId != null);
        }
        // --- FIN DE CÓDIGO AÑADIDO ---

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        binding.tvImageSelected.setVisibility(View.VISIBLE);
                        binding.tvImageSelected.setText("Nueva imagen seleccionada");
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
        setupDropdowns();
        setupListeners();

        // Deshabilitar el botón de publicar hasta que sepamos el grupo de la maestra
        binding.btnPublish.setEnabled(false);
        loadTeacherGroup();

        // --- INICIO DE CÓDIGO AÑADIDO ---
        // Si estamos en modo edición, cargar los datos
        if (isEditMode) {
            binding.toolbar.setTitle("Editar Aviso");
            binding.btnPublish.setText("Actualizar Aviso");
            loadNoticeData();
        } else {
            binding.toolbar.setTitle("Publicar Aviso");
            binding.btnPublish.setText("Publicar");
        }
        // --- FIN DE CÓDIGO AÑADIDO ---
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupDropdowns() {
        categories = new String[]{"Tarea", "Evento", "Recordatorio", "Urgente"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        binding.actCategory.setAdapter(categoryAdapter);
        if (!isEditMode) { // Solo poner por defecto si es nuevo
            binding.actCategory.setText(categories[0], false);
        }

        scopes = new String[]{"Mi grupo", "Toda la escuela"};
        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                scopes
        );
        binding.actScope.setAdapter(scopeAdapter);
        if (!isEditMode) { // Solo poner por defecto si es nuevo
            binding.actScope.setText(scopes[0], false);
        }
    }

    private void setupListeners() {
        binding.btnAttachImage.setOnClickListener(v -> selectImage());

        // --- CÓDIGO MODIFICADO ---
        // El botón ahora llama a 'saveNotice' que decide si crear o actualizar
        binding.btnPublish.setOnClickListener(v -> saveNotice());
        // --- FIN DE CÓDIGO MODIFICADO ---
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

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private void loadNoticeData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.getNoticeById(existingNoticeId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    noticeToEdit = resource.getData();
                    if (noticeToEdit != null) {
                        populateUi(noticeToEdit);
                    } else {
                        Toast.makeText(requireContext(), "No se pudo cargar el aviso", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(binding.getRoot()).navigateUp();
                    }
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(binding.getRoot()).navigateUp();
                    break;
            }
        });
    }

    private void populateUi(Notice notice) {
        binding.etTitle.setText(notice.getTitle());
        binding.etDescription.setText(notice.getDescription());

        // Mapear el valor constante (ej: "TAREA") al texto del dropdown (ej: "Tarea")
        binding.actCategory.setText(getCategoryString(notice.getCategory()), false);
        binding.actScope.setText(getScopeString(notice.getScope()), false);

        if (notice.getImageUrl() != null && !notice.getImageUrl().isEmpty()) {
            binding.tvImageSelected.setText("Imagen adjunta. Toca para cambiar.");
            binding.tvImageSelected.setVisibility(View.VISIBLE);
        } else {
            binding.tvImageSelected.setVisibility(View.GONE);
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    // --- MÉTODO 'publishNotice' RENOMBRADO Y MODIFICADO ---
    private void saveNotice() {
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

        String categoryString = binding.actCategory.getText().toString();
        String scopeString = binding.actScope.getText().toString();

        String category = getCategoryConstant(categoryString);
        String scope = scopeString.equals(scopes[0]) ?
                Constants.SCOPE_GROUP : Constants.SCOPE_SCHOOL;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnPublish.setEnabled(false);

        if (isEditMode) {
            // --- LÓGICA DE ACTUALIZACIÓN ---
            noticeToEdit.setTitle(title);
            noticeToEdit.setDescription(description);
            noticeToEdit.setCategory(category);
            noticeToEdit.setScope(scope);
            noticeToEdit.setTeacherName(teacherName); // Actualizar por si la maestra cambió de nombre

            if (scope.equals(Constants.SCOPE_GROUP)) {
                noticeToEdit.setGroupName(teacherGroupName);
            } else {
                noticeToEdit.setGroupName(null); // O ""
            }

            String oldImageUrl = (noticeToEdit.getImageUrl() != null) ? noticeToEdit.getImageUrl() : null;

            viewModel.updateNotice(noticeToEdit, selectedImageUri, oldImageUrl).observe(getViewLifecycleOwner(), resource -> {
                handleSaveResponse(resource, "Aviso actualizado correctamente");
            });

        } else {
            // --- LÓGICA DE CREACIÓN (la que ya tenías) ---
            Notice notice = new Notice(teacherId, title, description, category, scope);
            notice.setTeacherName(teacherName);

            if (scope.equals(Constants.SCOPE_GROUP)) {
                if (teacherGroupName == null || teacherGroupName.isEmpty()) {
                    Toast.makeText(requireContext(), "No tienes un grupo asignado para publicar", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnPublish.setEnabled(true);
                    return;
                }
                notice.setGroupName(teacherGroupName);
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            notice.setValidUntil(calendar.getTime());

            viewModel.publishNotice(notice, selectedImageUri).observe(getViewLifecycleOwner(), resource -> {
                handleSaveResponse(resource, "Aviso publicado correctamente");
            });
        }
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    // Helper para manejar la respuesta del ViewModel (crear o actualizar)
    private void handleSaveResponse(Resource<String> resource, String successMessage) {
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
                    binding.btnPublish.setEnabled(true);
                    Toast.makeText(requireContext(), resource.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    // Helper para mapear constante a string (para poblar UI)
    private String getCategoryString(String constant) {
        switch (constant) {
            case Constants.NOTICE_TAREA: return categories[0];
            case Constants.NOTICE_EVENTO: return categories[1];
            case Constants.NOTICE_RECORDATORIO: return categories[2];
            case Constants.NOTICE_URGENTE: return categories[3];
            default: return categories[2]; // Recordatorio por defecto
        }
    }

    // Helper para mapear constante a string (para poblar UI)
    private String getScopeString(String constant) {
        if (Constants.SCOPE_GROUP.equals(constant)) {
            return scopes[0]; // "Mi grupo"
        } else {
            return scopes[1]; // "Toda la escuela"
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---

    // Helper para mapear string a constante (para guardar en DB)
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