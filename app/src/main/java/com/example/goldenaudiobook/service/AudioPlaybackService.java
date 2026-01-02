package com.example.goldenaudiobook.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
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

    // Notification ID
    private static final int NOTIFICATION_ID = 1001;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private NotificationHelper notificationHelper;
    private final IBinder binder = new AudioServiceBinder();
    private Audiobook currentAudiobook;
    private int currentTrackIndex = 0;
    private PlaybackStateListener playbackStateListener;

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
            // Handle audiobook data if provided
            if (intent.hasExtra(EXTRA_AUDIOBOOK_URL)) {
                String url = intent.getStringExtra(EXTRA_AUDIOBOOK_URL);
                String title = intent.getStringExtra(EXTRA_AUDIOBOOK_TITLE);
                
                if (currentAudiobook == null || !url.equals(currentAudiobook.getUrl())) {
                    Log.d(TAG, "Loading new audiobook: " + title);
                }
            }

            // Handle actions
            if (intent.getAction() != null) {
                handleAction(intent.getAction(), intent);
            }
        }

        return START_STICKY; // Service will be restarted if killed
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
        }
    }

    /**
     * Plays a specific track by index
     */
    public void playTrack(int trackIndex) {
        if (player != null && trackIndex >= 0 && trackIndex < player.getMediaItemCount()) {
            player.seekTo(trackIndex, 0);
            player.play();
            currentTrackIndex = trackIndex;
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
     * Toggles play/pause
     */
    public void togglePlayPause() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
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
