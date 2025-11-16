package com.example.kinderconnect.ui.teacher;

import android.content.Context; // <-- AÑADIDO
import android.os.Bundle;
import android.text.InputType; // <-- AÑADIDO
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // <-- AÑADIDO
import android.widget.FrameLayout; // <-- AÑADIDO
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.kinderconnect.R;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Group;
import com.example.kinderconnect.databinding.FragmentManageGroupBinding;
import com.example.kinderconnect.utils.Resource;
import com.example.kinderconnect.utils.ValidationUtils; // <-- AÑADIDO

public class ManageGroupFragment extends Fragment {

    private FragmentManageGroupBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private Group currentGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupListeners();
        loadCurrentGroup();
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnCreateGroup.setOnClickListener(v -> validateAndCreateGroup());

        // --- INICIO DE CÓDIGO MODIFICADO ---
        binding.tvTransferGroup.setOnClickListener(v -> {
            if (currentGroup != null) {
                showTransferGroupDialog();
            } else {
                Toast.makeText(requireContext(), "No hay grupo para transferir", Toast.LENGTH_SHORT).show();
            }
        });
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private void showTransferGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Transferir Grupo");
        builder.setMessage("Ingresa el correo electrónico de la nueva maestra a la que deseas transferir este grupo y todos sus alumnos.");

        // Configurar el EditText
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("correo@maestra.com");

        // Añadir márgenes al EditText
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) getResources().getDimension(R.dimen.padding_normal);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        // Configurar botones
        builder.setPositiveButton("Transferir", (dialog, which) -> {
            String newEmail = input.getText().toString().trim().toLowerCase();
            if (ValidationUtils.isValidEmail(newEmail)) {
                if (newEmail.equals(preferencesManager.getUserEmail())) {
                    Toast.makeText(requireContext(), "No puedes transferir el grupo a ti misma", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Iniciar la transferencia
                performTransfer(newEmail);
            } else {
                Toast.makeText(requireContext(), "Por favor, ingresa un correo válido", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void performTransfer(String newEmail) {
        setLoading(true, "Transfiriendo...");

        viewModel.transferGroup(currentGroup, newEmail).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            // Solo actuar cuando no esté cargando
            if (resource.getStatus() != Resource.Status.LOADING) {
                setLoading(false, null);
                handleTransferResponse(resource);
            }
        });
    }

    private void handleTransferResponse(Resource<Void> resource) {
        if (resource.getStatus() == Resource.Status.SUCCESS) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Transferencia Exitosa")
                    .setMessage("El grupo y todos sus alumnos han sido transferidos a la nueva maestra. Serás redirigida al inicio.")
                    .setPositiveButton("Aceptar", (dialog, which) -> {
                        if (binding != null) {
                            Navigation.findNavController(binding.getRoot()).navigateUp();
                        }
                    })
                    .setCancelable(false)
                    .show();

        } else if (resource.getStatus() == Resource.Status.ERROR) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Error en la Transferencia")
                    .setMessage(resource.getMessage())
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }
    // --- FIN DE CÓDIGO AÑADIDO ---


    private void loadCurrentGroup() {
        String teacherId = preferencesManager.getUserId();
        if (teacherId == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true, null); // Solo el ProgressBar

        viewModel.getGroupForTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            setLoading(false, null);

            if (resource.getStatus() == Resource.Status.SUCCESS) {
                currentGroup = resource.getData();
                updateUI();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentGroup != null) {
            // La maestra YA tiene un grupo
            binding.cardCurrentGroup.setVisibility(View.VISIBLE);
            binding.layoutCreateGroup.setVisibility(View.GONE);
            binding.tvCurrentGroupInfo.setText(
                    String.format("Grado: %s, Grupo: %s", currentGroup.getGrade(), currentGroup.getGroupName())
            );
        } else {
            // La maestra NO tiene grupo
            binding.cardCurrentGroup.setVisibility(View.GONE);
            binding.layoutCreateGroup.setVisibility(View.VISIBLE);
        }
    }

    private void validateAndCreateGroup() {
        String grade = binding.etGrade.getText().toString().trim();
        String groupName = binding.etGroupName.getText().toString().trim().toUpperCase();

        if (grade.isEmpty()) {
            binding.tilGrade.setError("Ingresa el grado");
            return;
        }
        binding.tilGrade.setError(null);

        if (groupName.isEmpty()) {
            binding.tilGroupName.setError("Ingresa el grupo");
            return;
        }
        binding.tilGroupName.setError(null);

        String teacherId = preferencesManager.getUserId();
        String teacherEmail = preferencesManager.getUserEmail();
        String teacherName = preferencesManager.getUserName();

        if (teacherId == null || teacherEmail == null) {
            Toast.makeText(requireContext(), "Error de sesión, vuelve a iniciar", Toast.LENGTH_SHORT).show();
            return;
        }

        Group newGroup = new Group(teacherId, teacherEmail, teacherName, grade, groupName);

        setLoading(true, "Creando grupo...");

        viewModel.createGroup(newGroup).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.getStatus() != Resource.Status.LOADING) {
                setLoading(false, null);
            }

            switch (resource.getStatus()) {
                case SUCCESS:
                    Toast.makeText(requireContext(), "Grupo creado exitosamente", Toast.LENGTH_SHORT).show();
                    // 'loadCurrentGroup' se disparará solo gracias al SnapshotListener,
                    // actualizando la UI automáticamente.
                    break;
                case ERROR:
                    // Mostrar error de duplicado o de maestra
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Error al crear grupo")
                            .setMessage(resource.getMessage())
                            .setPositiveButton("Aceptar", null)
                            .show();
                    break;
                case LOADING:
                    break;
            }
        });
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---
    private void setLoading(boolean isLoading, @Nullable String message) {
        if (binding == null) return;

        if (message != null) {
            // Usar el Overlay
            binding.progressBar.setVisibility(View.GONE);
            binding.loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.tvLoadingText.setText(message);
        } else {
            // Usar solo el ProgressBar pequeño
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.loadingOverlay.setVisibility(View.GONE);
        }

        binding.btnCreateGroup.setEnabled(!isLoading);
        binding.tvTransferGroup.setEnabled(!isLoading);
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}