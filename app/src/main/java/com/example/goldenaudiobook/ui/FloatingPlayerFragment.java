package com.example.goldenaudiobook.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudioTrackAdapter;
import com.example.goldenaudiobook.databinding.MiniPlayerBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.AudioTrack;
import com.example.goldenaudiobook.service.AudioPlaybackService;
import com.example.goldenaudiobook.viewmodel.FloatingPlayerViewModel;

/**
 * Floating Player Fragment - visible across all activities/fragments
 * Shows mini player at bottom, expands to full player with playlist
 */
@UnstableApi public class FloatingPlayerFragment extends Fragment implements AudioTrackAdapter.OnTrackClickListener {

    private static final String TAG = "FloatingPlayer";

    private FloatingPlayerViewModel viewModel;
    private MiniPlayerBinding miniBinding;
    private View expandedView;

    private AudioPlaybackService playbackService;
    private boolean serviceBound = false;
    private AudioTrackAdapter playlistAdapter;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private boolean isUpdatingProgress = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            AudioPlaybackService.AudioServiceBinder binder = (AudioPlaybackService.AudioServiceBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
            viewModel.setPlaybackService(playbackService);
            viewModel.setServiceBound(true);

            // Sync state with service
            if (playbackService != null) {
                Audiobook currentBook = playbackService.getCurrentAudiobook();
                if (currentBook != null) {
                    viewModel.setCurrentAudiobook(currentBook);
                }
                boolean isPlaying = playbackService.isPlaying();
                viewModel.setIsPlaying(isPlaying);
                viewModel.setCurrentTrackIndex(playbackService.getCurrentTrackIndex());
                viewModel.setDuration(playbackService.getDuration());
                viewModel.setCurrentPosition(playbackService.getCurrentPosition());

                // Update play/pause buttons immediately
                updatePlayPauseButtons(isPlaying);

                // Add playback state listener to sync with service
                playbackService.setPlaybackStateListener(new AudioPlaybackService.PlaybackStateListener() {
                    @Override
                    public void onPlaybackStateChanged(boolean isPlaying) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.d(TAG, "Playback state changed: " + isPlaying);
                                playbackService.getPlayer().play();
                                viewModel.setIsPlaying(isPlaying);
                                updatePlayPauseButtons(isPlaying);


                                // Start or stop progress updates
                                if (isPlaying) {
                                    startProgressUpdate();
                                }
                            });
                        }
                    }

