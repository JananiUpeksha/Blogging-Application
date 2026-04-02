package com.example.chatapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.chatapp.databinding.FragmentMessageViewBinding;

import java.util.Date;

public class MessageViewFragment extends Fragment {

    private FragmentMessageViewBinding binding;
    private DatabaseHelper db;
    private Message        message;
    private long           messageId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        if (getArguments() != null) {
            messageId = getArguments().getLong("messageId", -1);
        }

        if (messageId == -1) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        message = db.getMessageById(messageId);
        if (message == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        populateUI();
        setupClickListeners();
    }

    private void populateUI() {
        binding.tvTitle.setText(message.title);
        binding.tvBody.setText(message.body);

        String dateStr = (String) DateFormat.format(
                "MMMM dd, yyyy • hh:mm aa", new Date(message.timestamp));
        binding.tvTimestamp.setText(dateStr);

        if (message.imagePath != null && !message.imagePath.isEmpty()) {
            binding.imageCard.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(Uri.parse(message.imagePath))
                    .centerCrop()
                    .into(binding.ivMessageImage);
        } else {
            binding.imageCard.setVisibility(View.GONE);
        }

        binding.tvPendingBadge.setVisibility(message.isPending ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        binding.btnEdit.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("messageId", messageId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_messageView_to_createEdit, args);
        });

        binding.btnDelete.setOnClickListener(v -> confirmDelete());

        binding.btnShareEmail.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("messageId", messageId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_messageView_to_emailShare, args);
        });

        binding.btnUploadSocial.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("messageId", messageId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_messageView_to_uploadSocial, args);
        });

        binding.btnDeleteBottom.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete this post?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    // This method call is now valid
                    db.deleteMessage(messageId);
                    Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
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