package com.example.goldenaudiobook.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.ui.AudiobookDetailActivity;
import com.example.goldenaudiobook.util.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreground service for background audio playback
 * Provides ExoPlayer and MediaSession for media controls
 */
@UnstableApi @OptIn(markerClass = UnstableApi.class)
public class AudioPlaybackService extends Service {

    private static final String TAG = "AudioPlaybackService";

    // Intent actions
    public static final String ACTION_PLAY = "com.example.goldenaudiobook.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.goldenaudiobook.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.goldenaudiobook.ACTION_STOP";
    public static final String ACTION_NEXT = "com.example.goldenaudiobook.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.goldenaudiobook.ACTION_PREVIOUS";

    // Intent extras
    public static final String EXTRA_AUDIOBOOK_URL = "audiobook_url";
    public static final String EXTRA_AUDIOBOOK_TITLE = "audiobook_title";
    public static final String EXTRA_AUDIO_URL = "audio_url";
    public static final String EXTRA_TRACK_INDEX = "track_index";
    public static final String EXTRA_PLAYBACK_POSITION = "playback_position";
    public static final String EXTRA_WAS_PLAYING = "was_playing";
    public static final String EXTRA_RESUME_FROM_STATE = "resume_from_state";
    public static final String EXTRA_AUDIO_URLS = "audio_urls";
    public static final String EXTRA_TRACK_NAMES = "track_names";

