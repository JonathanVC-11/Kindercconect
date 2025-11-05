package com.example.kinderconnect.ui.adapters;

import android.view.LayoutInflater;
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
                    return oldItem.getMediaUrl().equals(newItem.getMediaUrl());
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

        // --- ¡¡BLOQUE MODIFICADO AQUÍ!! ---
        void bind(GalleryItem item) {
            if (item.getMediaType().equals(Constants.MEDIA_VIDEO)) {
                // Es un VIDEO
                binding.ivPlayIcon.setVisibility(android.view.View.VISIBLE);

                // Cargar un placeholder (logo) porque Glide no puede cargar un video .mp4
                Glide.with(binding.getRoot().getContext())
                        .load(R.drawable.ic_logo) // Carga el logo como thumbnail
                        .placeholder(R.drawable.ic_logo)
                        .centerCrop()
                        .into(binding.ivThumbnail);

            } else {
                // Es una IMAGEN
                binding.ivPlayIcon.setVisibility(android.view.View.GONE);

                // Cargar la imagen normalmente
                Glide.with(binding.getRoot().getContext())
                        .load(item.getThumbnailUrl()) // Usa el thumbnail
                        .placeholder(R.drawable.ic_logo)
                        .centerCrop()
                        .into(binding.ivThumbnail);
            }
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