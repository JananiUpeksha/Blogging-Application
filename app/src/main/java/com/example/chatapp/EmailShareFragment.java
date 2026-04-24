package com.example.chatapp;

import android.content.Intent;
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
import com.example.chatapp.databinding.FragmentEmailShareBinding;

public class EmailShareFragment extends Fragment {

    private FragmentEmailShareBinding binding;
    private DatabaseHelper db;
    private Message message;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEmailShareBinding.inflate(inflater, container, false);
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

        prefillFields();
        setupClickListeners();
        checkNetworkAndUpdateUI();
    }

    private void prefillFields() {
        if (message == null) return;
        // Changed from "Talk Pal" to "DraftSpace"
        binding.etSubject.setText("DraftSpace: " + message.title);
        String preview = message.body != null
                ? message.body.substring(0, Math.min(message.body.length(), 150))
                : "";
        binding.tvMessagePreview.setText(preview);
        binding.tvMessageTitle.setText(message.title);

        String sizeInfo = (message.imagePath != null && !message.imagePath.isEmpty())
                ? "Has photo attached"
                : "Text only";
        binding.tvAttachmentInfo.setText(sizeInfo);
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
            // Disable all email buttons
            binding.btnSend.setEnabled(false);
            binding.btnGmail.setEnabled(false);
            binding.btnOutlook.setEnabled(false);
            binding.btnOthers.setEnabled(false);

            binding.btnSend.setAlpha(0.5f);
            binding.btnGmail.setAlpha(0.5f);
            binding.btnOutlook.setAlpha(0.5f);
            binding.btnOthers.setAlpha(0.5f);

            Toast.makeText(requireContext(),
                    "📡 You are offline. Please connect to internet to send emails.",
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
                .setTitle("📡 No Internet Connection")
                .setMessage("Cannot send emails without internet.\n\n" +
                        "Please connect to WiFi or mobile data and try again.")
                .setPositiveButton("Open WiFi Settings", (d, w) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void sendEmail() {
        String to = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = buildEmailBody();

        if (message != null && message.imagePath != null && !message.imagePath.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(message.imagePath));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Send via"));
        } else {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            try {
                startActivity(Intent.createChooser(intent, "Send email"));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendEmailWithPackage(String packageName) {
        String to = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body = buildEmailBody();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "App not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildEmailBody() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message.title).append("\n\n");
            if (message.body != null) sb.append(message.body).append("\n\n");
        }
        // Changed from "Talk Pal" to "DraftSpace"
        sb.append("— Shared from DraftSpace");
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