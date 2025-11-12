package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentGradeViewBinding;
import com.example.kinderconnect.data.local.PreferencesManager; // <-- AÑADIDO
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.utils.Constants;

public class GradeViewFragment extends Fragment {
    private FragmentGradeViewBinding binding;
    private ParentViewModel viewModel;

    // --- INICIO DE CÓDIGO MODIFICADO ---
    private PreferencesManager preferencesManager; // <-- AÑADIDO
    private String studentId;
    private String studentName;
    // --- FIN DE CÓDIGO MODIFICADO ---

    private int selectedPeriod = 1;
    private String[] periods;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGradeViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        preferencesManager = new PreferencesManager(requireContext()); // <-- AÑADIDO

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Lógica para determinar el alumno
        if (getArguments() != null && getArguments().getString("studentId") != null) {
            // Opción 1: Venimos desde los accesos rápidos (Home)
            studentId = getArguments().getString("studentId");
            studentName = getArguments().getString("studentName");
        } else {
            // Opción 2: Venimos desde la barra de navegación inferior
            studentId = preferencesManager.getCurrentStudentId();
            studentName = preferencesManager.getCurrentStudentName();
        }

        if (studentId == null || studentName == null) {
            Toast.makeText(requireContext(), "Error: No se pudo cargar el alumno", Toast.LENGTH_SHORT).show();
            // Opcional: navegar de vuelta a Home
            return;
        }

        binding.tvStudentName.setText(studentName);
        // --- FIN DE CÓDIGO MODIFICADO ---

        setupPeriodDropdown();
        loadGrades(); // Llamar a loadGrades directamente
    }

    private void setupPeriodDropdown() {
        // ... (sin cambios) ...
        periods = new String[]{"Periodo 1", "Periodo 2", "Periodo 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                periods
        );
        binding.actPeriod.setAdapter(adapter);
        binding.actPeriod.setText(periods[0], false);

        binding.actPeriod.setOnItemClickListener((parent, view, position, id) -> {
            selectedPeriod = position + 1;
            loadGrades();
        });
    }

    private void loadGrades() {
        // ... (sin cambios) ...
        if (studentId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);

        viewModel.getGradeByStudentAndPeriod(studentId, selectedPeriod)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (binding == null) return;

                    if (resource != null) {
                        switch (resource.getStatus()) {
                            case LOADING:
                                break;
                            case SUCCESS:
                                binding.progressBar.setVisibility(View.GONE);
                                if (resource.getData() != null) {
                                    binding.scrollView.setVisibility(View.VISIBLE);
                                    binding.tvEmpty.setVisibility(View.GONE);
                                    displayGrades(resource.getData());
                                } else {
                                    binding.scrollView.setVisibility(View.GONE);
                                    binding.tvEmpty.setVisibility(View.VISIBLE);
                                }
                                break;
                            case ERROR:
                                binding.progressBar.setVisibility(View.GONE);
                                binding.scrollView.setVisibility(View.GONE);
                                binding.tvEmpty.setVisibility(View.VISIBLE);
                                Toast.makeText(requireContext(), resource.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

    // ... (El resto de la clase: displayGrades, setAreaData, getLevelText, getLevelColor, onDestroyView
    // no necesitan cambios) ...
    private void displayGrades(Grade grade) {
        if (grade.getEvaluations() == null) return;

        for (int i = 0; i < Constants.DEVELOPMENT_AREAS.length; i++) {
            String key = "area" + (i + 1);
            Grade.AreaEvaluation evaluation = grade.getEvaluations().get(key);

            if (evaluation != null) {
                setAreaData(i, evaluation);
            }
        }
    }

    private void setAreaData(int areaIndex, Grade.AreaEvaluation evaluation) {
        String level = getLevelText(evaluation.getLevel());
        int colorRes = getLevelColor(evaluation.getLevel());
        int color = ContextCompat.getColor(requireContext(), colorRes);

        switch (areaIndex) {
            case 0:
                binding.tvArea1Level.setText(level);
                binding.tvArea1Level.setTextColor(color);
                binding.tvArea1Observations.setText(evaluation.getObservations());
                break;
            case 1:
                binding.tvArea2Level.setText(level);
                binding.tvArea2Level.setTextColor(color);
                binding.tvArea2Observations.setText(evaluation.getObservations());
                break;
            case 2:
                binding.tvArea3Level.setText(level);
                binding.tvArea3Level.setTextColor(color);
                binding.tvArea3Observations.setText(evaluation.getObservations());
                break;
            case 3:
                binding.tvArea4Level.setText(level);
                binding.tvArea4Level.setTextColor(color);
                binding.tvArea4Observations.setText(evaluation.getObservations());
                break;
            case 4:
                binding.tvArea5Level.setText(level);
                binding.tvArea5Level.setTextColor(color);
                binding.tvArea5Observations.setText(evaluation.getObservations());
                break;
            case 5:
                binding.tvArea6Level.setText(level);
                binding.tvArea6Level.setTextColor(color);
                binding.tvArea6Observations.setText(evaluation.getObservations());
                break;
        }
    }

    private String getLevelText(String level) {
        if (level == null) return "Sin evaluar";
        switch (level) {
            case Constants.GRADE_REQUIERE_APOYO: return "Requiere apoyo";
            case Constants.GRADE_EN_DESARROLLO: return "En desarrollo";
            case Constants.GRADE_ESPERADO: return "Esperado";
            case Constants.GRADE_SOBRESALIENTE: return "Sobresaliente";
            default: return "Sin evaluar";
        }
    }

    private int getLevelColor(String level) {
        if (level == null) return R.color.onSurfaceVariant;
        switch (level) {
            case Constants.GRADE_REQUIERE_APOYO: return R.color.error;
            case Constants.GRADE_EN_DESARROLLO: return R.color.tertiary;
            case Constants.GRADE_ESPERADO: return R.color.green_500;
            case Constants.GRADE_SOBRESALIENTE: return R.color.primary;
            default: return R.color.onSurfaceVariant;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}