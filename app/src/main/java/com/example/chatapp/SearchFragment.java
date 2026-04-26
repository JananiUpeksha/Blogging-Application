package com.example.chatapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatapp.databinding.FragmentSearchBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private DatabaseHelper db;
    private MessageAdapter adapter;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_WITH_IMAGES = 1;
    private static final int FILTER_RECENT = 2;
    private static final int FILTER_OLDEST = 3;

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
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_search_to_messageView, args);
                },
                count -> {}
        );

        binding.recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResults.setAdapter(adapter);
    }

    private void setupSearchBar() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
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
        binding.chipAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        binding.chipWithImages.setOnClickListener(v -> setFilter(FILTER_WITH_IMAGES));
        binding.chipRecent.setOnClickListener(v -> setFilter(FILTER_RECENT));
        binding.chipOldest.setOnClickListener(v -> setFilter(FILTER_OLDEST));

        updateChipSelection();
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateChipSelection();
        reRunSearch();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        binding.btnClearSearchEmpty.setOnClickListener(v -> {
            binding.etSearch.setText("");
            runSearch("");
        });
    }

    private void reRunSearch() {
        runSearch(binding.etSearch.getText().toString().trim());
    }

    private void runSearch(String query) {
        List<Message> results;

        if (query.isEmpty()) {
            results = db.getAllMessages();
        } else {
            results = db.searchMessages(query);
        }

        results = applyFilter(results);
        adapter.submitList(results);

        int count = results.size();
        binding.tvResultCount.setText(getString(R.string.results_found, count));

        boolean isEmpty = count == 0 && !query.isEmpty();

        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerResults.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private List<Message> applyFilter(List<Message> original) {

        // 🔹 Always work on a copy (avoid mutating DB list)
        List<Message> list = new ArrayList<>(original);

        switch (currentFilter) {

            case FILTER_WITH_IMAGES:
                list.removeIf(m -> m.imagePath == null || m.imagePath.isEmpty());
                break;

            case FILTER_RECENT:
                Collections.sort(list, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                break;

            case FILTER_OLDEST:
                Collections.sort(list, Comparator.comparingLong(m -> m.timestamp));
                break;

            case FILTER_ALL:
            default:
                // Optional: default sort by recent
                Collections.sort(list, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                break;
        }

        return list;
    }

    private void updateChipSelection() {
        int colorActive = requireContext().getColor(R.color.action_blue);
        int colorInactive = requireContext().getColor(R.color.bg_main);
        int textActive = requireContext().getColor(android.R.color.white);
        int textInactive = requireContext().getColor(R.color.text_muted);

        TextView[] views = {
                binding.chipAll,
                binding.chipWithImages,
                binding.chipRecent,
                binding.chipOldest
        };

        int[] filters = {
                FILTER_ALL,
                FILTER_WITH_IMAGES,
                FILTER_RECENT,
                FILTER_OLDEST
        };

        for (int i = 0; i < views.length; i++) {
            boolean active = (currentFilter == filters[i]);

            views[i].setBackgroundTintList(
                    ColorStateList.valueOf(active ? colorActive : colorInactive)
            );
            views[i].setTextColor(active ? textActive : textInactive);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}