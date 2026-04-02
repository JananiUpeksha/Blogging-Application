package com.example.chatapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.chatapp.databinding.FragmentCreateEditBinding;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateEditFragment extends Fragment {

    private FragmentCreateEditBinding binding;
    private DatabaseHelper db;
    private long editMessageId = -1;
    private Message existingMessage;
    private String currentImagePath = null;
    private Uri currentImageUri = null;
    private Uri cameraFileUri = null;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            currentImageUri = cameraFileUri;
                            showImagePreview(currentImageUri);
                        }
                    });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            try {
                                // Grant persistable permission for selected image
                                requireContext().getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                            currentImageUri = uri;
                            currentImagePath = null;
                            showImagePreview(currentImageUri);
                        }
                    });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {});

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        if (getArguments() != null) {
            editMessageId = getArguments().getLong("messageId", -1);
        }
        if (editMessageId != -1) {
            existingMessage = db.getMessageById(editMessageId);
            populateForEdit();
        }

        setupCharCounter();
        setupClickListeners();
        updateTitle();
    }

    private void updateTitle() {
        binding.tvScreenTitle.setText(editMessageId != -1 ? "Edit Post" : "Create New Post");
    }

    private void populateForEdit() {
        if (existingMessage == null) return;
        binding.etTitle.setText(existingMessage.title);
        binding.etContent.setText(existingMessage.body);
        if (existingMessage.imagePath != null && !existingMessage.imagePath.isEmpty()) {
            currentImageUri = Uri.parse(existingMessage.imagePath);
            showImagePreview(currentImageUri);
        }
    }

    private void setupCharCounter() {
        binding.etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                binding.tvCharCounter.setText(s.length() + " / 500");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnSave.setOnClickListener(v -> saveMessage());
        binding.btnCamera.setOnClickListener(v -> openCamera());
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnRemoveImage.setOnClickListener(v -> {
            currentImageUri = null;
            currentImagePath = null;
            binding.imagePreviewCard.setVisibility(View.GONE);
        });
    }

    private void openCamera() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return;
        }
        try {
            File photoFile = createImageFile();
            cameraFileUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            currentImagePath = photoFile.getAbsolutePath();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Cannot open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("TP_" + timestamp, ".jpg", storageDir);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void showImagePreview(Uri uri) {
        binding.imagePreviewCard.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(binding.ivPreview);
    }

    private void saveMessage() {
        String title = binding.etTitle.getText().toString().trim();
        String content = binding.etContent.getText().toString().trim();
        if (title.isEmpty()) {
            binding.etTitle.setError("Title is required");
            return;
        }
        String imageStr = (currentImageUri != null) ? currentImageUri.toString() : currentImagePath;
        if (editMessageId != -1) {
            // This now matches the new method in DatabaseHelper.java
            db.updateMessage(editMessageId, title, content, imageStr);
            Toast.makeText(requireContext(), "Post updated", Toast.LENGTH_SHORT).show();
        } else {
            db.insertMessage(title, content, imageStr, false);
            Toast.makeText(requireContext(), "Post created", Toast.LENGTH_SHORT).show();
        }
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}