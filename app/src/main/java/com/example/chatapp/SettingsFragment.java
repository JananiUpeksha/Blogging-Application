package com.example.chatapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.chatapp.databinding.FragmentSettingsBinding;
import java.util.List;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private DatabaseHelper db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        refreshStatus();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnSyncNow.setOnClickListener(v -> syncPendingPosts());
        binding.rowClearAll.setOnClickListener(v -> confirmClear());
        binding.navChat.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    // SYNC FUNCTION - Uploads pending posts when online
    private void syncPendingPosts() {
        // Check if online
        if (!NetworkUtils.isOnline(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("No Internet Connection")
                    .setMessage("Cannot sync without internet.\n\nPlease connect to WiFi or mobile data and try again.")
                    .setPositiveButton("Settings", (d, w) -> {
                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        List<Message> pending = db.getPendingMessages();

        if (pending.isEmpty()) {
            Toast.makeText(requireContext(), "✅ All posts are synced!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        Toast.makeText(requireContext(), "Syncing " + pending.size() + " posts...", Toast.LENGTH_SHORT).show();

        // Simulate upload to server
        int syncedCount = 0;
        for (Message message : pending) {
            // In a real app, you would upload to your server here
            // Example: uploadToServer(message);

            // Mark as synced (is_pending = 0)
            db.updateSyncStatus(message.id, 0);
            syncedCount++;

            // Optional: Add small delay to simulate network request
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Toast.makeText(requireContext(), "✅ Synced " + syncedCount + " posts successfully!", Toast.LENGTH_LONG).show();
        refreshStatus();
    }

    private void refreshStatus() {
        int pendingCount = db.getPendingCount();

        if (pendingCount > 0) {
            binding.tvSyncStatus.setText(pendingCount + " posts pending sync");
            binding.tvSyncStatus.setTextColor(android.graphics.Color.RED);
            binding.btnSyncNow.setEnabled(true);

            // Show info about offline posts
            if (!NetworkUtils.isOnline(requireContext())) {
                binding.tvSyncStatus.setText(pendingCount + " posts pending (Connect to internet to sync)");
                binding.btnSyncNow.setText("Waiting for Internet");
                binding.btnSyncNow.setEnabled(false);
            } else {
                binding.btnSyncNow.setText("Sync Now");
                binding.btnSyncNow.setEnabled(true);
            }
        } else {
            binding.tvSyncStatus.setText("✅ All posts synced");
            binding.tvSyncStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            binding.btnSyncNow.setText("Sync Now");
            binding.btnSyncNow.setEnabled(true);
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data?")
                .setMessage("⚠️ Warning: This will delete ALL posts permanently!\n\nThis action cannot be undone.")
                .setPositiveButton("Delete Everything", (d, w) -> {
                    db.clearAll();
                    refreshStatus();
                    Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}