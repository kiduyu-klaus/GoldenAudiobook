package com.example.goldenaudiobook.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.databinding.ActivitySplashBinding;
import com.example.goldenaudiobook.ui.MainActivity;

/**
 * Beautiful modern splash activity with golden-themed animations
 * Serves as the elegant entry point to the Golden Audiobook experience
 */
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the splash screen fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setNavigationBarColor(
                ContextCompat.getColor(this, R.color.splash_background));

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupAnimation();
    }

    private void setupAnimation() {
        // Set up transition listener to detect when animation completes
        binding.motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
                // Animation started - no action needed
            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
                // Animation in progress - no action needed
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                // Animation finished - navigate to main activity
                if (!isNavigating) {
                    isNavigating = true;
                    navigateToMain();
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
                // Not used in our implementation
            }
        });

        // Fallback navigation after 3 seconds even if animation somehow fails
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isNavigating) {
                isNavigating = true;
                navigateToMain();
            }
        }, 10000);
    }

    @OptIn(markerClass = UnstableApi.class) private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);

        // Apply smooth fade transition between activities
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Finish this activity so back button doesn't return here
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during splash - user must see the beautiful animation
        // Or optionally allow it with a small delay
        if (!isNavigating) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isNavigating) {
                    isNavigating = true;
                    super.onBackPressed();
                }
            }, 500);
        }
    }
}
