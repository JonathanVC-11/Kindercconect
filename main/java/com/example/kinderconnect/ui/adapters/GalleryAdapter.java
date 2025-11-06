package com.example.kinderconnect.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kinderconnect.R;
import com.example.kinderconnect.databinding.ItemGalleryBinding;
import com.example.kinderconnect.data.model.GalleryItem;
import com.example.kinderconnect.utils.Constants;
import com.example.kinderconnect.utils.DateUtils; // <-- AÑADIDO

public class GalleryAdapter extends ListAdapter<GalleryItem, GalleryAdapter.GalleryViewHolder> {
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public GalleryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<GalleryItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GalleryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull GalleryItem oldItem, @NonNull GalleryItem newItem) {
                    return oldItem.getItemId().equals(newItem.getItemId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull GalleryItem oldItem, @NonNull GalleryItem newItem) {
                    // Comprobar ambas URLs y la fecha
                    boolean urlsMatch = oldItem.getMediaUrl().equals(newItem.getMediaUrl()) &&
                            (oldItem.getThumbnailUrl() == null ? newItem.getThumbnailUrl() == null : oldItem.getThumbnailUrl().equals(newItem.getThumbnailUrl()));
                    boolean datesMatch = (oldItem.getUploadedAt() == null ? newItem.getUploadedAt() == null : oldItem.getUploadedAt().equals(newItem.getUploadedAt()));
                    return urlsMatch && datesMatch;
                }
            };

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGalleryBinding binding = ItemGalleryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GalleryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class GalleryViewHolder extends RecyclerView.ViewHolder {
        private final ItemGalleryBinding binding;

        GalleryViewHolder(ItemGalleryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    longClickListener.onItemLongClick(getItem(position));
                    return true;
                }
                return false;
            });
        }

        // --- ¡¡LÓGICA 'BIND' MODIFICADA!! ---
        void bind(GalleryItem item) {

            // Condición 1: Es un video
            boolean isVideo = item.getMediaType().equals(Constants.MEDIA_VIDEO);
            // Condición 2: El thumbnail no existe o está vacío
            boolean noThumbnail = item.getThumbnailUrl() == null || item.getThumbnailUrl().isEmpty();

            if (isVideo || noThumbnail) {
                // Es un VIDEO o es una IMAGEN SIN THUMBNAIL
                binding.ivPlayIcon.setVisibility(isVideo ? android.view.View.VISIBLE : android.view.View.GONE);

                // Cargar un placeholder
                Glide.with(binding.getRoot().getContext())
                        .load(R.drawable.ic_logo)
                        .placeholder(R.drawable.ic_logo)
                        .centerCrop()
                        .into(binding.ivThumbnail);

            } else {
                // Es una IMAGEN y SÍ tiene un thumbnail
                binding.ivPlayIcon.setVisibility(android.view.View.GONE);

                // Cargar la miniatura comprimida (thumbnailUrl)
                Glide.with(binding.getRoot().getContext())
                        .load(item.getThumbnailUrl())
                        .placeholder(R.drawable.ic_logo)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            }

            // --- AÑADIDO: Lógica para la fecha ---
            if (item.getUploadedAt() != null) {
                // Usamos el método de tiempo relativo que ya tienes en DateUtils
                binding.tvDate.setText(DateUtils.getRelativeTimeString(item.getUploadedAt()));
                binding.tvDate.setVisibility(View.VISIBLE);
            } else {
                binding.tvDate.setVisibility(View.GONE);
            }
            // ------------------------------------
        }
    }

    public interface OnItemClickListener {
        void onItemClick(GalleryItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(GalleryItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }
}