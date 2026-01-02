package com.example.goldenaudiobook.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudioTrackAdapter;
import com.example.goldenaudiobook.databinding.ExpandedPlayerBinding;
import com.example.goldenaudiobook.model.AudioTrack;
import com.example.goldenaudiobook.viewmodel.FloatingPlayerViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


import java.util.List;

/**
 * Bottom Sheet Dialog for Expanded Player with Playlist
 */
@UnstableApi public class BottomSheetPlayer extends BottomSheetDialogFragment {

    private static final String TAG = "BottomSheetPlayer";

    private ExpandedPlayerBinding binding;
    private FloatingPlayerViewModel viewModel;
    private AudioTrackAdapter playlistAdapter;

    private Runnable onDismissListener;

    public static BottomSheetPlayer newInstance() {
        return new BottomSheetPlayer();
    }

    public void setListener(Runnable listener) {
        this.onDismissListener = listener;
    }

    @OptIn(markerClass = UnstableApi.class) @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), 
                new FloatingPlayerViewModel.Factory(requireActivity().getApplication()))
                .get(FloatingPlayerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ExpandedPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupExpandedPlayer();
        observeViewModel();
    }

    private void setupExpandedPlayer() {
        // Setup RecyclerView for playlist
        playlistAdapter = new AudioTrackAdapter(position -> {
            viewModel.playTrack(position);
        });
        binding.trackList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        binding.trackList.setAdapter(playlistAdapter);

        // Collapse button
        binding.collapseButton.setOnClickListener(v -> {
            dismiss();
        });

        // Playback controls
        binding.playPauseButtonLarge.setOnClickListener(v -> {
            viewModel.togglePlayPause();
        });

        binding.previousButton.setOnClickListener(v -> {
            viewModel.playPreviousTrack();
        });

        binding.nextButton.setOnClickListener(v -> {
            viewModel.playNextTrack();
        });

        binding.replayButton.setOnClickListener(v -> {
            viewModel.seekTo(0);
        });

        binding.stopButton.setOnClickListener(v -> {
            viewModel.stopPlayback();
            dismiss();
        });

        // SeekBar
        binding.seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // Called when user starts dragging
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                viewModel.seekTo(seekBar.getProgress());
            }
        });
    }

    private void observeViewModel() {
        // Observe current audiobook
        viewModel.getCurrentAudiobook().observe(getViewLifecycleOwner(), audiobook -> {
            if (audiobook != null) {
                binding.trackTitleLarge.setText(audiobook.getDisplayTitle());
                binding.trackAuthorLarge.setText(audiobook.getDisplayAuthor());

                if (audiobook.getImageUrl() != null && !audiobook.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(this)
                            .load(audiobook.getImageUrl())
                            .placeholder(R.drawable.placeholder_book)
                            .error(R.drawable.placeholder_book)
                            .into(binding.albumArtLarge);
                }
            }
        });

        // Observe current track index
        viewModel.getCurrentTrackIndex().observe(getViewLifecycleOwner(), index -> {
            if (index != null && index >= 0) {
                playlistAdapter.setSelectedPosition(index);
                binding.trackList.scrollToPosition(index);
            }
        });

        // Observe playing state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                binding.playPauseButtonLarge.setImageResource(
                        isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
                );
            }
        });

        // Observe track list
        viewModel.getTrackList().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks != null) {
                playlistAdapter.submitList(tracks);
            }
        });

        // Observe position
        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != null && !isUserSeeking()) {
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    binding.seekBar.setProgress(position.intValue());
                    binding.currentTime.setText(formatTime(position));
                }
            }
        });

        // Observe duration
        viewModel.getDuration().observe(getViewLifecycleOwner(), dur -> {
            if (dur != null && dur > 0) {
                binding.seekBar.setMax(dur.intValue());
                binding.totalTime.setText(formatTime(dur));
            }
        });
    }

    private boolean isUserSeeking() {
        // Check if seekBar is being touched (simplified)
        return binding.seekBar.isPressed();
    }

    private String formatTime(long millis) {
        if (millis < 0) return "00:00";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.run();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
