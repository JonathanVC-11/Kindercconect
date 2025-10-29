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

        // Obtener studentId de los argumentos
        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
        }

        setupToolbar();
        setupPeriodSpinner();
        setupListeners();
        loadExistingGrade();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupPeriodSpinner() {
        String[] periods = {"Periodo 1", "Periodo 2", "Periodo 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPeriod.setAdapter(adapter);

        binding.spinnerPeriod.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int position, long id) {
                        selectedPeriod = position + 1;
                        loadExistingGrade();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                }
        );
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
                        }
                    }
                });
    }

    private void loadGradeData() {
        // Cargar evaluaciones previas si existen
        if (currentGrade != null && currentGrade.getEvaluations() != null) {
            // Implementar lógica para cargar las evaluaciones en los RadioButtons
        }
    }

    private void saveGrade() {
        String teacherId = preferencesManager.getUserId();

        Grade grade = new Grade(studentId, teacherId, selectedPeriod);

        // Área 1: Lenguaje y Comunicación
        String level1 = getSelectedLevel(binding.rgArea1);
        String obs1 = binding.etObservations1.getText().toString().trim();
        grade.addEvaluation("area1", Constants.DEVELOPMENT_AREAS[0], level1, obs1);

        // Área 2: Pensamiento Matemático
        String level2 = getSelectedLevel(binding.rgArea2);
        String obs2 = binding.etObservations2.getText().toString().trim();
        grade.addEvaluation("area2", Constants.DEVELOPMENT_AREAS[1], level2, obs2);

        // Área 3: Exploración del Mundo Natural
        String level3 = getSelectedLevel(binding.rgArea3);
        String obs3 = binding.etObservations3.getText().toString().trim();
        grade.addEvaluation("area3", Constants.DEVELOPMENT_AREAS[2], level3, obs3);

        // Área 4: Desarrollo Personal y Social
        String level4 = getSelectedLevel(binding.rgArea4);
        String obs4 = binding.etObservations4.getText().toString().trim();
        grade.addEvaluation("area4", Constants.DEVELOPMENT_AREAS[3], level4, obs4);

        // Área 5: Expresión Artística
        String level5 = getSelectedLevel(binding.rgArea5);
        String obs5 = binding.etObservations5.getText().toString().trim();
        grade.addEvaluation("area5", Constants.DEVELOPMENT_AREAS[4], level5, obs5);

        // Área 6: Desarrollo Físico y Salud
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
        if (selectedId == -1) return Constants.GRADE_EN_DESARROLLO;

        RadioButton radioButton = binding.getRoot().findViewById(selectedId);
        String text = radioButton.getText().toString();

        if (text.contains("Requiere")) return Constants.GRADE_REQUIERE_APOYO;
        if (text.contains("desarrollo")) return Constants.GRADE_EN_DESARROLLO;
        if (text.contains("Esperado")) return Constants.GRADE_ESPERADO;
        if (text.contains("Sobresaliente")) return Constants.GRADE_SOBRESALIENTE;

        return Constants.GRADE_EN_DESARROLLO;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
