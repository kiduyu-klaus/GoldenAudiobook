package com.example.goldenaudiobook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.databinding.ItemAudioTrackBinding;
import com.example.goldenaudiobook.model.AudioTrack;

/**
 * RecyclerView adapter for displaying audio tracks
 */
public class AudioTrackAdapter extends ListAdapter<AudioTrack, AudioTrackAdapter.TrackViewHolder> {

    private final OnTrackClickListener listener;
    private int selectedPosition = -1;

    public interface OnTrackClickListener {
        void onTrackClick(AudioTrack track, int position);
    }

    public AudioTrackAdapter(OnTrackClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AudioTrack> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AudioTrack>() {
                @Override
                public boolean areItemsTheSame(@NonNull AudioTrack oldItem, @NonNull AudioTrack newItem) {
                    return oldItem.getTrackNumber() == newItem.getTrackNumber();
                }

                @Override
                public boolean areContentsTheSame(@NonNull AudioTrack oldItem, @NonNull AudioTrack newItem) {
                    return oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle());
                }
            };

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAudioTrackBinding binding = ItemAudioTrackBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TrackViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        AudioTrack track = getItem(position);
        holder.bind(track, position == selectedPosition);
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {
        private final ItemAudioTrackBinding binding;

        TrackViewHolder(ItemAudioTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AudioTrack track, boolean isSelected) {
            binding.trackNumber.setText(String.valueOf(track.getTrackNumber()));
            binding.trackTitle.setText(track.getDisplayTitle());

            // Highlight selected track
            if (isSelected) {
                binding.getRoot().setBackgroundResource(R.drawable.bg_selected_track);
            } else {
                binding.getRoot().setBackgroundResource(R.drawable.bg_track_item);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, getAdapterPosition());
                }
            });
        }
    }
}
