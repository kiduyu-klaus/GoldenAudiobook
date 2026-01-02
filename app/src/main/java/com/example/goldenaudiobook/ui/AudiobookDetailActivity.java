package com.example.goldenaudiobook.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudioTrackAdapter;
import com.example.goldenaudiobook.databinding.ActivityAudiobookDetailBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.AudioTrack;
import com.example.goldenaudiobook.service.AudioPlaybackService;
import com.example.goldenaudiobook.viewmodel.AudiobookDetailViewModel;
import com.example.goldenaudiobook.viewmodel.FloatingPlayerViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying audiobook details with audio playback
 */
@UnstableApi public class AudiobookDetailActivity extends AppCompatActivity implements AudioTrackAdapter.OnTrackClickListener {

    private ActivityAudiobookDetailBinding binding;
    private AudiobookDetailViewModel viewModel;
    private FloatingPlayerViewModel floatingPlayerViewModel;
    private AudioTrackAdapter trackAdapter;
    private AudioPlaybackService playbackService;
    private boolean serviceBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlaybackService.AudioServiceBinder binder = (AudioPlaybackService.AudioServiceBinder) service;
            playbackService = binder.getService();
            serviceBound = true;

            // Set up playback state listener
            playbackService.setPlaybackStateListener(new AudioPlaybackService.PlaybackStateListener() {
                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    runOnUiThread(() -> updatePlayPauseButton(isPlaying));
                }

                @Override
                public void onMediaItemTransition(int currentIndex) {
                    runOnUiThread(() -> {
                        trackAdapter.setSelectedPosition(currentIndex);
                        viewModel.setCurrentTrack(currentIndex);
                    });
                }

                @Override
                public void onPlayerError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AudiobookDetailActivity.this,
                                "Error playing audio: " + error, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onPlaybackPositionChanged(long position, long duration) {
                    // Handled by periodic update
                }
            });

            // Load audiobook into service if we have it
            Audiobook audiobook = viewModel.getAudiobook().getValue();
            if (audiobook != null) {
                Integer currentIndex = viewModel.getCurrentTrackIndex().getValue();
                int trackIndex = currentIndex != null ? currentIndex : 0;
                playbackService.loadAudiobook(audiobook, trackIndex);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            playbackService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudiobookDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AudiobookDetailViewModel.class);

        // Initialize FloatingPlayerViewModel (shared across activities)
        floatingPlayerViewModel = new ViewModelProvider(this,
                new FloatingPlayerViewModel.Factory(getApplication()))
                .get(FloatingPlayerViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupPlayerControls();
        observeViewModel();

        // Bind to the playback service
        bindPlaybackService();

        // Load audiobook details
        String url = getIntent().getStringExtra("audiobook_url");
        if (url != null) {
            viewModel.loadAudiobookDetails(url);
        } else {
            Toast.makeText(this, "Invalid audiobook", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getWindow().setNavigationBarColor(
                ContextCompat.getColor(this, R.color.primary)
        );

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        trackAdapter = new AudioTrackAdapter(this);
        binding.trackList.setLayoutManager(new LinearLayoutManager(this));
        binding.trackList.setAdapter(trackAdapter);
    }

    private void setupPlayerControls() {
        // Play/Pause button
        binding.playPauseButton.setOnClickListener(v -> togglePlayPause());

        // Previous button
        binding.previousButton.setOnClickListener(v -> {
            viewModel.playPreviousTrack();
            if (serviceBound && playbackService != null) {
                playbackService.playPreviousTrack();
            }
        });

        // Next button
        binding.nextButton.setOnClickListener(v -> {
            viewModel.playNextTrack();
            if (serviceBound && playbackService != null) {
                playbackService.playNextTrack();
            }
        });

        // SeekBar
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (serviceBound && playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
                isUserSeeking = false;
            }
        });

        // Replay button
        binding.replayButton.setOnClickListener(v -> {
            if (serviceBound && playbackService != null) {
                playbackService.seekTo(0);
            }
        });
    }

    private void bindPlaybackService() {
        Intent serviceIntent = new Intent(this, AudioPlaybackService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void observeViewModel() {
        viewModel.getAudiobook().observe(this, audiobook -> {
            if (audiobook != null) {
                displayAudiobookInfo(audiobook);
                setupTracks(audiobook);

                // Update FloatingPlayerViewModel to show in floating player
                floatingPlayerViewModel.setCurrentAudiobook(audiobook);

                // Load audiobook into service if bound
                if (serviceBound && playbackService != null) {
                    Integer currentIndex = viewModel.getCurrentTrackIndex().getValue();
                    int trackIndex = currentIndex != null ? currentIndex : 0;
                    playbackService.loadAudiobook(audiobook, trackIndex);
                    floatingPlayerViewModel.setCurrentTrackIndex(trackIndex);
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.contentLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getIsPlaying().observe(this, isPlaying -> {
            updatePlayPauseButton(isPlaying);
        });

        viewModel.getCurrentTrackIndex().observe(this, index -> {
            if (index != null) {
                trackAdapter.setSelectedPosition(index);
            }
        });
    }

    private void displayAudiobookInfo(Audiobook audiobook) {
        // Set title
        String title = audiobook.getDisplayTitle();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        binding.bookTitle.setText(title);

        // Set author
        String author = audiobook.getDisplayAuthor();
        binding.bookAuthor.setText(author);

        // Set categories
        List<String> categories = audiobook.getCategories();
        if (categories != null && !categories.isEmpty()) {
            String categoryText = String.join(" • ", categories);
            binding.bookCategories.setText(categoryText);
            binding.bookCategories.setVisibility(View.VISIBLE);
        } else {
            binding.bookCategories.setVisibility(View.GONE);
        }

        // Set description
        String description = audiobook.getDescription();
        if (description != null && !description.isEmpty()) {
            binding.bookDescription.setText(description);
            binding.descriptionCard.setVisibility(View.VISIBLE);
        } else {
            binding.descriptionCard.setVisibility(View.GONE);
        }

        // Set published date
        String date = audiobook.getPublishedDate();
        if (date != null && !date.isEmpty()) {
            binding.bookDate.setText(date);
            binding.bookDate.setVisibility(View.VISIBLE);
        } else {
            binding.dateLabel.setVisibility(View.GONE);
        }

        // Load image
        String imageUrl = audiobook.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_book)
                    .error(R.drawable.placeholder_book)
                    .into(binding.bookCover);
        }
    }

    private void setupTracks(Audiobook audiobook) {
        List<String> audioUrls = audiobook.getAudioUrls();
        List<String> trackNames = audiobook.getTrackNames();

        if (audioUrls != null && !audioUrls.isEmpty()) {
            List<AudioTrack> tracks = new ArrayList<>();
            for (int i = 0; i < audioUrls.size(); i++) {
                String name = (trackNames != null && i < trackNames.size())
                        ? trackNames.get(i)
                        : "Track " + (i + 1);
                tracks.add(new AudioTrack(i + 1, name, audioUrls.get(i)));
            }
            trackAdapter.submitList(tracks);
            binding.playerCard.setVisibility(View.VISIBLE);
        } else {
            binding.playerCard.setVisibility(View.GONE);
        }
    }

    private void togglePlayPause() {
        if (serviceBound && playbackService != null) {
            playbackService.togglePlayPause();
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            binding.playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void updatePlayerUI() {
        if (serviceBound && playbackService != null) {
            long duration = playbackService.getDuration();
            long position = playbackService.getCurrentPosition();

            if (duration > 0) {
                binding.seekBar.setMax((int) duration);
                binding.seekBar.setProgress((int) position);
                viewModel.setDuration(duration);
            }

            binding.currentTime.setText(formatTime(position));
            binding.totalTime.setText(formatTime(duration));

            // Update navigation buttons
            binding.previousButton.setEnabled(viewModel.hasPreviousTrack());
            binding.nextButton.setEnabled(viewModel.hasNextTrack());

            // Update play/pause button
            updatePlayPauseButton(playbackService.isPlaying());
        }
    }

    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (serviceBound && playbackService != null && 
                    playbackService.isPlaying() && !isUserSeeking) {
                    updatePlayerUI();
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private void stopProgressUpdate() {
        handler.removeCallbacksAndMessages(null);
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
        viewModel.setCurrentTrack(position);
        trackAdapter.setSelectedPosition(position);

        // Update floating player
        floatingPlayerViewModel.setCurrentTrackIndex(position);
        floatingPlayerViewModel.setIsPlaying(true);

        if (serviceBound && playbackService != null) {
            playbackService.playTrack(position);
        }
    }

    @Override
    public void onTrackClick(AudioTrack track, int position) {
        viewModel.setCurrentTrack(position);
        trackAdapter.setSelectedPosition(position);

        // Update floating player
        floatingPlayerViewModel.setCurrentTrackIndex(position);
        floatingPlayerViewModel.setIsPlaying(true);

        if (serviceBound && playbackService != null) {
            playbackService.playTrack(position);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // Start progress updates when activity is visible
        if (serviceBound && playbackService != null && playbackService.isPlaying()) {
            startProgressUpdate();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind from service
        if (serviceBound) {
            if (playbackService != null) {
                playbackService.removePlaybackStateListener();
            }
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
