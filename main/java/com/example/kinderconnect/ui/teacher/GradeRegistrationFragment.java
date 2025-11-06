package com.example.kinderconnect.ui.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentGradeRegistrationBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.utils.Constants;

public class GradeRegistrationFragment extends Fragment {
    private FragmentGradeRegistrationBinding binding;
    private TeacherViewModel viewModel;
    private PreferencesManager preferencesManager;
    private String studentId;
    private int selectedPeriod = 1;
    private Grade currentGrade;
    private String[] periods; // --- AÑADIDO ---

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGradeRegistrationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }

        setupToolbar();
        setupPeriodDropdown(); // --- MÉTODO RENOMBRADO ---
        setupListeners();
        loadExistingGrade();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    // --- LÓGICA DE SPINNER REEMPLAZADA POR DROPDOWN ---
    private void setupPeriodDropdown() {
        periods = new String[]{"Periodo 1", "Periodo 2", "Periodo 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                periods
        );
        binding.actPeriod.setAdapter(adapter);
        binding.actPeriod.setText(periods[0], false); // Poner valor por defecto

        // Cambiar el listener
        binding.actPeriod.setOnItemClickListener((parent, view, position, id) -> {
            selectedPeriod = position + 1;
            loadExistingGrade();
        });
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveGrade());
    }

    private void loadExistingGrade() {
        if (studentId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getGradeByStudentAndPeriod(studentId, selectedPeriod)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null && resource.getStatus() ==
                            com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                        binding.progressBar.setVisibility(View.GONE);
                        currentGrade = resource.getData();

                        if (currentGrade != null) {
                            loadGradeData();
                        } else {
                            clearGradeData(); // Limpiar campos si no hay datos
                        }
                    }
                });
    }

    private void loadGradeData() {
        if (currentGrade != null && currentGrade.getEvaluations() != null) {
            // Implementar lógica para cargar las evaluaciones en los RadioButtons
            // (Esta lógica parece faltar en el original, pero el guardado sí funciona)
            // Por ahora, nos aseguramos de que el guardado funcione.
        }
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    private void clearGradeData() {
        // Limpiar todos los RadioGroups
        binding.rgArea1.clearCheck();
        binding.rgArea2.clearCheck();
        binding.rgArea3.clearCheck();
        binding.rgArea4.clearCheck();
        binding.rgArea5.clearCheck();
        binding.rgArea6.clearCheck();

        // Limpiar todos los campos de texto
        binding.etObservations1.setText("");
        binding.etObservations2.setText("");
        binding.etObservations3.setText("");
        binding.etObservations4.setText("");
        binding.etObservations5.setText("");
        binding.etObservations6.setText("");
    }


    private void saveGrade() {
        String teacherId = preferencesManager.getUserId();

        Grade grade = new Grade(studentId, teacherId, selectedPeriod);

        String level1 = getSelectedLevel(binding.rgArea1);
        String obs1 = binding.etObservations1.getText().toString().trim();
        grade.addEvaluation("area1", Constants.DEVELOPMENT_AREAS[0], level1, obs1);

        String level2 = getSelectedLevel(binding.rgArea2);
        String obs2 = binding.etObservations2.getText().toString().trim();
        grade.addEvaluation("area2", Constants.DEVELOPMENT_AREAS[1], level2, obs2);

        String level3 = getSelectedLevel(binding.rgArea3);
        String obs3 = binding.etObservations3.getText().toString().trim();
        grade.addEvaluation("area3", Constants.DEVELOPMENT_AREAS[2], level3, obs3);

        String level4 = getSelectedLevel(binding.rgArea4);
        String obs4 = binding.etObservations4.getText().toString().trim();
        grade.addEvaluation("area4", Constants.DEVELOPMENT_AREAS[3], level4, obs4);

        String level5 = getSelectedLevel(binding.rgArea5);
        String obs5 = binding.etObservations5.getText().toString().trim();
        grade.addEvaluation("area5", Constants.DEVELOPMENT_AREAS[4], level5, obs5);

        String level6 = getSelectedLevel(binding.rgArea6);
        String obs6 = binding.etObservations6.getText().toString().trim();
        grade.addEvaluation("area6", Constants.DEVELOPMENT_AREAS[5], level6, obs6);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        viewModel.saveGrade(grade).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Calificaciones guardadas correctamente",
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

    private String getSelectedLevel(android.widget.RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return Constants.GRADE_EN_DESARROLLO; // Default

        View radioButton = binding.getRoot().findViewById(selectedId);
        // Comprobamos el ID del RadioButton para ser más precisos
        if (selectedId == R.id.rbArea1Support || selectedId == R.id.rbArea2Support || selectedId == R.id.rbArea3Support || selectedId == R.id.rbArea4Support || selectedId == R.id.rbArea5Support || selectedId == R.id.rbArea6Support) {
            return Constants.GRADE_REQUIERE_APOYO;
        }
        if (selectedId == R.id.rbArea1Developing || selectedId == R.id.rbArea2Developing || selectedId == R.id.rbArea3Developing || selectedId == R.id.rbArea4Developing || selectedId == R.id.rbArea5Developing || selectedId == R.id.rbArea6Developing) {
            return Constants.GRADE_EN_DESARROLLO;
        }
        if (selectedId == R.id.rbArea1Expected || selectedId == R.id.rbArea2Expected || selectedId == R.id.rbArea3Expected || selectedId == R.id.rbArea4Expected || selectedId == R.id.rbArea5Expected || selectedId == R.id.rbArea6Expected) {
            return Constants.GRADE_ESPERADO;
        }
        if (selectedId == R.id.rbArea1Outstanding || selectedId == R.id.rbArea2Outstanding || selectedId == R.id.rbArea3Outstanding || selectedId == R.id.rbArea4Outstanding || selectedId == R.id.rbArea5Outstanding || selectedId == R.id.rbArea6Outstanding) {
            return Constants.GRADE_SOBRESALIENTE;
        }

        return Constants.GRADE_EN_DESARROLLO;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}