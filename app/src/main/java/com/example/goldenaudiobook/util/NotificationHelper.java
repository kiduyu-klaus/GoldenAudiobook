package com.example.goldenaudiobook.util;

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
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.ui.AudiobookDetailActivity;

/**
 * Helper class for managing audio playback notification channels and notifications
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "audiobook_playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_PLAY = "com.example.goldenaudiobook.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.goldenaudiobook.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.example.goldenaudiobook.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.goldenaudiobook.ACTION_PREVIOUS";
    public static final String ACTION_STOP = "com.example.goldenaudiobook.ACTION_STOP";

    Context context;

    public NotificationHelper(Context context) {
        this.context = context;
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
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            // Don't play sound or vibrate for notification updates
            channel.setSound(null, null);
            channel.enableVibration(false);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Builds and shows the playback notification with image
     */
    public static android.app.Notification buildNotification(
            Context context,
            ExoPlayer player,
            MediaSession mediaSession,
            Audiobook audiobook,
            Bitmap coverBitmap) {

        // Create content intent (tap to open app)
        Intent contentIntent = new Intent(context, AudiobookDetailActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification using MediaStyle
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(audiobook != null ? audiobook.getDisplayTitle() : "Audiobook")
                .setContentText(audiobook != null ? audiobook.getDisplayAuthor() : "Playing")
                .setSubText("Now Playing")
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(player.isPlaying())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                        .setShowActionsInCompactView(0, 1, 2));

        // Set large icon (cover image) if available
        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap);
        }

        // Add play/pause action
        if (player.isPlaying()) {
            builder.addAction(
                    R.drawable.ic_pause,
                    "Pause",
                    createActionPendingIntent(context, ACTION_PAUSE, player)
            );
        } else {
            builder.addAction(
                    R.drawable.ic_play,
                    "Play",
                    createActionPendingIntent(context, ACTION_PLAY, player)
            );
        }

        // Add previous track action
        builder.addAction(
                R.drawable.ic_previous,
                "Previous",
                createActionPendingIntent(context, ACTION_PREVIOUS, player)
        );

        // Add next track action
        builder.addAction(
                R.drawable.ic_next,
                "Next",
                createActionPendingIntent(context, ACTION_NEXT, player)
        );

        return builder.build();
    }



    /**
     * Shows a notification for media playback with simplified parameters
     */
    public static void showNotification(
            Context context,
            ExoPlayer player,
            MediaSession mediaSession,
            String title,
            String artist,
            boolean isPlaying) {

        if (context == null) return;

        // Create a simple Audiobook-like object for the notification
        Audiobook tempAudiobook = new Audiobook();
        tempAudiobook.setTitle(title);
        tempAudiobook.setAuthor(artist);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            android.app.Notification notification = buildNotification(
                    context, player, mediaSession, tempAudiobook, null);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Shows a notification for media playback with audiobook image
     */
    public static void showNotification(
            Context context,
            ExoPlayer player,
            MediaSession mediaSession,
            Audiobook audiobook,
            boolean isPlaying) {

        if (context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            android.app.Notification notification = buildNotificationWithImage(
                    context, player, mediaSession, audiobook);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Builds notification with audiobook cover image
     */
    private static android.app.Notification buildNotificationWithImage(
            Context context,
            ExoPlayer player,
            MediaSession mediaSession,
            Audiobook audiobook) {

        // Create content intent (tap to open app)
        Intent contentIntent = new Intent(context, AudiobookDetailActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification using MediaStyle
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(audiobook != null ? audiobook.getDisplayTitle() : "Audiobook")
                .setContentText(audiobook != null ? audiobook.getDisplayAuthor() : "Playing")
                .setSubText("Now Playing")
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(player.isPlaying())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT);

        // Add previous track action (index 0)
        builder.addAction(
                R.drawable.ic_previous,
                "Previous",
                createActionPendingIntent(context, ACTION_PREVIOUS, player)
        );

        // Add play/pause action (index 1)
        if (player.isPlaying()) {
            builder.addAction(
                    R.drawable.ic_pause,
                    "Pause",
                    createActionPendingIntent(context, ACTION_PAUSE, player)
            );
        } else {
            builder.addAction(
                    R.drawable.ic_play,
                    "Play",
                    createActionPendingIntent(context, ACTION_PLAY, player)
            );
        }

        // Add next track action (index 2)
        builder.addAction(
                R.drawable.ic_next,
                "Next",
                createActionPendingIntent(context, ACTION_NEXT, player)
        );

        // Apply MediaStyle AFTER adding all actions
        builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0, 1, 2)); // Show all three buttons

        // Load and set the audiobook cover image asynchronously
        if (audiobook != null && audiobook.getImageUrl() != null && !audiobook.getImageUrl().isEmpty()) {
            try {
                // Use Glide to load the image in the background and set it as large icon
                new Thread(() -> {
                    try {
                        android.graphics.Bitmap bitmap = com.bumptech.glide.Glide.with(context)
                                .asBitmap()
                                .load(audiobook.getImageUrl())
                                .submit()
                                .get();

                        builder.setLargeIcon(bitmap);

                        // Update notification with image
                        NotificationManager notificationManager =
                                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.notify(NOTIFICATION_ID, builder.build());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return builder.build();
    }




    /**
     * Dismisses the playback notification
     */
    public static void dismissNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    /**
     * Creates a PendingIntent for notification action buttons
     */
    private static PendingIntent createActionPendingIntent(
            Context context,
            String action,
            ExoPlayer player) {

        Intent intent = new Intent(context, AudiobookDetailActivity.class);
        intent.setAction(action);

        return PendingIntent.getActivity(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Processes notification action intents
     */
    public static boolean handleActionIntent(Context context, String action, ExoPlayer player) {
        if (player == null) return false;

        switch (action) {
            case ACTION_PLAY:
                player.play();
                return true;

            case ACTION_PAUSE:
                player.pause();
                return true;

            case ACTION_NEXT:
                // Navigate to next track
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                }
                return true;

            case ACTION_PREVIOUS:
                // Navigate to previous track or rewind
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                } else {
                    player.seekTo(0);
                }
                return true;

            case ACTION_STOP:
                player.stop();
                player.clearMediaItems();
                return true;

            default:
                return false;
        }
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
}