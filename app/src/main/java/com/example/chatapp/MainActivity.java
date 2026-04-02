package com.example.chatapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chatapp.databinding.ActivityMainBinding;

/**
 * Single-Activity host for all fragments.
 * The ActionBar is hidden; each fragment manages its own top-bar via ViewBinding.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide the default ActionBar – fragments use their own top bars
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    /** Allow fragments to get the NavController easily. */
    public NavController getNavController() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHost == null) {
            throw new IllegalStateException("NavHostFragment not found");
        }
        return navHost.getNavController();
    }
}
