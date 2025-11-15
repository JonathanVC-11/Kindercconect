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
import com.example.kinderconnect.utils.DateUtils;

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

        // --- ¡¡LÓGICA 'BIND' MODIFICADA Y SIMPLIFICADA!! ---
        void bind(GalleryItem item) {

            boolean isVideo = item.getMediaType().equals(Constants.MEDIA_VIDEO);
            binding.ivPlayIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);

            // Usar el thumbnail si existe
            String urlToLoad = item.getThumbnailUrl();

            // Si no hay thumbnail (ej. video antiguo o foto), usar la URL original
            if (urlToLoad == null || urlToLoad.isEmpty()) {
                urlToLoad = item.getMediaUrl();
            }

            // Cargar la URL decidida (sea thumbnail o media original)
            Glide.with(binding.getRoot().getContext())
                    .load(urlToLoad)
                    .placeholder(R.drawable.ic_logo)
                    .centerCrop()
                    .into(binding.ivThumbnail);


            // Lógica para la fecha (sin cambios)
            if (item.getUploadedAt() != null) {
                binding.tvDate.setText(DateUtils.getRelativeTimeString(item.getUploadedAt()));
                binding.tvDate.setVisibility(View.VISIBLE);
            } else {
                binding.tvDate.setVisibility(View.GONE);
            }
        }
        // --- FIN DE LA MODIFICACIÓN ---
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