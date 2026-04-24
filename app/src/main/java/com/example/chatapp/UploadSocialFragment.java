package com.example.chatapp;

import android.content.Intent;
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

public class UploadSocialFragment extends Fragment {

    private FragmentUploadSocialBinding binding;
    private DatabaseHelper db;
    private Message message;
    private String selectedPlatform = "Twitter"; // Default selection

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadSocialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        // Retrieve message data safely
        if (getArguments() != null) {
            long messageId = getArguments().getLong("messageId", -1);
            if (messageId != -1) {
                message = db.getMessageById(messageId);
            }
        }

        populatePreview();
        setupPlatformTiles();
        setupClickListeners();
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

        // Initial UI state
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
        binding.btnUploadNow.setOnClickListener(v -> triggerUpload());
        binding.btnSwitchAccount.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Account Switch", Toast.LENGTH_SHORT).show());
    }

    private void triggerUpload() {
        if (message == null) {
            Toast.makeText(requireContext(), "Error: No data", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText = "DraftSpace: " + message.title + "\n\n" + message.body;

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);

            // Try to open Twitter specifically
            intent.setPackage("com.twitter.android");

            startActivity(Intent.createChooser(intent, "Upload to " + selectedPlatform));
        } catch (Exception e) {
            // Fallback: Use generic share if Twitter not installed
            genericShare(shareText);
        }
    }

    private void genericShare(String shareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}