package com.example.chatapp;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.chatapp.databinding.FragmentHomeBinding;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MessageAdapter adapter;
    private DatabaseHelper db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());

        setupRecyclerView();
        setupClickListeners();
        loadMessages();
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(
                msg -> {
                    Bundle args = new Bundle();
                    args.putLong("messageId", msg.id);
                    NavHostFragment.findNavController(this).navigate(R.id.action_home_to_messageView, args);
                },
                selectedCount -> {
                    if (selectedCount > 0) {
                        binding.selectionHeader.setVisibility(View.VISIBLE);
                        binding.normalHeader.setVisibility(View.GONE);
                        // REAL-TIME UPDATE: Display correct count
                        binding.tvSelectionCount.setText(selectedCount + " selected");
                        binding.fabNewMessage.setVisibility(View.GONE);
                    } else {
                        binding.selectionHeader.setVisibility(View.GONE);
                        binding.normalHeader.setVisibility(View.VISIBLE);
                        binding.fabNewMessage.setVisibility(View.VISIBLE);
                    }
                }
        );

        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMessages.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.fabNewMessage.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_createEdit));

        binding.btnSettings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_settings));

        binding.btnExitSelection.setOnClickListener(v -> adapter.clearSelection());

        binding.btnDeleteSelection.setOnClickListener(v -> confirmDeleteSelected());
    }

    private void confirmDeleteSelected() {
        List<Long> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + ids.size() + " posts?")
                .setMessage("This will permanently remove these items from your local database.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteMessages(ids);
                    adapter.clearSelection();
                    loadMessages();
                    Toast.makeText(requireContext(), "Posts deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadMessages() {
        List<Message> messages = db.getAllMessages();
        adapter.submitList(messages);
        binding.tvEmptyState.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerMessages.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMessages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}