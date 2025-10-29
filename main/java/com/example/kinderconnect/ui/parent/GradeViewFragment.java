package com.example.kinderconnect.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.kinderconnect.databinding.FragmentGradeViewBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Grade;
import com.example.kinderconnect.utils.Constants;

public class GradeViewFragment extends Fragment {
    private FragmentGradeViewBinding binding;
    private ParentViewModel viewModel;
    private PreferencesManager preferencesManager;
    private String studentId;
    private int selectedPeriod = 1;

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
        preferencesManager = new PreferencesManager(requireContext());

        setupPeriodSpinner();
        loadStudentData();
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
                        loadGrades();
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                }
        );
    }

    private void loadStudentData() {
        String parentId = preferencesManager.getUserId();

        viewModel.getStudentsByParent(parentId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.getStatus() ==
                    com.example.kinderconnect.utils.Resource.Status.SUCCESS) {

                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    studentId = resource.getData().get(0).getStudentId();
                    binding.tvStudentName.setText(resource.getData().get(0).getFullName());
                    loadGrades();
                }
            }
        });
    }

    private void loadGrades() {
        if (studentId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);

        viewModel.getGradeByStudentAndPeriod(studentId, selectedPeriod)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource != null) {
                        switch (resource.getStatus()) {
                            case LOADING:
                                break;

                            case SUCCESS:
                                binding.progressBar.setVisibility(View.GONE);
                                if (resource.getData() != null) {
                                    binding.scrollView.setVisibility(View.VISIBLE);
                                    displayGrades(resource.getData());
                                } else {
                                    binding.tvEmpty.setVisibility(View.VISIBLE);
                                }
                                break;

                            case ERROR:
                                binding.progressBar.setVisibility(View.GONE);
                                binding.tvEmpty.setVisibility(View.VISIBLE);
                                Toast.makeText(requireContext(), resource.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
    }

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
        int color = getLevelColor(evaluation.getLevel());

        switch (areaIndex) {
            case 0:
                binding.tvArea1Level.setText(level);
                binding.tvArea1Level.setTextColor(requireContext().getColor(color));
                binding.tvArea1Observations.setText(evaluation.getObservations());
                break;
            case 1:
                binding.tvArea2Level.setText(level);
                binding.tvArea2Level.setTextColor(requireContext().getColor(color));
                binding.tvArea2Observations.setText(evaluation.getObservations());
                break;
            case 2:
                binding.tvArea3Level.setText(level);
                binding.tvArea3Level.setTextColor(requireContext().getColor(color));
                binding.tvArea3Observations.setText(evaluation.getObservations());
                break;
            case 3:
                binding.tvArea4Level.setText(level);
                binding.tvArea4Level.setTextColor(requireContext().getColor(color));
                binding.tvArea4Observations.setText(evaluation.getObservations());
                break;
            case 4:
                binding.tvArea5Level.setText(level);
                binding.tvArea5Level.setTextColor(requireContext().getColor(color));
                binding.tvArea5Observations.setText(evaluation.getObservations());
                break;
            case 5:
                binding.tvArea6Level.setText(level);
                binding.tvArea6Level.setTextColor(requireContext().getColor(color));
                binding.tvArea6Observations.setText(evaluation.getObservations());
                break;
        }
    }

    private String getLevelText(String level) {
        switch (level) {
            case Constants.GRADE_REQUIERE_APOYO: return "Requiere apoyo";
            case Constants.GRADE_EN_DESARROLLO: return "En desarrollo";
            case Constants.GRADE_ESPERADO: return "Esperado";
            case Constants.GRADE_SOBRESALIENTE: return "Sobresaliente";
            default: return "Sin evaluar";
        }
    }

    private int getLevelColor(String level) {
        switch (level) {
            case Constants.GRADE_REQUIERE_APOYO: return android.R.color.holo_red_dark;
            case Constants.GRADE_EN_DESARROLLO: return android.R.color.holo_orange_dark;
            case Constants.GRADE_ESPERADO: return android.R.color.holo_green_dark;
            case Constants.GRADE_SOBRESALIENTE: return android.R.color.holo_blue_dark;
            default: return android.R.color.darker_gray;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