    // Notification ID
    private static final int NOTIFICATION_ID = 1001;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private NotificationHelper notificationHelper;
    private final IBinder binder = new AudioServiceBinder();
    private Audiobook currentAudiobook;
    private int currentTrackIndex = 0;
    private PlaybackStateListener playbackStateListener;
    private final Handler positionUpdateHandler = new Handler(Looper.getMainLooper());
    private final Runnable positionUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && playbackStateListener != null) {
                long position = player.getCurrentPosition();
                long duration = player.getDuration();
                playbackStateListener.onPlaybackPositionChanged(position, duration);
            }
            // Continue updating while playing
            if (player != null && player.isPlaying()) {
                positionUpdateHandler.postDelayed(this, 100);
            }
        }
    };

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    /**
     * Interface for notifying activity about playback state changes
     */
    public interface PlaybackStateListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onMediaItemTransition(int currentIndex);
        void onPlayerError(String error);
        void onPlaybackPositionChanged(long position, long duration);
    }

    /**
     * Binder for activity to communicate with service
     */
    public class AudioServiceBinder extends Binder {
        public AudioPlaybackService getService() {
            return AudioPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);

        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();

        // Initialize MediaSession
        mediaSession = new MediaSession.Builder(this, player)
                .setId("audiobook_playback_service")
                .build();

        // Set up player listeners
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "Playback state changed: " + playbackState);

                if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback ended");
                }

                if (playbackState == Player.STATE_READY) {
                    startForegroundService();
                }

                // Notify listener
                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(player.isPlaying());
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: " + isPlaying);

                if (isPlaying) {
                    startForegroundService();
                    // Start position updates
                    positionUpdateHandler.removeCallbacks(positionUpdateRunnable);
                    positionUpdateHandler.post(positionUpdateRunnable);
                } else {
                    // Stop position updates
                    positionUpdateHandler.removeCallbacks(positionUpdateRunnable);
                    // Final position update
                    if (playbackStateListener != null) {
                        playbackStateListener.onPlaybackPositionChanged(
                                player.getCurrentPosition(), player.getDuration());
                    }
                }

                // Notify listener
                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(isPlaying);
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Log.d(TAG, "Media item transition, reason: " + reason);
                currentTrackIndex = player.getCurrentMediaItemIndex();

                if (playbackStateListener != null) {
                    playbackStateListener.onMediaItemTransition(currentTrackIndex);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());

                if (playbackStateListener != null) {
                    playbackStateListener.onPlayerError(error.getMessage());
                }
            }
        });

        // Initialize PlayerNotificationManager
        notificationHelper.getPlayerNotificationManager(player, mediaSession);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));

        if (intent != null) {
            // Check if this is a resume from saved state
            boolean resumeFromState = intent.getBooleanExtra(EXTRA_RESUME_FROM_STATE, false);

            if (resumeFromState) {
                // Handle resume from saved state
                handleResumeFromState(intent);
            }

            // Check if this is a notification click resume
            boolean isResumeFromNotification = intent.getBooleanExtra("resume_from_notification", false);

            if (isResumeFromNotification) {
                // Handle resume from notification
                handleResumeFromNotification(intent);
            }

            // Handle audiobook data if provided (for new playback)
            if (intent.hasExtra(EXTRA_AUDIOBOOK_URL) && !isResumeFromNotification && !resumeFromState) {
                String url = intent.getStringExtra(EXTRA_AUDIOBOOK_URL);
                String title = intent.getStringExtra(EXTRA_AUDIOBOOK_TITLE);
                int trackIndex = intent.getIntExtra(EXTRA_TRACK_INDEX, 0);

                // Build audiobook from intent extras
                Audiobook audiobook = buildAudiobookFromIntent(intent);

                if (audiobook != null && audiobook.getAudioUrls() != null && !audiobook.getAudioUrls().isEmpty()) {
                    if (currentAudiobook == null || !url.equals(currentAudiobook.getUrl())) {
                        Log.d(TAG, "Loading new audiobook from intent: " + title);
                    }
                    loadAudiobook(audiobook, trackIndex);
                }
            }

            // Handle actions
            if (intent.getAction() != null) {
                handleAction(intent.getAction(), intent);
            }
        }

        return START_STICKY; // Service will be restarted if killed
    }

    /**
     * Build an Audiobook object from intent extras
     */
    private Audiobook buildAudiobookFromIntent(Intent intent) {
        String url = intent.getStringExtra(EXTRA_AUDIOBOOK_URL);
        String title = intent.getStringExtra(EXTRA_AUDIOBOOK_TITLE);
        String author = intent.getStringExtra("audiobook_author");
        String imageUrl = intent.getStringExtra("audiobook_image");

        if (url == null) return null;

        Audiobook audiobook = new Audiobook();
        audiobook.setUrl(url);
        audiobook.setTitle(title != null ? title : "");
        audiobook.setAuthor(author != null ? author : "");
        audiobook.setImageUrl(imageUrl != null ? imageUrl : "");

        // Get audio URLs from intent
        String audioUrlsStr = intent.getStringExtra(EXTRA_AUDIO_URLS);
        String trackNamesStr = intent.getStringExtra(EXTRA_TRACK_NAMES);

        if (audioUrlsStr != null && !audioUrlsStr.isEmpty()) {
            List<String> audioUrls = parseAudioUrls(audioUrlsStr);
            List<String> trackNames = trackNamesStr != null ? parseAudioUrls(trackNamesStr) : new ArrayList<>();
            audiobook.setAudioUrls(audioUrls);
            audiobook.setTrackNames(trackNames);
        }

        return audiobook;
    }

    private List<String> parseAudioUrls(String str) {
        List<String> result = new ArrayList<>();
        if (str != null && !str.isEmpty()) {
            String[] parts = str.split("\\|@@\\|");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }
        }
        return result;
    }

    /**
     * Handle resume from saved state (e.g., when app restarts)
     */
    private void handleResumeFromState(Intent intent) {
        String url = intent.getStringExtra(EXTRA_AUDIOBOOK_URL);
        int trackIndex = intent.getIntExtra(EXTRA_TRACK_INDEX, 0);
        long position = intent.getLongExtra(EXTRA_PLAYBACK_POSITION, 0);
        boolean wasPlaying = intent.getBooleanExtra(EXTRA_WAS_PLAYING, false);

        Log.d(TAG, "Resuming from state - URL: " + url + ", track: " + trackIndex + ", position: " + position);

        // Build audiobook from intent
        Audiobook audiobook = buildAudiobookFromIntent(intent);

        if (audiobook != null && audiobook.getAudioUrls() != null && !audiobook.getAudioUrls().isEmpty()) {
            loadAudiobook(audiobook, trackIndex);
            if (position > 0) {
                player.seekTo(trackIndex, position);
            }
            if (wasPlaying) {
                player.play();
            }
        }
    }

    /**
     * Handle resume from notification click
     */
    private void handleResumeFromNotification(Intent intent) {
        String url = intent.getStringExtra(EXTRA_AUDIOBOOK_URL);
        int trackIndex = intent.getIntExtra("track_index", 0);
        long position = intent.getLongExtra("playback_position", 0);
        boolean wasPlaying = intent.getBooleanExtra("was_playing", false);

        Log.d(TAG, "Resuming from notification - URL: " + url + ", track: " + trackIndex + ", position: " + position);

        // If we have currentAudiobook and it matches, resume
        if (currentAudiobook != null && url != null && url.equals(currentAudiobook.getUrl())) {
            if (position > 0) {
                player.seekTo(trackIndex, position);
            }
            if (wasPlaying) {
                player.play();
            }
        } else {
            // Need to rebuild audiobook from currentAudiobook if available
            if (currentAudiobook != null) {
                loadAudiobook(currentAudiobook, trackIndex);
                if (position > 0) {
                    player.seekTo(trackIndex, position);
                }
                if (wasPlaying) {
                    player.play();
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service onUnbind");
        // Keep service running when activity unbinds for background playback
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");

        // Stop position updates
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable);

        // Release resources
        if (notificationHelper != null) {
            notificationHelper.release();
        }

        if (mediaSession != null) {
            mediaSession.release();
        }

        if (player != null) {
            player.release();
        }

        playbackStateListener = null;
        super.onDestroy();
    }

    /**
     * Starts the service as a foreground service with notification
     */
    private void startForegroundService() {
        try {
            // Build a notification and start foreground
            Notification notification = buildNotification();
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
    }

    /**
     * Builds the notification for foreground service
     */
    private Notification buildNotification() {
        try {
            notificationHelper.getPlayerNotificationManager(player, mediaSession);
            // The notification is automatically managed by PlayerNotificationManager
            return null; // PlayerNotificationManager handles notification internally
        } catch (Exception e) {
            Log.e(TAG, "Error building notification", e);
            return null;
        }
    }

    /**
     * Handles action intents
     */
    private void handleAction(String action, Intent intent) {
        Log.d(TAG, "Handling action: " + action);

        switch (action) {
            case ACTION_PLAY:
                player.play();
                break;
            case ACTION_PAUSE:
                player.pause();
                break;
            case ACTION_STOP:
                stopPlayback();
                break;
            case ACTION_NEXT:
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                }
                break;
            case ACTION_PREVIOUS:
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                } else {
                    player.seekTo(0);
                }
                break;
        }
    }

    // Public methods for activity to control playback

    public ExoPlayer getPlayer() {
        return player;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }

    public Audiobook getCurrentAudiobook() {
        return currentAudiobook;
    }

    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    /**
     * Sets the current audiobook and updates notification
     */
    public void setAudiobook(Audiobook audiobook) {
        this.currentAudiobook = audiobook;
        this.currentTrackIndex = 0;

        if (notificationHelper != null) {
            notificationHelper.updateAudiobook(audiobook);
        }
    }

    /**
     * Loads an audiobook and plays a specific track
     */
    public void loadAudiobook(Audiobook audiobook, int trackIndex) {
        this.currentAudiobook = audiobook;
        this.currentTrackIndex = trackIndex;

        if (audiobook != null && audiobook.getAudioUrls() != null && !audiobook.getAudioUrls().isEmpty()) {
            // Clear existing media items
            player.clearMediaItems();

            // Create media items for all tracks
            for (int i = 0; i < audiobook.getAudioUrls().size(); i++) {
                String audioUrl = audiobook.getAudioUrls().get(i);
                String trackName = audiobook.getTrackNames() != null && i < audiobook.getTrackNames().size()
                        ? audiobook.getTrackNames().get(i)
                        : "Track " + (i + 1);

                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(Uri.parse(audioUrl))
                        .setMediaId(audioUrl)
                        .setTag(i)
                        .build();
                player.addMediaItem(mediaItem);
            }

            // Seek to the requested track
            if (trackIndex >= 0 && trackIndex < player.getMediaItemCount()) {
                player.seekTo(trackIndex, 0);
            }

            // Update notification
            if (notificationHelper != null) {
                notificationHelper.updateAudiobook(audiobook);
            }

            Log.d(TAG, "Loaded audiobook with " + audiobook.getAudioUrls().size() + " tracks");
        } else {
            Log.e(TAG, "Cannot load audiobook: null or empty audio URLs");
        }
    }

    /**
     * Loads an audiobook with all audio URLs and track names
     */
    public void loadAudiobookWithTracks(Audiobook audiobook, int trackIndex, List<String> audioUrls, List<String> trackNames) {
        this.currentAudiobook = audiobook;
        this.currentTrackIndex = trackIndex;

        if (audiobook != null && audioUrls != null && !audioUrls.isEmpty()) {
            // Clear existing media items
            player.clearMediaItems();

            // Create media items for all tracks
            for (int i = 0; i < audioUrls.size(); i++) {
                String audioUrl = audioUrls.get(i);
                String trackName = trackNames != null && i < trackNames.size()
                        ? trackNames.get(i)
                        : "Track " + (i + 1);

                MediaItem mediaItem = new MediaItem.Builder()
                        .setUri(Uri.parse(audioUrl))
                        .setMediaId(audioUrl)
                        .setTag(i)
                        .build();
                player.addMediaItem(mediaItem);
            }

            // Seek to the requested track
            if (trackIndex >= 0 && trackIndex < player.getMediaItemCount()) {
                player.seekTo(trackIndex, 0);
            }

            // Update notification
            if (notificationHelper != null) {
                notificationHelper.updateAudiobook(audiobook);
            }

            Log.d(TAG, "Loaded audiobook with " + audioUrls.size() + " tracks");
        }
    }

    /**
     * Plays a specific track by index
     */
    public void playTrack(int trackIndex) {
        Log.d(TAG, "playTrack called with index: " + trackIndex + ", mediaItemCount: " + player.getMediaItemCount());

        if (player != null) {
            if (player.getMediaItemCount() == 0) {
                Log.e(TAG, "No media items loaded, cannot play track");
                return;
            }

            if (trackIndex >= 0 && trackIndex < player.getMediaItemCount()) {
                player.seekTo(trackIndex, 0);
                player.play();
                currentTrackIndex = trackIndex;
            } else {
                Log.e(TAG, "Invalid track index: " + trackIndex);
            }
        } else {
            Log.e(TAG, "Player is null, cannot play track");
        }
    }

    /**
     * Plays the next track
     */
    public void playNextTrack() {
        if (player != null && player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
        }
    }

    /**
     * Plays the previous track
     */
    public void playPreviousTrack() {
        if (player != null) {
            if (player.getCurrentPosition() > 3000) {
                // If more than 3 seconds into the track, restart it
                player.seekTo(0);
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem();
            }
        }
    }

    /**
     * Sets the playback state listener
     */
    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.playbackStateListener = listener;
    }

    /**
     * Removes the playback state listener
     */
    public void removePlaybackStateListener() {
        this.playbackStateListener = null;
    }

    /**
     * Checks if playback is active
     */
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    /**
     * Gets the current media item count
     */
    public int getMediaItemCount() {
        return player != null ? player.getMediaItemCount() : 0;
    }

    /**
     * Checks if there's a next track
     */
    public boolean hasNextTrack() {
        return player != null && player.hasNextMediaItem();
    }

    /**
     * Checks if there's a previous track
     */
    public boolean hasPreviousTrack() {
        return player != null && player.hasPreviousMediaItem();
    }

    /**
     * Toggles play/pause
     */
    public void togglePlayPause() {
        Log.d(TAG, "togglePlayPause called, player=" + player + ", isPlaying=" + (player != null ? player.isPlaying() : "null"));

        if (player != null) {
            if (player.getMediaItemCount() == 0) {
                Log.e(TAG, "No media items loaded, cannot toggle play/pause");
                return;
            }

            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        } else {
            Log.e(TAG, "Player is null, cannot toggle play/pause");
        }
    }

    /**
     * Seeks to a specific position
     */
    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    /**
     * Gets the current playback position
     */
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    /**
     * Gets the total duration
     */
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    /**
     * Stops playback and terminates the service
     */
    public void stopPlayback() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }

        if (notificationHelper != null) {
            notificationHelper.dismissNotification();
        }

        stopForeground(true);
        stopSelf();
    }

    /**
     * Gets the notification helper for the service
     */
    public NotificationHelper getNotificationHelper() {
        return notificationHelper;
    }
}
