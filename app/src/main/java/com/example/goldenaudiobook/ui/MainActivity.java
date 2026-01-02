package com.example.goldenaudiobook.ui;

import static com.example.goldenaudiobook.util.Utils.getDialogPowerMenu;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.databinding.ActivityMainBinding;
import com.example.goldenaudiobook.service.AudioPlaybackService;
import com.example.goldenaudiobook.util.Utils;
import com.example.goldenaudiobook.viewmodel.FloatingPlayerViewModel;
import com.google.android.material.navigation.NavigationView;
import com.skydoves.powermenu.PowerMenu;

/**
 * Main Activity hosting the navigation graph and drawer
 * Also hosts the FloatingPlayerFragment for persistent audio playback
 */
@UnstableApi public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private PowerMenu dialogMenu;
    private FloatingPlayerViewModel floatingPlayerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i(TAG, "onCreate: "+getIntent().getStringExtra("authorUrl"));

        if (getIntent() != null) {
            Log.i("AuthorAllBooksFragment", "head to AuthorAllBooksFragment: ");
            String authorUrl = getIntent().getStringExtra("authorUrl");
            String authorName = getIntent().getStringExtra("authorName");

            if (authorUrl != null && authorName != null) {
                Bundle args = new Bundle();
                args.putString("authorUrl", authorUrl);
                args.putString("authorName", authorName);
                navController.navigate(R.id.authorAllBooksFragment, args);
            }

            // Clear the extra to prevent re-navigation
            getIntent().removeExtra("navigate_to_author");
        }

        setupToolbar();
        setupNavigation();
        setupFloatingPlayer();
        initializeDialogMenu();

        // Check if notifications are enabled
        if (Utils.areNotificationsEnabled(this)) {
            // Notifications are enabled
        } else {
            // Show dialog or alert to enable notifications
            Utils.openNotificationSettings(this);
        }
        // Handle navigation to author books from AudiobookDetailActivity
        //handleAuthorNavigation();

        View layout = binding.getRoot();
        dialogMenu.showAtCenter(layout);
    }
    /**
     * Handle navigation to AuthorAllBooksFragment from AudiobookDetailActivity
     */
    private void handleAuthorNavigation() {
        if (getIntent() != null && getIntent().hasExtra("navigate_to_author")) {
            Log.i("AuthorAllBooksFragment", "head to AuthorAllBooksFragment: ");
            String authorUrl = getIntent().getStringExtra("authorUrl");
            String authorName = getIntent().getStringExtra("authorName");

            if (authorUrl != null && authorName != null) {
                Bundle args = new Bundle();
                args.putString("authorUrl", authorUrl);
                args.putString("authorName", authorName);
                navController.navigate(R.id.authorAllBooksFragment, args);
            }

            // Clear the extra to prevent re-navigation
            getIntent().removeExtra("navigate_to_author");
        }
    }
    private void initializeDialogMenu() {
        Log.i("initializeDialogMenu", "initializeDialogMenu: ");
        dialogMenu = getDialogPowerMenu(this, this);
        View footerView = dialogMenu.getFooterView();
        TextView textView_yes = footerView.findViewById(R.id.textView_yes);
        textView_yes.setOnClickListener(
                view -> {
                    Toast.makeText(getBaseContext(), "Yes", Toast.LENGTH_SHORT).show();
                    dialogMenu.dismiss();
                });
        TextView textView_no = footerView.findViewById(R.id.textView_no);
        textView_no.setOnClickListener(
                view -> {
                    Toast.makeText(getBaseContext(), "No", Toast.LENGTH_SHORT).show();
                    dialogMenu.dismiss();
                });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        // Set navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.primary));
            getWindow().setNavigationBarColor(
                    ContextCompat.getColor(this, R.color.primary)
            );

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            navController.navigate(R.id.searchFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupNavigation() {
        // Setup NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Setup AppBarConfiguration with top-level destinations
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment,
                    R.id.searchFragment
            ).setOpenableLayout(binding.drawerLayout).build();

            // Setup toolbar with nav controller
            NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

            // Setup navigation drawer
            binding.navView.setNavigationItemSelectedListener(this);

            // Sync nav controller with nav view
            //NavigationUI.setupWithNavController(binding.navView, navController);
        }
    }

    @OptIn(markerClass = UnstableApi.class) private void setupFloatingPlayer() {
        // Initialize ViewModel
        floatingPlayerViewModel = new ViewModelProvider(this,
                new FloatingPlayerViewModel.Factory(getApplication()))
                .get(FloatingPlayerViewModel.class);

        // Add FloatingPlayerFragment if not already added
        Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.floating_player_container);
        if (existingFragment == null) {
            FloatingPlayerFragment floatingPlayerFragment = new FloatingPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.floating_player_container, floatingPlayerFragment)
                    .commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            navController.navigate(R.id.homeFragment);
        } else if (itemId == R.id.nav_search) {
            navController.navigate(R.id.searchFragment);
        } else if (itemId == R.id.nav_categories) {
            navController.navigate(R.id.categoriesFragment);
        } else if (itemId == R.id.nav_bestsellers) {
            navigateToCategory("https://goldenaudiobook.net/category/bestsellers/", "Bestsellers");
        } else if (itemId == R.id.nav_action) {
            navigateToCategory("https://goldenaudiobook.net/category/action/", "Action");
        } else if (itemId == R.id.nav_fantasy) {
            navigateToCategory("https://goldenaudiobook.net/category/audio-fantasy/", "Fantasy");
        } else if (itemId == R.id.nav_romance) {
            navigateToCategory("https://goldenaudiobook.net/category/audiobooks-romance/", "Romance");
        } else if (itemId == R.id.nav_mystery) {
            navigateToCategory("https://goldenaudiobook.net/category/mystery/", "Mystery");
        } else if (itemId == R.id.nav_thriller) {
            navigateToCategory("https://goldenaudiobook.net/category/thriller/", "Thriller");
        } else if (itemId == R.id.nav_scifi) {
            navigateToCategory("https://goldenaudiobook.net/category/science-fiction-audiobooks/", "Sci-Fi");
        } else if (itemId == R.id.nav_classic) {
            navigateToCategory("https://goldenaudiobook.net/category/classic/", "Classic");
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void navigateToCategory(String url, String name) {
        Bundle args = new Bundle();
        args.putString("categoryUrl", url);
        args.putString("categoryName", name);
        navController.navigate(R.id.categoryAudiobooksFragment, args);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else if (!dialogMenu.isShowing()) {
            dialogMenu.showAtCenter(binding.getRoot());

        } else {
            super.onBackPressed();
        }
    }

    @OptIn(markerClass = UnstableApi.class) @Override
    protected void onPause() {
        super.onPause();
        // Save playback state when app goes to background
        if (floatingPlayerViewModel != null) {
            floatingPlayerViewModel.saveState();
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Important! Update the intent
        handleAuthorNavigation();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
