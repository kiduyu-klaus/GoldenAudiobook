package com.example.goldenaudiobook.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.goldenaudiobook.R;
import com.skydoves.powermenu.CustomPowerMenu;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

public class Utils {
    CustomPowerMenu customPowerMenu;
    Context ctx;

    public Utils(Context ctx) {
        this.ctx = ctx;
    }

    public static PowerMenu getDialogPowerMenu(Context context, LifecycleOwner lifecycleOwner) {
        return new PowerMenu.Builder(context)
                .setHeaderView(R.layout.layout_dialog_header)
                .setFooterView(R.layout.layout_dialog_footer)
                .addItem(new PowerMenuItem("Do you want to exit App", false))
                .setLifecycleOwner(lifecycleOwner)
                .setAnimation(MenuAnimation.SHOW_UP_CENTER)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setPadding(14)
                .setWidth(600)
                .setSelectedEffect(false)
                .build();
    }

    /**
     * Check if the app has notification permissions
     * @param context Application context
     * @return true if notifications are enabled, false otherwise
     */
    public static boolean areNotificationsEnabled(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        return notificationManager.areNotificationsEnabled();
    }

    /**
     * Check if notification channel is enabled (API 26+)
     * @param context Application context
     * @param channelId The notification channel ID to check
     * @return true if channel is enabled, false otherwise
     */
    public static boolean isNotificationChannelEnabled(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                android.app.NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
                if (channel != null) {
                    return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
                }
            }
            return false;
        }
        // For pre-Oreo devices, just check if notifications are enabled
        return areNotificationsEnabled(context);
    }

    /**
     * Open notification settings for the app
     * @param context Application context
     */
    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Request notification permission (Android 13+)
     * Call this from an Activity
     * @param context Activity context
     */
    public static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context instanceof androidx.activity.ComponentActivity) {
                androidx.activity.ComponentActivity activity = (androidx.activity.ComponentActivity) context;
                activity.registerForActivityResult(
                        new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                // Permission granted
                            } else {
                                // Permission denied
                                openNotificationSettings(context);
                            }
                        }
                ).launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
