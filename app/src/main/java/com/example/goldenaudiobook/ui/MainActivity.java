package com.example.goldenaudiobook.ui;

import static com.example.goldenaudiobook.utils.Utils.getDialogPowerMenu;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.databinding.ActivityMainBinding;
import com.example.goldenaudiobook.model.NavItem;
import com.example.goldenaudiobook.utils.Utils;
import com.google.android.material.navigation.NavigationView;
import com.skydoves.powermenu.PowerMenu;

import java.util.List;

/**
 * Main Activity hosting the navigation graph and drawer
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private PowerMenu dialogMenu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupNavigation();

        initializeDialogMenu();

        View layout = binding.getRoot();
        dialogMenu.showAtCenter(layout);
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
    }

    private void setupNavigation() {
        // Setup NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Setup AppBarConfiguration with top-level destinations
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment
            ).setOpenableLayout(binding.drawerLayout).build();

            // Setup toolbar with nav controller
            NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

            // Setup navigation drawer
            binding.navView.setNavigationItemSelectedListener(this);

            // Sync nav controller with nav view
            //NavigationUI.setupWithNavController(binding.navView, navController);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            navController.navigate(R.id.homeFragment);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
