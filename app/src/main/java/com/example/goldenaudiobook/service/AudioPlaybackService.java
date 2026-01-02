package com.example.goldenaudiobook.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.util.NotificationHelper;

/**
 * Foreground service for background audio playback
 */
@UnstableApi
public class AudioPlaybackService extends Service {

    private static final String TAG = "AudioPlaybackService";

    public static final String ACTION_PLAY = "com.example.goldenaudiobook.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.goldenaudiobook.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.goldenaudiobook.ACTION_STOP";

    private ExoPlayer player;
    private MediaSession mediaSession;
    private NotificationHelper notificationHelper;
    private final IBinder binder = new AudioServiceBinder();
    private Audiobook currentAudiobook;
    private PlaybackStateListener playbackStateListener;

    /**
     * Interface for notifying activity about playback state changes
     */
    public interface PlaybackStateListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onMediaItemTransition(int currentIndex);
        void onPlayerError(String error);
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
        player = new ExoPlayer.Builder(this).build();

        // Initialize MediaSession
        mediaSession = new MediaSession.Builder(this, player)
                .setId("audiobook_playback_service")
                .build();

        // Set up player listeners
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Optionally handle track completion
                    Log.d(TAG, "Playback ended");
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: " + isPlaying);

                if (isPlaying) {
                    // Start foreground service with notification
                    startForegroundService();
                }

                // Notify listener
                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(isPlaying);
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Log.d(TAG, "Media item transition: " + reason);

                // Notify listener (you'll need to track current index)
                if (playbackStateListener != null) {
                    // You might want to pass the actual index here
                    playbackStateListener.onMediaItemTransition(player.getCurrentMediaItemIndex());
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
        Log.d(TAG, "onStartCommand");

        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
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
        // Don't stop the service when activity unbinds
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

        super.onDestroy();
    }

    /**
     * Starts the service as a foreground service with notification
     */
    private void startForegroundService() {
        try {
            // PlayerNotificationManager automatically handles foreground service
            // when player starts playing. We just need to ensure it's initialized.
            notificationHelper.getPlayerNotificationManager(player, mediaSession)
                    .setUseChronometer(true);
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
    }

    /**
     * Handles action intents
     */
    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                player.play();
                break;
            case ACTION_PAUSE:
                player.pause();
                break;
            case ACTION_STOP:
                player.stop();
                stopForeground(true);
                stopSelf();
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

    public void setAudiobook(Audiobook audiobook) {
        this.currentAudiobook = audiobook;
        if (notificationHelper != null) {
            notificationHelper.updateAudiobook(audiobook);
        }
    }

    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.playbackStateListener = listener;
    }

    public void removePlaybackStateListener() {
        this.playbackStateListener = null;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void play() {
        if (player != null) {
            player.play();
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        stopForeground(true);
        stopSelf();
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }
}