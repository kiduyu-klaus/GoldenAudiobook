package com.example.goldenaudiobook.ui;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudioTrackAdapter;
import com.example.goldenaudiobook.databinding.ActivityAudiobookDetailBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.AudioTrack;
import com.example.goldenaudiobook.viewmodel.AudiobookDetailViewModel;
import com.example.goldenaudiobook.util.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying audiobook details with audio playback
 */
public class AudiobookDetailActivity extends AppCompatActivity implements AudioTrackAdapter.OnTrackClickListener {

    private ActivityAudiobookDetailBinding binding;
    private AudiobookDetailViewModel viewModel;
    private AudioTrackAdapter trackAdapter;
    private androidx.media3.exoplayer.ExoPlayer player;
    private MediaSession mediaSession;
    private NotificationHelper notificationHelper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudiobookDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AudiobookDetailViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupPlayerControls();
        observeViewModel();

        // Initialize player
        initializePlayer();

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
            playCurrentTrack();
        });

        // Next button
        binding.nextButton.setOnClickListener(v -> {
            viewModel.playNextTrack();
            playCurrentTrack();
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
                if (player != null) {
                    player.seekTo(seekBar.getProgress());

                }
                isUserSeeking = false;
            }
        });

        // Replay button
        binding.replayButton.setOnClickListener(v -> {
            if (player != null) {
                player.seekTo(0);
            }
        });
    }

    private void initializePlayer() {
        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);
        notificationHelper.createNotificationChannel(this);

        player = new androidx.media3.exoplayer.ExoPlayer.Builder(this).build();

        // Initialize MediaSession
        mediaSession = new MediaSession.Builder(this, player).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updatePlayerUI();
                updateNotification();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                viewModel.setPlaying(isPlaying);
                updatePlayerUI();
                if (isPlaying) {
                    startProgressUpdate();
                } else {
                    stopProgressUpdate();
                }
                updateNotification();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(AudiobookDetailActivity.this,
                        "Error playing audio: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                Integer currentIndex = viewModel.getCurrentTrackIndex().getValue();
                if (currentIndex != null) {
                    trackAdapter.setSelectedPosition(currentIndex);
                }
                updateNotification();
            }
        });

        binding.playerView.setPlayer(player);
    }

    private void updateNotification() {
        if (notificationHelper != null && mediaSession != null) {
            Audiobook audiobook = viewModel.getAudiobook().getValue();

            if (audiobook != null) {
                // Use the overloaded method with full audiobook object
                NotificationHelper.showNotification(
                        AudiobookDetailActivity.this,
                        player,
                        mediaSession,
                        audiobook,
                        player.isPlaying());
            } else {
                // Fallback to simple version
                NotificationHelper.showNotification(
                        AudiobookDetailActivity.this,
                        player,
                        mediaSession,
                        "Audiobook",
                        "Unknown Author",
                        player.isPlaying());
            }
        }
    }

    private void observeViewModel() {
        viewModel.getAudiobook().observe(this, audiobook -> {
            if (audiobook != null) {
                displayAudiobookInfo(audiobook);
                setupTracks(audiobook);
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
                playCurrentTrack();
            }
        });
    }

    private void displayAudiobookInfo(Audiobook audiobook) {
        // Set title
        String title = audiobook.getDisplayTitle();
        getSupportActionBar().setTitle(title);
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

    private void playCurrentTrack() {
        Audiobook audiobook = viewModel.getAudiobook().getValue();
        Integer currentIndex = viewModel.getCurrentTrackIndex().getValue();

        if (audiobook != null && audiobook.getAudioUrls() != null && currentIndex != null
                && currentIndex < audiobook.getAudioUrls().size()) {

            String audioUrl = audiobook.getAudioUrls().get(currentIndex);
            Uri uri = Uri.parse(audioUrl);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }

    private void togglePlayPause() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        }
    }

    private void updatePlayerUI() {
        if (player != null) {
            // Update seekbar
            long duration = player.getDuration();
            long position = player.getCurrentPosition();

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
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            binding.playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            binding.playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying() && !isUserSeeking) {
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
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onTrackClick(AudioTrack track, int position) {
        viewModel.setCurrentTrack(position);
        trackAdapter.setSelectedPosition(position);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdate();

        // Release MediaSession
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }

        if (notificationHelper != null) {
            notificationHelper = null;
        }

        binding = null;
    }
}
