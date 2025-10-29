package com.example.kinderconnect.ui.teacher.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ItemStudentBinding;
import com.example.kinderconnect.data.model.Student;
import com.example.kinderconnect.utils.DateUtils;

public class StudentAdapter extends ListAdapter<Student, StudentAdapter.StudentViewHolder> {
    private OnItemClickListener listener;

    public StudentAdapter() {
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
                    return oldItem.getFullName().equals(newItem.getFullName()) &&
                            oldItem.getGroupName().equals(newItem.getGroupName());
                }
            };

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        StudentViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        void bind(Student student) {
            binding.tvStudentName.setText(student.getFullName());
            binding.tvGroupName.setText(student.getGroupName());

            if (student.getBirthDate() != null) {
                binding.tvBirthDate.setText(DateUtils.formatDate(student.getBirthDate()));
            }

            if (student.getPhotoUrl() != null && !student.getPhotoUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(student.getPhotoUrl())
                        .placeholder(R.drawable.ic_logo)
                        .circleCrop()
                        .into(binding.ivStudentPhoto);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
