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

    // --- INICIO DE CÓDIGO AÑADIDO ---
    private OnItemLongClickListener longClickListener;
    // --- FIN DE CÓDIGO AÑADIDO ---

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

            // Clic normal
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });

            // --- INICIO DE CÓDIGO AÑADIDO ---
            // Clic largo (pulsación larga)
            binding.getRoot().setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    longClickListener.onItemLongClick(getItem(position));
                    return true; // Importante: consumir el evento
                }
                return false;
            });
            // --- FIN DE CÓDIGO AÑADIDO ---
        }

        void bind(Student student) {
            binding.tvStudentName.setText(student.getFullName());
            binding.tvGroupName.setText(student.getGroupName());

            if (student.getBirthDate() != null) {
                binding.tvBirthDate.setText(DateUtils.formatDate(student.getBirthDate()));
            }

            // --- INICIO DE CÓDIGO MODIFICADO ---
            // Añadido un placeholder por defecto si no hay foto
            if (student.getPhotoUrl() != null && !student.getPhotoUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(student.getPhotoUrl())
                        .placeholder(R.drawable.ic_logo)
                        .circleCrop()
                        .into(binding.ivStudentPhoto);
            } else {
                Glide.with(binding.getRoot().getContext())
                        .load(R.drawable.ic_logo) // Imagen por defecto
                        .circleCrop()
                        .into(binding.ivStudentPhoto);
            }
            // --- FIN DE CÓDIGO MODIFICADO ---
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    public interface OnItemLongClickListener {
        void onItemLongClick(Student student);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }
    // --- FIN DE CÓDIGO AÑADIDO ---
}