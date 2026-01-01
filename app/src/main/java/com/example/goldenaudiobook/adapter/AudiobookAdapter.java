package com.example.goldenaudiobook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.databinding.ItemAudiobookCardBinding;
import com.example.goldenaudiobook.model.Audiobook;

import java.util.List;

/**
 * RecyclerView adapter for displaying audiobook cards
 */
public class AudiobookAdapter extends ListAdapter<Audiobook, AudiobookAdapter.AudiobookViewHolder> {

    private final OnAudiobookClickListener listener;

    public interface OnAudiobookClickListener {
        void onAudiobookClick(Audiobook audiobook);
    }

    public AudiobookAdapter(OnAudiobookClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Audiobook> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Audiobook>() {
                @Override
                public boolean areItemsTheSame(@NonNull Audiobook oldItem, @NonNull Audiobook newItem) {
                    return oldItem.getUrl() != null && oldItem.getUrl().equals(newItem.getUrl());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Audiobook oldItem, @NonNull Audiobook newItem) {
                    return oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle()) &&
                            oldItem.getImageUrl() != null && oldItem.getImageUrl().equals(newItem.getImageUrl());
                }
            };

    @NonNull
    @Override
    public AudiobookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAudiobookCardBinding binding = ItemAudiobookCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AudiobookViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AudiobookViewHolder holder, int position) {
        Audiobook audiobook = getItem(position);
        holder.bind(audiobook);
    }

    class AudiobookViewHolder extends RecyclerView.ViewHolder {
        private final ItemAudiobookCardBinding binding;

        AudiobookViewHolder(ItemAudiobookCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Audiobook audiobook) {
            // Set title
            String displayTitle = audiobook.getDisplayTitle();
            binding.titleText.setText(displayTitle);

            // Set author if available
            String author = audiobook.getAuthor();
            if (author != null && !author.isEmpty()) {
                binding.authorText.setText(author);
                binding.authorText.setVisibility(View.VISIBLE);
            } else {
                binding.authorText.setVisibility(View.GONE);
            }

            // Set categories
            List<String> categories = audiobook.getCategories();
            if (categories != null && !categories.isEmpty()) {
                binding.categoryText.setText(categories.get(0));
                binding.categoryText.setVisibility(View.VISIBLE);
            } else {
                binding.categoryText.setVisibility(View.GONE);
            }

            // Set date
            String date = audiobook.getPublishedDate();
            if (date != null && !date.isEmpty()) {
                binding.dateText.setText(date);
                binding.dateText.setVisibility(View.VISIBLE);
            } else {
                binding.dateText.setVisibility(View.GONE);
            }

            // Load image with Glide
            String imageUrl = audiobook.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_book)
                        .error(R.drawable.placeholder_book)
                        .centerCrop()
                        .into(binding.coverImage);
            } else {
                binding.coverImage.setImageResource(R.drawable.placeholder_book);
            }

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAudiobookClick(audiobook);
                }
            });
        }
    }
}
