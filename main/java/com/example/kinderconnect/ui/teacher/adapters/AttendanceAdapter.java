package com.example.kinderconnect.ui.teacher.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ItemAttendanceBinding;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.Constants;
import java.util.HashMap;
import java.util.Map;

public class AttendanceAdapter extends ListAdapter<Student, AttendanceAdapter.AttendanceViewHolder> {
    private OnStatusChangeListener listener;
    private Map<String, String> attendanceMap = new HashMap<>();

    public AttendanceAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Student> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Student>() {
                @Override
                public boolean areItemsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
                    return oldItem.getStudentId().equals(newItem.getStudentId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
                    return oldItem.getFullName().equals(newItem.getFullName());
                }
            };

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceBinding binding = ItemAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceBinding binding;
        private Student currentStudent;

        AttendanceViewHolder(ItemAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Student student) {
            currentStudent = student;
            binding.tvStudentName.setText(student.getFullName());

            if (student.getPhotoUrl() != null && !student.getPhotoUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(student.getPhotoUrl())
                        .placeholder(R.drawable.ic_logo)
                        .circleCrop()
                        .into(binding.ivStudentPhoto);
            }

            // --- LÓGICA DE LISTENERS MODIFICADA ---
            // Quitar listeners antiguos para evitar llamadas duplicadas
            binding.chipGroup.setOnCheckedStateChangeListener(null);

            // Restaurar estado
            String status = attendanceMap.get(student.getStudentId());
            updateChipState(status);

            // Añadir nuevo listener
            binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return; // No hacer nada si se deselecciona

                int checkedId = checkedIds.get(0); // Es singleSelection
                String newStatus;
                if (checkedId == R.id.chipPresent) {
                    newStatus = Constants.ATTENDANCE_PRESENT;
                } else if (checkedId == R.id.chipLate) {
                    newStatus = Constants.ATTENDANCE_LATE;
                } else if (checkedId == R.id.chipAbsent) {
                    newStatus = Constants.ATTENDANCE_ABSENT;
                } else {
                    return;
                }

                attendanceMap.put(currentStudent.getStudentId(), newStatus);
                if (listener != null) {
                    listener.onStatusChange(currentStudent.getStudentId(), newStatus);
                }
            });
        }

        // --- LÓGICA DE UI MODIFICADA ---
        private void updateChipState(String status) {
            // Desmarcar todos primero
            binding.chipPresent.setChecked(false);
            binding.chipLate.setChecked(false);
            binding.chipAbsent.setChecked(false);

            // Marcar el correcto
            if (status != null) {
                switch (status) {
                    case Constants.ATTENDANCE_PRESENT:
                        binding.chipPresent.setChecked(true);
                        break;
                    case Constants.ATTENDANCE_LATE:
                        binding.chipLate.setChecked(true);
                        break;
                    case Constants.ATTENDANCE_ABSENT:
                        binding.chipAbsent.setChecked(true);
                        break;
                }
            }
        }
    }

    public interface OnStatusChangeListener {
        void onStatusChange(String studentId, String status);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        this.listener = listener;
    }

    public void updateAttendance(Map<String, String> attendanceMap) {
        this.attendanceMap = attendanceMap;
        notifyDataSetChanged();
    }
}