package com.example.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.chatapp.databinding.FragmentEmailShareBinding;
import java.io.File;

public class EmailShareFragment extends Fragment {

    private FragmentEmailShareBinding binding;
    private Message message;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEmailShareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());

        long messageId = -1;
        if (getArguments() != null) {
            messageId = getArguments().getLong("messageId", -1);
        }
        if (messageId != -1) {
            message = db.getMessageById(messageId);
        }

        prefillFields();
        setupClickListeners();
        checkNetworkAndUpdateUI();
    }

    private void prefillFields() {
        if (message == null) return;

        String subjectText = getString(R.string.email_subject_prefix, message.title);
        binding.etSubject.setText(subjectText);

        String preview = message.body != null
                ? message.body.substring(0, Math.min(message.body.length(), 150))
                : "";
        binding.tvMessagePreview.setText(preview);
        binding.tvMessageTitle.setText(message.title);

        // Check if image actually exists
        boolean hasImage = false;
        if (message.imagePath != null && !message.imagePath.isEmpty()) {
            File imageFile = new File(message.imagePath);
            hasImage = imageFile.exists();
        }

        String attachmentInfo = hasImage ? getString(R.string.has_photo) : getString(R.string.text_only);
        binding.tvAttachmentInfo.setText(attachmentInfo);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        binding.btnSend.setOnClickListener(v -> checkAndSendEmail());
        binding.btnGmail.setOnClickListener(v -> checkAndSendEmailWithPackage("com.google.android.gm"));
        binding.btnOutlook.setOnClickListener(v -> checkAndSendEmailWithPackage("com.microsoft.office.outlook"));
        binding.btnOthers.setOnClickListener(v -> checkAndSendEmail());
    }

    private void checkNetworkAndUpdateUI() {
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.btnSend.setEnabled(false);
            binding.btnGmail.setEnabled(false);
            binding.btnOutlook.setEnabled(false);
            binding.btnOthers.setEnabled(false);

            binding.btnSend.setAlpha(0.5f);
            binding.btnGmail.setAlpha(0.5f);
            binding.btnOutlook.setAlpha(0.5f);
            binding.btnOthers.setAlpha(0.5f);

            Toast.makeText(requireContext(),
                    R.string.offline_message,
                    Toast.LENGTH_LONG).show();
        } else {
            binding.btnSend.setEnabled(true);
            binding.btnGmail.setEnabled(true);
            binding.btnOutlook.setEnabled(true);
            binding.btnOthers.setEnabled(true);

            binding.btnSend.setAlpha(1f);
            binding.btnGmail.setAlpha(1f);
            binding.btnOutlook.setAlpha(1f);
            binding.btnOthers.setAlpha(1f);
        }
    }

    private void checkAndSendEmail() {
        if (!NetworkUtils.isOnline(requireContext())) {
            showNoInternetDialog();
            return;
        }
        sendEmail();
    }

    private void checkAndSendEmailWithPackage(String packageName) {
        if (!NetworkUtils.isOnline(requireContext())) {
            showNoInternetDialog();
            return;
        }
        sendEmailWithPackage(packageName);
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_internet_title)
                .setMessage(R.string.no_internet_message)
                .setPositiveButton(R.string.open_wifi_settings, (d, w) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private Uri getImageUri() {
        if (message == null || message.imagePath == null || message.imagePath.isEmpty()) {
            return null;
        }

        try {
            File imageFile = new File(message.imagePath);

            // Log the path for debugging
            Log.d("EmailShare", "Image path: " + message.imagePath);
            Log.d("EmailShare", "File exists: " + imageFile.exists());
            Log.d("EmailShare", "File absolute path: " + imageFile.getAbsolutePath());

            if (!imageFile.exists()) {
                // Try alternative paths
                String fileName = new File(message.imagePath).getName();

                // Try Pictures directory
                File picsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (picsDir != null) {
                    File altFile = new File(picsDir, fileName);
                    if (altFile.exists()) {
                        imageFile = altFile;
                        Log.d("EmailShare", "Found image in Pictures dir: " + altFile.getAbsolutePath());
                    }
                }

                // Try DCIM directory
                if (!imageFile.exists()) {
                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    File altFile = new File(dcimDir, fileName);
                    if (altFile.exists()) {
                        imageFile = altFile;
                        Log.d("EmailShare", "Found image in DCIM dir: " + altFile.getAbsolutePath());
                    }
                }

                if (!imageFile.exists()) {
                    String fileNameOnly = fileName;
                    Toast.makeText(requireContext(),
                            getString(R.string.image_not_found, fileNameOnly),
                            Toast.LENGTH_LONG).show();
                    return null;
                }
            }

            // Create URI using FileProvider for Android 7.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        imageFile);
            } else {
                return Uri.fromFile(imageFile);
            }
        } catch (Exception e) {
            Log.e("EmailShare", "Error getting image URI", e);
            Toast.makeText(requireContext(),
                    getString(R.string.error_accessing_image, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void sendEmail() {
        String to = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = buildEmailBody();

        if (to.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_recipient, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        // Handle image attachment
        Uri imageUri = getImageUri();
        if (imageUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Toast.makeText(requireContext(), R.string.image_attached, Toast.LENGTH_SHORT).show();
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.send_email_via)));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_email_app, Toast.LENGTH_LONG).show();
        }
    }

    private void sendEmailWithPackage(String packageName) {
        String to = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = buildEmailBody();

        if (to.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_recipient, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        // Handle image attachment
        Uri imageUri = getImageUri();
        if (imageUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.app_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private String buildEmailBody() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            if (message.title != null && !message.title.isEmpty()) {
                sb.append("📄 ").append(message.title).append("\n\n");
            }
            if (message.body != null && !message.body.isEmpty()) {
                sb.append(message.body).append("\n\n");
            }
        }
        sb.append(getString(R.string.shared_from_draftspace));
        return sb.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNetworkAndUpdateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}