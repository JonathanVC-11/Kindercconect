package com.example.kinderconnect.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kinderconnect.R;
import com.example.kinderconnect.data.model.Notification;
import com.example.kinderconnect.databinding.ItemNotificationBinding;
import com.example.kinderconnect.utils.DateUtils;

public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {

    private OnItemClickListener listener;

    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Notification>() {
                @Override
                public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                    return oldItem.getNotificationId().equals(newItem.getNotificationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
                    return oldItem.isRead() == newItem.isRead() &&
                            oldItem.getTitle().equals(newItem.getTitle());
                }
            };

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        void bind(Notification notification) {
            binding.tvTitle.setText(notification.getTitle());
            binding.tvBody.setText(notification.getBody());

            if (notification.getTimestamp() != null) {
                binding.tvTimestamp.setText(DateUtils.getRelativeTimeString(notification.getTimestamp()));
            } else {
                binding.tvTimestamp.setText("");
            }

            // Set icono basado en tipo
            int iconRes;
            switch (notification.getType() != null ? notification.getType() : "") {
                case "ATTENDANCE":
                    iconRes = R.drawable.ic_calendar;
                    break;
                case "NEW_STUDENT":
                    iconRes = R.drawable.ic_person;
                    break;
                case "BUS_ROUTE":
                    iconRes = R.drawable.ic_bus;
                    break;
                case "NOTICE":
                default:
                    iconRes = R.drawable.ic_notifications; // El ícono genérico
                    break;
            }
            binding.ivIcon.setImageResource(iconRes);

            // Mostrar/Ocultar indicador de no leído
            binding.viewUnreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Notification notification);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}