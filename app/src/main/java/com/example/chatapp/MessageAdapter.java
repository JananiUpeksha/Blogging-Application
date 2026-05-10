package com.example.chatapp;

import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.chatapp.databinding.ItemMessageBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Message message);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private List<Message> items = new ArrayList<>();
    private Set<Long> selected = new HashSet<>();
    private boolean multiSelectMode = false;
    private OnItemClickListener clickListener;
    private OnSelectionChangedListener selectionListener;
    private String searchQuery = "";

    public MessageAdapter(OnItemClickListener clickListener, OnSelectionChangedListener selectionListener) {
        this.clickListener = clickListener;
        this.selectionListener = selectionListener;
    }

    public void submitList(List<Message> newList) {
        this.items = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selected);
    }

    public void clearSelection() {
        selected.clear();
        multiSelectMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageBinding binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMessageBinding b;

        ViewHolder(ItemMessageBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Message msg) {
            if (msg == null) return;

            // Set title with highlight if searching
            String title = msg.title != null ? msg.title : "";
            if (!searchQuery.isEmpty()) {
                b.tvTitle.setText(highlightText(title));
            } else {
                b.tvTitle.setText(title);
            }

            // Set preview with highlight if searching
            String preview = msg.body != null ? msg.body : "";
            if (!searchQuery.isEmpty()) {
                b.tvPreview.setText(highlightText(preview));
            } else {
                b.tvPreview.setText(preview);
            }

            // Set timestamp
            b.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(
                    msg.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            ));

            // Handle image loading - FIXED VERSION
            loadImage(msg);

            // Handle selection mode
            boolean isSelected = selected.contains(msg.id);
            if (multiSelectMode) {
                b.cbSelect.setVisibility(View.VISIBLE);
                b.cbSelect.setChecked(isSelected);
            } else {
                b.cbSelect.setVisibility(View.GONE);
            }

            // Handle click events
            b.getRoot().setOnClickListener(v -> {
                if (multiSelectMode) {
                    toggleSelection(msg);
                } else {
                    if (clickListener != null) {
                        clickListener.onItemClick(msg);
                    }
                }
            });

            b.getRoot().setOnLongClickListener(v -> {
                if (!multiSelectMode) {
                    multiSelectMode = true;
                    toggleSelection(msg);
                    notifyDataSetChanged();
                }
                return true;
            });
        }

        private void loadImage(Message msg) {
            // Check if message has image path
            if (msg.imagePath != null && !msg.imagePath.isEmpty()) {
                try {
                    // Try to load as file path first
                    File imageFile = new File(msg.imagePath);

                    if (imageFile.exists()) {
                        // File exists - load it
                        b.ivPostImage.setVisibility(View.VISIBLE);

                        Uri imageUri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            imageUri = FileProvider.getUriForFile(
                                    b.getRoot().getContext(),
                                    b.getRoot().getContext().getPackageName() + ".fileprovider",
                                    imageFile
                            );
                        } else {
                            imageUri = Uri.fromFile(imageFile);
                        }

                        Glide.with(b.getRoot().getContext())
                                .load(imageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(b.ivPostImage);
                    } else {
                        // Try to load as direct URI
                        try {
                            Uri uri = Uri.parse(msg.imagePath);
                            if (uri != null) {
                                b.ivPostImage.setVisibility(View.VISIBLE);
                                Glide.with(b.getRoot().getContext())
                                        .load(uri)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .centerCrop()
                                        .into(b.ivPostImage);
                            } else {
                                hideImage();
                            }
                        } catch (Exception e) {
                            Log.e("MessageAdapter", "Error parsing URI", e);
                            hideImage();
                        }
                    }
                } catch (Exception e) {
                    Log.e("MessageAdapter", "Error loading image", e);
                    hideImage();
                }
            } else {
                // No image path - hide the ImageView
                hideImage();
            }
        }

        private void hideImage() {
            b.ivPostImage.setVisibility(View.GONE);
            // Clear any existing image to prevent showing wrong image on recycle
            b.ivPostImage.setImageDrawable(null);
        }

        private SpannableString highlightText(String text) {
            if (text == null || text.isEmpty() || searchQuery.isEmpty()) {
                return new SpannableString(text != null ? text : "");
            }

            SpannableString spannableString = new SpannableString(text);
            String lowerCaseText = text.toLowerCase();
            String lowerCaseQuery = searchQuery.toLowerCase();

            int startIndex = 0;
            while (startIndex < text.length()) {
                int index = lowerCaseText.indexOf(lowerCaseQuery, startIndex);
                if (index >= 0) {
                    try {
                        spannableString.setSpan(
                                new BackgroundColorSpan(android.graphics.Color.YELLOW),
                                index,
                                index + searchQuery.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        startIndex = index + searchQuery.length();
                    } catch (Exception e) {
                        break;
                    }
                } else {
                    break;
                }
            }

            return spannableString;
        }

        private void toggleSelection(Message msg) {
            if (selected.contains(msg.id)) {
                selected.remove(msg.id);
            } else {
                selected.add(msg.id);
            }

            if (selected.isEmpty()) {
                multiSelectMode = false;
            }

            notifyItemChanged(getAdapterPosition());

            if (selectionListener != null) {
                selectionListener.onSelectionChanged(selected.size());
            }
        }
    }
}