package com.example.chatapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chatapp.databinding.FragmentUploadSocialBinding;

/**
 * Upload to Social screen.
 *
 * Allows user to select a platform (Blogger / Twitter / Imgur / Medium)
 * and tap "Upload Now".
 *
 * NOTE: Actual OAuth / API calls are stubbed with Toasts.
 * Replace the uploadTo*() methods with real API calls when ready.
 */
public class UploadSocialFragment extends Fragment {

    private FragmentUploadSocialBinding binding;
    private DatabaseHelper db;
    private Message        message;

    // Which platform tile is selected
    private String selectedPlatform = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadSocialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        long messageId = -1;
        if (getArguments() != null) {
            messageId = getArguments().getLong("messageId", -1);
        }
        if (messageId != -1) {
            message = db.getMessageById(messageId);
        }

        populatePreview();
        setupPlatformTiles();
        setupClickListeners();
    }

    // ── Preview ────────────────────────────────────────────────────

    private void populatePreview() {
        if (message == null) return;
        binding.tvPreviewTitle.setText(message.title);
        String bodyPreview = message.body != null
                ? message.body.substring(0, Math.min(message.body.length(), 140))
                : "";
        binding.tvPreviewBody.setText(bodyPreview);
        binding.tvCharCount.setText(bodyPreview.length() + "/280");
    }

    // ── Platform Tiles ─────────────────────────────────────────────

    private void setupPlatformTiles() {
        binding.tileBlogger.setOnClickListener(v  -> selectPlatform("Blogger",  binding.tileBlogger));
        binding.tileTwitter.setOnClickListener(v  -> selectPlatform("Twitter",  binding.tileTwitter));
        binding.tileImgur.setOnClickListener(v    -> selectPlatform("Imgur",    binding.tileImgur));
        binding.tileMedium.setOnClickListener(v   -> selectPlatform("Medium",   binding.tileMedium));
        binding.tileTumblr.setOnClickListener(v   -> selectPlatform("Tumblr",   binding.tileTumblr));
        binding.tileFacebook.setOnClickListener(v -> selectPlatform("Facebook", binding.tileFacebook));

        // Pre-select Twitter to match the UI design
        selectPlatform("Twitter", binding.tileTwitter);
    }

    private void selectPlatform(String platform, View tile) {
        selectedPlatform = platform;
        // Reset all tiles
        resetTile(binding.tileBlogger);
        resetTile(binding.tileTwitter);
        resetTile(binding.tileImgur);
        resetTile(binding.tileMedium);
        resetTile(binding.tileTumblr);
        resetTile(binding.tileFacebook);
        // Highlight selected
        tile.setBackgroundResource(R.drawable.bg_platform_selected);
        binding.tvConnectedAs.setText("Connected to " + platform);
    }

    private void resetTile(View tile) {
        tile.setBackgroundResource(R.drawable.bg_platform_tile);
    }

    // ── Clicks ─────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        binding.btnUploadNow.setOnClickListener(v -> triggerUpload());

        binding.btnSwitchAccount.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Account switching – connect OAuth here", Toast.LENGTH_SHORT).show());
    }

    private void triggerUpload() {
        if (selectedPlatform == null) {
            Toast.makeText(requireContext(), "Select a platform first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message == null) {
            Toast.makeText(requireContext(), "No message to upload", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            Toast.makeText(requireContext(),
                    "You're offline – upload queued", Toast.LENGTH_LONG).show();
            // Mark as pending in DB so it shows in the offline queue
            db.updateMessage(message.id, message.title, message.body, message.imagePath);
            return;
        }

        // Stub – replace with real API calls
        Toast.makeText(requireContext(),
                "Uploading to " + selectedPlatform + "…", Toast.LENGTH_SHORT).show();

        // Simulate success
        binding.btnUploadNow.postDelayed(() -> {
            if (isAdded()) {
                Toast.makeText(requireContext(),
                        "Posted to " + selectedPlatform + " ✓", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        }, 1500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