                    @Override
                    public void onMediaItemTransition(int currentIndex) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.d(TAG, "Media item transition: " + currentIndex);
                                viewModel.setCurrentTrackIndex(currentIndex);
                                playlistAdapter.setSelectedPosition(currentIndex);
                                updateExpandedPlayerSeekBar();
                            });
                        }
                    }

                    @Override
                    public void onPlayerError(String error) {
                        // Handle error silently in floating player
                        Log.e(TAG, "Player error: " + error);
                    }

                    @Override
                    public void onPlaybackPositionChanged(long position, long duration) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                viewModel.setCurrentPosition(position);
                                viewModel.setDuration(duration);

                                // Update UI directly
                                if (!isUserSeeking) {
                                    updateProgress(position, duration);
                                }
                            });
                        }
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            serviceBound = false;
            playbackService = null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new FloatingPlayerViewModel.Factory(requireActivity().getApplication()))
                .get(FloatingPlayerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate mini player layout
        miniBinding = MiniPlayerBinding.inflate(inflater, container, false);
        return miniBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMiniPlayer();
        setupExpandedPlayer();
        bindPlaybackService();
        observeViewModel();
    }

    private void setupMiniPlayer() {
        // Click to expand
        miniBinding.getRoot().setOnClickListener(v -> {
            viewModel.setExpanded(true);
            showExpandedPlayer();
        });

        // Play/Pause button
        miniBinding.playPauseButton.setOnClickListener(v -> {
            Log.d(TAG, "Mini player play/pause clicked");
            playbackService.getPlayer().play();
            //viewModel.togglePlayPause();
        });

        // Close button
        miniBinding.closeButton.setOnClickListener(v -> {
            viewModel.stopPlayback();
        });
    }

    private void setupExpandedPlayer() {
        // Create expanded view programmatically
        expandedView = LayoutInflater.from(requireContext()).inflate(R.layout.expanded_player, null);

        // Setup RecyclerView for playlist
        RecyclerView trackList = expandedView.findViewById(R.id.track_list);
        playlistAdapter = new AudioTrackAdapter(this);
        trackList.setLayoutManager(new LinearLayoutManager(requireContext()));
        trackList.setAdapter(playlistAdapter);

        // Collapse button
        expandedView.findViewById(R.id.collapse_button).setOnClickListener(v -> {
            viewModel.setExpanded(false);
            hideExpandedPlayer();
        });

        // Playback controls
        expandedView.findViewById(R.id.play_pause_button_large).setOnClickListener(v -> {
            Log.d(TAG, "Expanded player play/pause clicked");
            viewModel.togglePlayPause();
        });

        expandedView.findViewById(R.id.previous_button).setOnClickListener(v -> {
            viewModel.playPreviousTrack();
        });

        expandedView.findViewById(R.id.next_button).setOnClickListener(v -> {
            viewModel.playNextTrack();
        });

        expandedView.findViewById(R.id.replay_button).setOnClickListener(v -> {
            if (playbackService != null) {
                playbackService.seekTo(0);
            }
        });

        expandedView.findViewById(R.id.stop_button).setOnClickListener(v -> {
            viewModel.stopPlayback();
            viewModel.setExpanded(false);
            hideExpandedPlayer();
        });

        // SeekBar
        SeekBar seekBar = expandedView.findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    TextView currentTime = expandedView.findViewById(R.id.current_time);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                viewModel.seekTo(seekBar.getProgress());
                isUserSeeking = false;
            }
        });
    }

    private void showExpandedPlayer() {
        if (getActivity() != null) {
            // Create and show bottom sheet dialog
            BottomSheetPlayer bottomSheet = BottomSheetPlayer.newInstance();
            bottomSheet.setListener(() -> {
                viewModel.setExpanded(false);
            });
            bottomSheet.show(getParentFragmentManager(), "ExpandedPlayer");
        }
    }

    public void hideExpandedPlayer() {
        viewModel.setExpanded(false);
    }

    private void bindPlaybackService() {
        // Start the service first to ensure it's running
        Intent serviceIntent = new Intent(requireContext(), AudioPlaybackService.class);
        requireContext().startService(serviceIntent);

        // Then bind to it
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void observeViewModel() {
        // Observe player visibility
        viewModel.getIsPlayerVisible().observe(getViewLifecycleOwner(), visible -> {
            miniBinding.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
        });

        // Observe current audiobook
        viewModel.getCurrentAudiobook().observe(getViewLifecycleOwner(), audiobook -> {
            if (audiobook != null) {
                updateMiniPlayerInfo(audiobook);
                updateExpandedPlayerInfo(audiobook);
            }
        });

        // Observe current track index
        viewModel.getCurrentTrackIndex().observe(getViewLifecycleOwner(), index -> {
            if (index != null && index >= 0) {
                playlistAdapter.setSelectedPosition(index);
            }
        });

        // Observe playing state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                updatePlayPauseButtons(isPlaying);
            }
        });

        // Observe track list
        viewModel.getTrackList().observe(getViewLifecycleOwner(), tracks -> {
            if (tracks != null) {
                playlistAdapter.submitList(tracks);
            }
        });

        // Observe position for progress updates
        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            Long duration = viewModel.getDuration().getValue();
            if (position != null && duration != null) {
                if (!isUserSeeking) {
                    updateProgress(position, duration);
                }
            }
        });

        // Observe duration
        viewModel.getDuration().observe(getViewLifecycleOwner(), dur -> {
            if (dur != null && dur > 0) {
                SeekBar seekBar = expandedView.findViewById(R.id.seek_bar);
                if (seekBar != null) {
                    seekBar.setMax(dur.intValue());
                }
                TextView totalTime = expandedView.findViewById(R.id.total_time);
                if (totalTime != null) {
                    totalTime.setText(formatTime(dur));
                }
            }
        });
    }

    private void updateMiniPlayerInfo(Audiobook audiobook) {
        miniBinding.trackTitle.setText(audiobook.getDisplayTitle());
        miniBinding.trackAuthor.setText(audiobook.getDisplayAuthor());

        if (audiobook.getImageUrl() != null && !audiobook.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(audiobook.getImageUrl())
                    .placeholder(R.drawable.placeholder_book)
                    .error(R.drawable.placeholder_book)
                    .into(miniBinding.albumArt);
        }
    }

    private void updateExpandedPlayerInfo(Audiobook audiobook) {
        TextView titleLarge = expandedView.findViewById(R.id.track_title_large);
        TextView authorLarge = expandedView.findViewById(R.id.track_author_large);
        ImageView albumArtLarge = expandedView.findViewById(R.id.album_art_large);

        if (titleLarge != null) {
            titleLarge.setText(audiobook.getDisplayTitle());
        }
        if (authorLarge != null) {
            authorLarge.setText(audiobook.getDisplayAuthor());
        }
        if (albumArtLarge != null && audiobook.getImageUrl() != null && !audiobook.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(audiobook.getImageUrl())
                    .placeholder(R.drawable.placeholder_book)
                    .error(R.drawable.placeholder_book)
                    .into(albumArtLarge);
        }
    }

    private void updatePlayPauseButtons(boolean isPlaying) {
        Log.d(TAG, "Updating play/pause buttons: " + isPlaying);

        // Mini player button
        miniBinding.playPauseButton.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );

        // Expanded player button
        ImageButton playPauseLarge = expandedView.findViewById(R.id.play_pause_button_large);
        if (playPauseLarge != null) {
            playPauseLarge.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
            );
        }
    }

    private void updateProgress(Long position, Long duration) {
        if (position == null || duration == null) return;

        // Update mini player progress
        if (miniBinding.playbackProgress != null) {
            miniBinding.playbackProgress.setMax(100);
            if (duration > 0) {
                int progress = (int) ((position * 100) / duration);
                miniBinding.playbackProgress.setProgress(progress);
            }
        }

        // Update expanded player SeekBar
        SeekBar seekBar = expandedView.findViewById(R.id.seek_bar);
        if (seekBar != null && !isUserSeeking) {
            if (duration > 0) {
                seekBar.setMax(duration.intValue());
                seekBar.setProgress(position.intValue());
            }
        }

        // Update current time display
        TextView currentTime = expandedView.findViewById(R.id.current_time);
        if (currentTime != null) {
            currentTime.setText(formatTime(position));
        }
    }

    private void updateExpandedPlayerSeekBar() {
        if (playbackService != null) {
            Long duration = viewModel.getDuration().getValue();
            Long position = viewModel.getCurrentPosition().getValue();
            if (duration != null && duration > 0 && position != null) {
                Log.i(TAG, "updateExpandedPlayerSeekBar: "+duration);
                updateProgress(position, duration);
            }
        }
    }

    private void startProgressUpdate() {
        if (isUpdatingProgress) return;
        isUpdatingProgress = true;

        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!serviceBound || playbackService == null) {
                    isUpdatingProgress = false;
                    return;
                }

                // Update UI from service
                long duration = playbackService.getDuration();
                long position = playbackService.getCurrentPosition();
                updateProgress(position, duration);

                // Continue updates while playing
                if (playbackService.isPlaying()) {
                    progressHandler.postDelayed(this, 100);
                } else {
                    isUpdatingProgress = false;
                }
            }
        }, 100);
    }

    private String formatTime(long millis) {
        if (millis < 0) return "00:00";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onAudiobookClick(AudioTrack track, int position) {
        viewModel.playTrack(position);
    }

    @Override
    public void onTrackClick(AudioTrack track, int position) {
        viewModel.playTrack(position);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressHandler.removeCallbacksAndMessages(null);
        isUpdatingProgress = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save current playback position before pausing
        if (playbackService != null && serviceBound) {
            viewModel.setCurrentPosition(playbackService.getCurrentPosition());
            viewModel.setIsPlaying(playbackService.isPlaying());
        }
        viewModel.saveState();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Sync with service when fragment resumes
        if (playbackService != null && serviceBound) {
            Audiobook currentBook = playbackService.getCurrentAudiobook();
            if (currentBook != null) {
                viewModel.setCurrentAudiobook(currentBook);
            }
            boolean isPlaying = playbackService.isPlaying();
            viewModel.setIsPlaying(isPlaying);
            viewModel.setCurrentTrackIndex(playbackService.getCurrentTrackIndex());
            viewModel.setCurrentPosition(playbackService.getCurrentPosition());
            viewModel.setDuration(playbackService.getDuration());

            // Update play/pause buttons
            updatePlayPauseButtons(isPlaying);

            // Update progress
            long duration = playbackService.getDuration();
            long position = playbackService.getCurrentPosition();
            updateProgress(position, duration);
        }
    }
}
