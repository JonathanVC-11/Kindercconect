package com.example.kinderconnect.ui.teacher;

import android.content.DialogInterface; // <-- AÑADIDO
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // <-- AÑADIDO
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.FragmentStudentListBinding;
import com.example.kinderconnect.data.local.PreferencesManager;
import com.example.kinderconnect.data.model.Student; // <-- AÑADIDO
import com.example.kinderconnect.ui.teacher.adapters.StudentAdapter;
import com.example.kinderconnect.utils.Resource;

public class StudentListFragment extends Fragment {
    private FragmentStudentListBinding binding;
    private TeacherViewModel viewModel;
    private StudentAdapter adapter;
    private PreferencesManager preferencesManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStudentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);
        preferencesManager = new PreferencesManager(requireContext());

        setupRecyclerView();
        // setupListeners(); // <-- ELIMINADO
        loadStudents();
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // Clic normal (para Calificaciones)
        adapter.setOnItemClickListener(student -> {
            Bundle args = new Bundle();
            args.putString("studentId", student.getStudentId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_students_to_grade_registration, args);
        });

        // --- INICIO DE CÓDIGO MODIFICADO ---
        // Clic largo (para Editar/Eliminar)
        adapter.setOnItemLongClickListener(student -> {
            showOptionsDialog(student);
        });
        // --- FIN DE CÓDIGO MODIFICADO ---
    }

    // --- INICIO DE CÓDIGO MODIFICADO ---
    private void showOptionsDialog(Student student) {
        // AHORA SOLO MOSTRAMOS "Eliminar Alumno"
        CharSequence[] options = {"Eliminar Alumno"};

        new AlertDialog.Builder(requireContext())
                .setTitle(student.getFullName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Eliminar
                        showDeleteConfirmationDialog(student);
                    }
                })
                .show();
    }

    private void showDeleteConfirmationDialog(Student student) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Alumno")
                .setMessage("¿Estás seguro que deseas eliminar a " + student.getFullName() + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteStudent(student))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteStudent(Student student) {
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.deleteStudent(student.getStudentId()).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.getStatus() != Resource.Status.LOADING) {
                binding.progressBar.setVisibility(View.GONE);
            }

            switch (resource.getStatus()) {
                case SUCCESS:
                    Toast.makeText(requireContext(), "Alumno eliminado", Toast.LENGTH_SHORT).show();
                    // La lista se actualizará sola gracias al SnapshotListener
                    break;
                case ERROR:
                    Toast.makeText(requireContext(), "Error al eliminar: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    break;
            }
        });
    }
    // --- FIN DE CÓDIGO MODIFICADO ---

    // --- MÉTODO ELIMINADO ---
    /*
    private void setupListeners() {
        binding.fabAddStudent.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(
                        R.id.action_students_to_add_student)); // Navega sin argumentos
    }
    */

    // --- LÓGICA DE ESTADO VACÍO MODIFICADA ---
    // (Esta ya la tenías bien, sin cambios)
    private void loadStudents() {
        String teacherId = preferencesManager.getUserId();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.GONE); // Ocultar layout vacío

        viewModel.getStudentsByTeacher(teacherId).observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return; // Vista destruida
            if (resource != null) {
                switch (resource.getStatus()) {
                    case LOADING:
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.emptyView.getRoot().setVisibility(View.GONE);
                        break;

                    case SUCCESS:
                        binding.progressBar.setVisibility(View.GONE);
                        if (resource.getData() != null && !resource.getData().isEmpty()) {
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            binding.emptyView.getRoot().setVisibility(View.GONE);
                            adapter.submitList(resource.getData());
                        } else {
                            // MOSTRAR ESTADO VACÍO
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                            binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_people);
                            binding.emptyView.tvEmptyTitle.setText("No hay alumnos");
                            // --- TEXTO MODIFICADO ---
                            binding.emptyView.tvEmptySubtitle.setText("Los padres deben registrar a sus hijos con tu correo para que aparezcan aquí.");
                        }
                        break;

                    case ERROR:
                        binding.progressBar.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.GONE);
                        // MOSTRAR ESTADO DE ERROR
                        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
                        binding.emptyView.ivEmptyIcon.setImageResource(R.drawable.ic_close);
                        binding.emptyView.tvEmptyTitle.setText("Error");
                        binding.emptyView.tvEmptySubtitle.setText(resource.getMessage());
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