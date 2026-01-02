package com.example.goldenaudiobook.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.PlayerNotificationManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.ui.AudiobookDetailActivity;

/**
 * Helper class for managing audio playback notification using PlayerNotificationManager
 */
@UnstableApi
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "audiobook_playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;
    private PlayerNotificationManager playerNotificationManager;
    private Audiobook currentAudiobook;
    private Bitmap currentCoverBitmap;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Creates the notification channel for audio playback
     * Required for Android 8.0 (API 26) and above
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audiobook Playback",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Controls for audiobook playback");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Don't play sound or vibrate for notification updates
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.canBypassDnd();
            channel.setBypassDnd(true);

            // Create the notification channel

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Initializes and returns the PlayerNotificationManager
     */
    public PlayerNotificationManager getPlayerNotificationManager(
            ExoPlayer player,
            MediaSession mediaSession) {

        if (playerNotificationManager == null) {
            // Create media descriptor adapter
            PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter =
                    new PlayerNotificationManager.MediaDescriptionAdapter() {
                        @Override
                        public CharSequence getCurrentContentTitle(Player player) {
                            return currentAudiobook != null ?
                                    currentAudiobook.getDisplayTitle() : "Audiobook";
                        }

                        @Override
                        public CharSequence getCurrentContentText(Player player) {
                            return currentAudiobook != null ?
                                    currentAudiobook.getDisplayAuthor() : "Playing";
                        }

                        @Nullable
                        @Override
                        public Bitmap getCurrentLargeIcon(Player player,
                                                          PlayerNotificationManager.BitmapCallback callback) {
                            // Load image asynchronously if needed
                            if (currentCoverBitmap != null) {
                                return currentCoverBitmap;
                            }

                            if (currentAudiobook != null &&
                                    currentAudiobook.getImageUrl() != null &&
                                    !currentAudiobook.getImageUrl().isEmpty()) {
                                loadCoverImageAsync(currentAudiobook.getImageUrl(), callback);
                            }

                            return null;
                        }

                        @Nullable
                        @Override
                        public PendingIntent createCurrentContentIntent(Player player) {
                            Intent intent = new Intent(context, AudiobookDetailActivity.class);

                            // Add NEW_TASK flag to allow launching from notification
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            // Pass the audiobook URL and title to open the correct audiobook
                            if (currentAudiobook != null) {
                                if (currentAudiobook.getUrl() != null && !currentAudiobook.getUrl().isEmpty()) {
                                    intent.putExtra("audiobook_url", currentAudiobook.getUrl());
                                }
                                if (currentAudiobook.getTitle() != null && !currentAudiobook.getTitle().isEmpty()) {
                                    intent.putExtra("audiobook_title", currentAudiobook.getTitle());
                                }
                            }

                            // Pass playback state for resume functionality
                            intent.putExtra("resume_from_notification", true);

                            // Get current playback info from the service via the player
                            int currentTrackIndex = player.getCurrentMediaItemIndex();
                            long currentPosition = player.getCurrentPosition();
                            boolean isPlaying = player.isPlaying();

                            intent.putExtra("track_index", currentTrackIndex);
                            intent.putExtra("playback_position", currentPosition);
                            intent.putExtra("was_playing", isPlaying);

                            // Use FLAG_UPDATE_CURRENT to update intent with latest audiobook info
                            // Use FLAG_IMMUTABLE for Android 12+ compatibility
                            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                flags |= PendingIntent.FLAG_IMMUTABLE;
                            }

                            // Use a unique request code based on audiobook URL to ensure
                            // each audiobook has its own PendingIntent
                            int requestCode = NOTIFICATION_ID;
                            if (currentAudiobook != null && currentAudiobook.getUrl() != null) {
                                requestCode = currentAudiobook.getUrl().hashCode();
                            }

                            return PendingIntent.getActivity(
                                    context,
                                    requestCode,
                                    intent,
                                    flags
                            );
                        }
                    };

            // Build PlayerNotificationManager
            playerNotificationManager = new PlayerNotificationManager.Builder(
                    context,
                    NOTIFICATION_ID,
                    CHANNEL_ID
            )
                    .setMediaDescriptionAdapter(descriptionAdapter)
                    .setSmallIconResourceId(R.drawable.ic_notification)
                    .setChannelNameResourceId(R.string.notification_channel_name)
                    .setChannelDescriptionResourceId(R.string.notification_channel_description)
                    .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                        @Override
                        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                            Log.d(TAG, "Notification cancelled, dismissedByUser: " + dismissedByUser);
                            // Stop the service or handle cleanup if needed
                        }

                        @Override
                        public void onNotificationPosted(int notificationId,
                                                         Notification notification, boolean ongoing) {
                            Log.d(TAG, "Notification posted, ongoing: " + ongoing);
                            // Handle notification posted if needed
                        }
                    })
                    .build();

            // Configure notification settings
            playerNotificationManager.setMediaSessionToken(mediaSession.getSessionCompatToken());
            playerNotificationManager.setUsePreviousAction(true);
            playerNotificationManager.setUseNextAction(true);
            playerNotificationManager.setUsePlayPauseActions(true);
            playerNotificationManager.setPriority(NotificationCompat.PRIORITY_LOW);
            playerNotificationManager.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Set the player
            playerNotificationManager.setPlayer(player);

            Log.d(TAG, "PlayerNotificationManager initialized");
        }

        return playerNotificationManager;
    }

    /**
     * Updates the audiobook information for the notification
     */
    public void updateAudiobook(Audiobook audiobook) {
        this.currentAudiobook = audiobook;
        this.currentCoverBitmap = null; // Clear cached bitmap

        // Invalidate the notification to refresh with new info and update PendingIntent
        if (playerNotificationManager != null) {
            playerNotificationManager.invalidate();
        }
    }

    /**
     * Updates the cover bitmap for the notification
     */
    public void updateCoverBitmap(Bitmap bitmap) {
        this.currentCoverBitmap = bitmap;

        // Invalidate the notification to refresh with new image
        if (playerNotificationManager != null) {
            playerNotificationManager.invalidate();
        }
    }

    /**
     * Loads cover image asynchronously using Glide
     */
    private void loadCoverImageAsync(String imageUrl,
                                     PlayerNotificationManager.BitmapCallback callback) {
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap,
                                                @Nullable Transition<? super Bitmap> transition) {
                        currentCoverBitmap = bitmap;
                        callback.onBitmap(bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle cleanup if needed
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "Failed to load cover image: " + imageUrl);
                    }
                });
    }

    /**
     * Shows the notification
     */
    public void showNotification(ExoPlayer player, MediaSession mediaSession,
                                 Audiobook audiobook) {
        updateAudiobook(audiobook);
        getPlayerNotificationManager(player, mediaSession);
    }

    /**
     * Shows notification with simplified parameters
     */
    public void showNotification(ExoPlayer player, MediaSession mediaSession,
                                 String title, String artist) {
        Audiobook tempAudiobook = new Audiobook();
        tempAudiobook.setTitle(title);
        tempAudiobook.setAuthor(artist);
        showNotification(player, mediaSession, tempAudiobook);
    }

    /**
     * Dismisses the playback notification
     */
    public void dismissNotification() {
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
        }
    }

    /**
     * Releases resources
     */
    public void release() {
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
            playerNotificationManager = null;
        }
        currentAudiobook = null;
        currentCoverBitmap = null;
    }

    /**
     * Gets the channel ID for notifications
     */
    public static String getChannelId() {
        return CHANNEL_ID;
    }

    /**
     * Gets the notification ID
     */
    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    /**
     * Gets the current audiobook being played
     */
    public Audiobook getCurrentAudiobook() {
        return currentAudiobook;
    }
}