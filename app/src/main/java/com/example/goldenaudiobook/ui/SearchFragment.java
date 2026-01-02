package com.example.goldenaudiobook.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudiobookAdapter;
import com.example.goldenaudiobook.databinding.FragmentSearchBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.viewmodel.SearchViewModel;

/**
 * Fragment for searching audiobooks
 */
public class SearchFragment extends Fragment implements AudiobookAdapter.OnAudiobookClickListener {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private AudiobookAdapter audiobookAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearchInput();
        observeViewModel();
    }

    private void setupRecyclerView() {
        audiobookAdapter = new AudiobookAdapter(this);
        binding.searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.searchResultsRecyclerView.setAdapter(audiobookAdapter);
    }

    private void setupSearchInput() {
        // Text change listener for search
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button
                binding.clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search on keyboard action
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Clear button click
        binding.clearButton.setOnClickListener(v -> {
            binding.searchEditText.setText("");
            viewModel.clearResults();
            showEmptyState();
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), audiobooks -> {
            if (audiobooks != null && !audiobooks.isEmpty()) {
                audiobookAdapter.submitList(audiobooks);
                showResults();
            } else if (viewModel.getHasSearched().getValue() != null && viewModel.getHasSearched().getValue()) {
                showEmptyState();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });

        viewModel.getCurrentQuery().observe(getViewLifecycleOwner(), query -> {
            // Could update UI with current search term if needed
        });
    }

    private void performSearch() {
        String query = binding.searchEditText.getText().toString().trim();
        if (!query.isEmpty()) {
            viewModel.search(query);
            hideKeyboard();
        }
    }

    private void showResults() {
        binding.searchResultsRecyclerView.setVisibility(View.VISIBLE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.errorStateLayout.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.searchResultsRecyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.errorStateLayout.setVisibility(View.GONE);

        String query = viewModel.getCurrentQuery().getValue();
        if (query != null && !query.isEmpty()) {
            binding.emptyStateText.setText("No results found for \"" + query + "\"");
        } else {
            binding.emptyStateText.setText("Search for your favorite audiobooks");
        }
    }

    private void showError(String errorMessage) {
        binding.searchResultsRecyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.errorStateLayout.setVisibility(View.VISIBLE);
        binding.errorText.setText(errorMessage);
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.searchEditText.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onAudiobookClick(Audiobook audiobook) {
        if (audiobook.getUrl() != null && !audiobook.getUrl().isEmpty()) {
            // Navigate to audiobook detail
            android.content.Intent intent = new android.content.Intent(requireContext(), AudiobookDetailActivity.class);
            intent.putExtra("audiobook_url", audiobook.getUrl());
            intent.putExtra("audiobook_title", audiobook.getTitle());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
