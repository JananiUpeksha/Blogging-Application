package com.example.chatapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.chatapp.databinding.FragmentSearchBinding;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private DatabaseHelper        db;
    private MessageAdapter        adapter;

    private static final int FILTER_ALL         = 0;
    private static final int FILTER_WITH_IMAGES = 1;
    private static final int FILTER_RECENT      = 2;
    private static final int FILTER_OLDEST      = 3;
    private int currentFilter = FILTER_ALL;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        setupRecyclerView();
        setupSearchBar();
        setupFilterChips();
        setupClickListeners();
        runSearch("");
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(
                msg -> {
                    Bundle args = new Bundle();
                    args.putLong("messageId", msg.id);
                    NavHostFragment.findNavController(this).navigate(R.id.action_search_to_messageView, args);
                },
                count -> { }
        );
        binding.recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResults.setAdapter(adapter);
    }

    private void setupSearchBar() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                runSearch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            runSearch("");
        });
    }

    private void setupFilterChips() {
        binding.chipAll.setOnClickListener(v -> { currentFilter = FILTER_ALL; updateChipSelection(); reRunSearch(); });
        binding.chipWithImages.setOnClickListener(v -> { currentFilter = FILTER_WITH_IMAGES; updateChipSelection(); reRunSearch(); });
        binding.chipRecent.setOnClickListener(v -> { currentFilter = FILTER_RECENT; updateChipSelection(); reRunSearch(); });
        binding.chipOldest.setOnClickListener(v -> { currentFilter = FILTER_OLDEST; updateChipSelection(); reRunSearch(); });
        updateChipSelection();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnClearSearchEmpty.setOnClickListener(v -> {
            binding.etSearch.setText("");
            runSearch("");
        });
    }

    private void reRunSearch() { runSearch(binding.etSearch.getText().toString().trim()); }

    private void runSearch(String query) {
        List<Message> results;
        if (query.isEmpty()) {
            results = db.getAllMessages();
        } else {
            // This call is now supported by the updated DatabaseHelper
            results = db.searchMessages(query);
        }
        results = applyFilter(results);
        adapter.submitList(results);
        int count = results.size();
        binding.tvResultCount.setText(count + " result" + (count == 1 ? "" : "s") + " found");
        binding.emptyState.setVisibility(count == 0 && !query.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerResults.setVisibility(count == 0 && !query.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private List<Message> applyFilter(List<Message> list) {
        if (currentFilter == FILTER_WITH_IMAGES) {
            list.removeIf(m -> m.imagePath == null || m.imagePath.isEmpty());
        } else if (currentFilter == FILTER_OLDEST) {
            java.util.Collections.reverse(list);
        }
        return list;
    }

    private void updateChipSelection() {
        int colorActive   = requireContext().getColor(R.color.purple_primary);
        int colorInactive = requireContext().getColor(R.color.chip_inactive_bg);
        int textActive    = requireContext().getColor(android.R.color.white);
        int textInactive  = requireContext().getColor(R.color.text_secondary);
        android.widget.TextView[] views = {binding.chipAll, binding.chipWithImages, binding.chipRecent, binding.chipOldest};
        int[] filters = {FILTER_ALL, FILTER_WITH_IMAGES, FILTER_RECENT, FILTER_OLDEST};
        for (int i = 0; i < views.length; i++) {
            boolean active = (currentFilter == filters[i]);
            views[i].setBackgroundColor(active ? colorActive : colorInactive);
            views[i].setTextColor(active ? textActive : textInactive);
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}