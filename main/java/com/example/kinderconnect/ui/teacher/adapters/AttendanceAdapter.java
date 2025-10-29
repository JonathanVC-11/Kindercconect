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

            setupListeners();
        }

        private void setupListeners() {
            binding.btnPresent.setOnClickListener(v -> {
                if (currentStudent != null) {
                    updateStatus(Constants.ATTENDANCE_PRESENT);
                }
            });

            binding.btnLate.setOnClickListener(v -> {
                if (currentStudent != null) {
                    updateStatus(Constants.ATTENDANCE_LATE);
                }
            });

            binding.btnAbsent.setOnClickListener(v -> {
                if (currentStudent != null) {
                    updateStatus(Constants.ATTENDANCE_ABSENT);
                }
            });
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

            // Restaurar estado
            String status = attendanceMap.get(student.getStudentId());
            updateButtonState(status);
        }

        private void updateStatus(String status) {
            attendanceMap.put(currentStudent.getStudentId(), status);
            updateButtonState(status);

            if (listener != null) {
                listener.onStatusChange(currentStudent.getStudentId(), status);
            }
        }

        private void updateButtonState(String status) {
            // Reset all buttons
            binding.btnPresent.setBackgroundTintList(
                    binding.getRoot().getContext().getColorStateList(R.color.divider));
            binding.btnLate.setBackgroundTintList(
                    binding.getRoot().getContext().getColorStateList(R.color.divider));
            binding.btnAbsent.setBackgroundTintList(
                    binding.getRoot().getContext().getColorStateList(R.color.divider));

            // Highlight selected
            if (status != null) {
                switch (status) {
                    case Constants.ATTENDANCE_PRESENT:
                        binding.btnPresent.setBackgroundTintList(
                                binding.getRoot().getContext().getColorStateList(R.color.attendance_present));
                        break;
                    case Constants.ATTENDANCE_LATE:
                        binding.btnLate.setBackgroundTintList(
                                binding.getRoot().getContext().getColorStateList(R.color.attendance_late));
                        break;
                    case Constants.ATTENDANCE_ABSENT:
                        binding.btnAbsent.setBackgroundTintList(
                                binding.getRoot().getContext().getColorStateList(R.color.attendance_absent));
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
