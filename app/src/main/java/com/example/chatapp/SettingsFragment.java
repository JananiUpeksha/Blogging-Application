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
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnSyncNow.setOnClickListener(v -> Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show());

        binding.rowExport.setOnClickListener(v -> Toast.makeText(requireContext(), "Exporting...", Toast.LENGTH_SHORT).show());
        binding.rowImport.setOnClickListener(v -> Toast.makeText(requireContext(), "Importing...", Toast.LENGTH_SHORT).show());
        binding.rowClearAll.setOnClickListener(v -> confirmClearAll());

        binding.btnLogOut.setOnClickListener(v -> Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show());

        // Bottom nav logic
        binding.navChat.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All History?")
                .setMessage("This will permanently delete all saved posts.")
                .setPositiveButton("Clear", (d, w) -> {
                    db.clearAll();
                    Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}