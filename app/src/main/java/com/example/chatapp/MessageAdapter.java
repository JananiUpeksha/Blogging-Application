package com.example.chatapp;

import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatapp.databinding.ItemMessageBinding;
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
            b.tvTitle.setText(msg.title);
            b.tvPreview.setText(msg.body);
            b.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(msg.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

            // Handle image
            if (msg.imagePath != null && !msg.imagePath.isEmpty()) {
                b.ivPostImage.setVisibility(View.VISIBLE);
                Glide.with(b.getRoot().getContext())
                        .load(Uri.parse(msg.imagePath))
                        .into(b.ivPostImage);
            } else {
                b.ivPostImage.setVisibility(View.GONE);
            }

            // Handle selection
            boolean isSelected = selected.contains(msg.id);
            if (multiSelectMode) {
                b.cbSelect.setVisibility(View.VISIBLE);
                b.cbSelect.setChecked(isSelected);
            } else {
                b.cbSelect.setVisibility(View.GONE);
            }

            b.getRoot().setOnClickListener(v -> {
                if (multiSelectMode) {
                    toggleSelection(msg);
                } else {
                    clickListener.onItemClick(msg);
                }
            });

            b.getRoot().setOnLongClickListener(v -> {
                if (!multiSelectMode) {
                    multiSelectMode = true;
                    toggleSelection(msg);
                }
                return true;
            });
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

            notifyDataSetChanged();

            if (selectionListener != null) {
                selectionListener.onSelectionChanged(selected.size());
            }
        }
    }
}