package com.example.kinderconnect.ui.parent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ItemNoticeBinding;
import com.example.kinderconnect.data.model.Notice;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.DateUtils;

public class NoticeAdapter extends ListAdapter<Notice, NoticeAdapter.NoticeViewHolder> {
    private OnItemClickListener listener;
    private final String currentUserId;

    public NoticeAdapter(String currentUserId) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
    }

    private static final DiffUtil.ItemCallback<Notice> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Notice>() {
                @Override
                public boolean areItemsTheSame(@NonNull Notice oldItem, @NonNull Notice newItem) {
                    return oldItem.getNoticeId().equals(newItem.getNoticeId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Notice oldItem, @NonNull Notice newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle());
                }
            };

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoticeBinding binding = ItemNoticeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoticeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class NoticeViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoticeBinding binding;

        NoticeViewHolder(ItemNoticeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        void bind(Notice notice) {
            binding.tvTitle.setText(notice.getTitle());
            binding.tvDescription.setText(notice.getDescription());
            binding.tvTeacherName.setText("Por: " + notice.getTeacherName());

            if (notice.getPublishedAt() != null) {
                binding.tvDate.setText(DateUtils.formatDate(notice.getPublishedAt()));
            }

            // Category badge
            int categoryColor = getCategoryColor(notice.getCategory());
            binding.viewCategoryIndicator.setBackgroundColor(
                    binding.getRoot().getContext().getColor(categoryColor));

            // Read/Unread indicator
            if (notice.isReadByUser(currentUserId)) {
                binding.viewUnreadIndicator.setVisibility(View.GONE);
            } else {
                binding.viewUnreadIndicator.setVisibility(View.VISIBLE);
            }
        }

        private int getCategoryColor(String category) {
            switch (category) {
                case Constants.NOTICE_TAREA: return R.color.notice_homework;
                case Constants.NOTICE_EVENTO: return R.color.notice_event;
                case Constants.NOTICE_RECORDATORIO: return R.color.notice_reminder;
                case Constants.NOTICE_URGENTE: return R.color.notice_urgent;
                default: return R.color.notice_reminder;
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Notice notice);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
