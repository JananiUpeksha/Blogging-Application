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
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.chatapp.databinding.FragmentEmailShareBinding;

/**
 * Email share screen.
 *
 * Pre-fills subject with message title and body with message content.
 * On "Send" fires an ACTION_SENDTO intent that opens the user's email client
 * (Gmail / Outlook / etc.).
 *
 * If an image is attached, it is also shared via ACTION_SEND with EXTRA_STREAM.
 */
public class EmailShareFragment extends Fragment {

    private FragmentEmailShareBinding binding;
    private DatabaseHelper db;
    private Message        message;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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
    }

    // ── Pre-fill ───────────────────────────────────────────────────

    private void prefillFields() {
        if (message == null) return;
        binding.etSubject.setText("Talk Pal: " + message.title);
        // Body shows first ~150 chars of the message body
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

    // ── Clicks ─────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        binding.btnSend.setOnClickListener(v -> sendEmail());

        // Quick-send buttons available in your new XML
        binding.btnGmail.setOnClickListener(v   -> sendEmailWithPackage("com.google.android.gm"));
        binding.btnOutlook.setOnClickListener(v -> sendEmailWithPackage("com.microsoft.office.outlook"));

        // btnYahoo was removed from fragment_email_share.xml, so we remove it here too

        binding.btnOthers.setOnClickListener(v  -> sendEmail()); // generic chooser
    }

    // ── Email Intent ───────────────────────────────────────────────

    private void sendEmail() {
        String to      = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body    = buildEmailBody();

        if (message != null && message.imagePath != null && !message.imagePath.isEmpty()) {
            // Share with image attachment
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL,   new String[]{to});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT,    body);
            intent.putExtra(Intent.EXTRA_STREAM,  Uri.parse(message.imagePath));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Send via"));
        } else {
            // Text-only
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL,   new String[]{to});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT,    body);
            try {
                startActivity(Intent.createChooser(intent, "Send email"));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(requireContext(),
                        "No email app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendEmailWithPackage(String packageName) {
        String to      = binding.etTo.getText().toString().trim();
        String subject = binding.etSubject.getText().toString().trim();
        String body    = buildEmailBody();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.setPackage(packageName);
        intent.putExtra(Intent.EXTRA_EMAIL,   new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT,    body);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // App not installed – fall back to chooser
            sendEmail();
        }
    }

    private String buildEmailBody() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message.title).append("\n\n");
            if (message.body != null) sb.append(message.body).append("\n\n");
        }
        sb.append("— Shared from Talk Pal");
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
