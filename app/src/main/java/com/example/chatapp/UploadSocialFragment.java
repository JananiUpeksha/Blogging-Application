package com.example.chatapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.example.chatapp.databinding.FragmentUploadSocialBinding;

public class UploadSocialFragment extends Fragment {

    private FragmentUploadSocialBinding binding;
    private DatabaseHelper db;
    private Message message;
    private String selectedPlatform = "Twitter";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadSocialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        if (getArguments() != null) {
            long messageId = getArguments().getLong("messageId", -1);
            if (messageId != -1) {
                message = db.getMessageById(messageId);
            }
        }

        populatePreview();
        setupPlatformTiles();
        setupClickListeners();
        updateNetworkStatus();
    }

    private void populatePreview() {
        if (message == null) return;
        binding.tvPreviewTitle.setText(message.title);
        String body = (message.body != null) ? message.body : "";
        String preview = body.substring(0, Math.min(body.length(), 140));
        binding.tvPreviewBody.setText(preview);
        binding.tvCharCount.setText(preview.length() + "/280");
    }

    private void setupPlatformTiles() {
        binding.tileBlogger.setOnClickListener(v -> selectPlatform("Blogger", binding.tileBlogger));
        binding.tileTwitter.setOnClickListener(v -> selectPlatform("Twitter", binding.tileTwitter));
        binding.tileImgur.setOnClickListener(v -> selectPlatform("Imgur", binding.tileImgur));
        binding.tileMedium.setOnClickListener(v -> selectPlatform("Medium", binding.tileMedium));
        binding.tileTumblr.setOnClickListener(v -> selectPlatform("Tumblr", binding.tileTumblr));
        binding.tileFacebook.setOnClickListener(v -> selectPlatform("Facebook", binding.tileFacebook));

        selectPlatform("Twitter", binding.tileTwitter);
    }

    private void selectPlatform(String platform, View tile) {
        selectedPlatform = platform;
        resetTiles();
        tile.setBackgroundResource(R.drawable.bg_platform_selected);
        binding.tvConnectedAs.setText("Connected to " + platform);
    }

    private void resetTiles() {
        binding.tileBlogger.setBackgroundResource(R.drawable.bg_platform_tile);
        binding.tileTwitter.setBackgroundResource(R.drawable.bg_platform_tile);
        binding.tileImgur.setBackgroundResource(R.drawable.bg_platform_tile);
        binding.tileMedium.setBackgroundResource(R.drawable.bg_platform_tile);
        binding.tileTumblr.setBackgroundResource(R.drawable.bg_platform_tile);
        binding.tileFacebook.setBackgroundResource(R.drawable.bg_platform_tile);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnUploadNow.setOnClickListener(v -> checkAndUpload());
        binding.btnSwitchAccount.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Account Switch", Toast.LENGTH_SHORT).show());
    }

    private void updateNetworkStatus() {
        if (!NetworkUtils.isOnline(requireContext())) {
            // Disable upload button and show offline message
            binding.btnUploadNow.setEnabled(false);
            binding.btnUploadNow.setText("OFFLINE - Connect to internet to share");
            binding.btnUploadNow.setAlpha(0.5f);

            // Show warning on all platform tiles
            Toast.makeText(requireContext(),
                    "⚠️ You are offline. Please connect to internet to share posts.",
                    Toast.LENGTH_LONG).show();
        } else {
            binding.btnUploadNow.setEnabled(true);
            binding.btnUploadNow.setText("Upload to " + selectedPlatform);
            binding.btnUploadNow.setAlpha(1f);
        }
    }

    private void checkAndUpload() {
        // First check: Is there a message?
        if (message == null) {
            Toast.makeText(requireContext(), "Error: No data to share", Toast.LENGTH_SHORT).show();
            return;
        }

        // Second check: Is device online? (CRITICAL - Prevents opening apps)
        if (!NetworkUtils.isOnline(requireContext())) {
            showNoInternetDialog();
            return;
        }

        // Third check: Is the target app installed?
        if (!isAppInstalled(getPackageNameForPlatform())) {
            showAppNotInstalledDialog();
            return;
        }

        // All checks passed - proceed with sharing
        triggerUpload();
    }

    private String getPackageNameForPlatform() {
        switch (selectedPlatform) {
            case "Twitter":
                return "com.twitter.android";
            case "Facebook":
                return "com.facebook.katana";
            case "Gmail":
                return "com.google.android.gm";
            case "Instagram":
                return "com.instagram.android";
            default:
                return null;
        }
    }

    private boolean isAppInstalled(String packageName) {
        if (packageName == null) return true;
        try {
            requireContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("📡 No Internet Connection")
                .setMessage("Cannot share to " + selectedPlatform + " without internet.\n\n" +
                        "💡 Tip: This post is saved locally. Go to Settings and sync when you're online.")
                .setPositiveButton("Go to Settings", (d, w) -> {
                    NavHostFragment.findNavController(this).navigate(R.id.action_home_to_settings);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showAppNotInstalledDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ App Not Installed")
                .setMessage(selectedPlatform + " app is not installed on your device.\n\n" +
                        "Please install it from Google Play to share content.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void triggerUpload() {
        // Already using "DraftSpace" - correct!
        String shareText = "DraftSpace: " + message.title + "\n\n" + message.body;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_SUBJECT, message.title);

        // Target specific platform
        String packageName = getPackageNameForPlatform();
        if (packageName != null) {
            intent.setPackage(packageName);
        }

        try {
            startActivity(Intent.createChooser(intent, "Upload to " + selectedPlatform));
            // Mark as synced after successful share attempt
            if (message.isPending) {
                db.updateSyncStatus(message.id, 0);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot share to " + selectedPlatform, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNetworkStatus(); // Recheck network when returning to fragment
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}