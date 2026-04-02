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

/**
 * Settings screen matching the UI prototype:
 *  - Profile section (email, sync status)
 *  - Database Management (Export / Import / Clear All)
 *  - Appearance (Dark mode toggle, theme color)
 *  - Social Accounts (connect status)
 *  - Backup Options
 *  - About (version, privacy, terms)
 *  - Log Out
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private DatabaseHelper db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        setupClickListeners();
        updateSyncStatus();
    }

    private void updateSyncStatus() {
        int pending = db.getPendingCount();
        if (pending > 0) {
            binding.tvSyncStatus.setText("Free Account • " + pending + " pending sync");
        } else {
            binding.tvSyncStatus.setText("Free Account • Sync Active");
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        // DB Management
        binding.rowExport.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Export feature – integrate your export logic here",
                        Toast.LENGTH_SHORT).show());

        binding.rowImport.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Import feature – integrate your import logic here",
                        Toast.LENGTH_SHORT).show());

        binding.rowClearAll.setOnClickListener(v -> confirmClearAll());

        // Sync Now
        binding.btnSyncNow.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Syncing…", Toast.LENGTH_SHORT).show();
            // TODO: trigger real sync service
        });

        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    checked
                    ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                    : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        // About rows
        binding.rowPrivacy.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show());
        binding.rowTerms.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Terms of Service", Toast.LENGTH_SHORT).show());

        // Log out
        binding.btnLogOut.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Log out – implement your auth logout here",
                        Toast.LENGTH_SHORT).show());

        // Bottom nav
        binding.navChat.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All History?")
                .setMessage("This will permanently delete all saved messages. Cannot be undone.")
                .setPositiveButton("Clear", (d, w) -> {
                    db.clearAll();
                    Toast.makeText(requireContext(),
                            "All messages cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
