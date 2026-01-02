package com.example.goldenaudiobook.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.goldenaudiobook.adapter.AudiobookAdapter;
import com.example.goldenaudiobook.databinding.FragmentHomeBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.viewmodel.HomeViewModel;

/**
 * Home Fragment displaying random audiobooks
 */
public class HomeFragment extends Fragment implements AudiobookAdapter.OnAudiobookClickListener {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private AudiobookAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        setupPagination();
        observeViewModel();

        // Load data
        viewModel.loadRandomAudiobooks();
    }

    private void setupRecyclerView() {
        adapter = new AudiobookAdapter(this);

        // Use GridLayoutManager with 2 columns for phones
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refresh();
        });
    }

    private void setupPagination() {
        // Previous button click
        binding.previousButton.setOnClickListener(v -> {
            viewModel.loadPreviousPage();
        });

        // Next button click
        binding.nextButton.setOnClickListener(v -> {
            viewModel.loadNextPage();
        });
    }

    private void observeViewModel() {
        viewModel.getAudiobooks().observe(getViewLifecycleOwner(), audiobooks -> {
            if (audiobooks != null && !audiobooks.isEmpty()) {
                adapter.submitList(audiobooks);
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
            if (isLoading && adapter.getItemCount() == 0) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Observe pagination state
        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), currentPage -> {
            updatePageIndicator();
        });

        viewModel.getTotalPages().observe(getViewLifecycleOwner(), totalPages -> {
            updatePageIndicator();
        });

        viewModel.getHasNextPage().observe(getViewLifecycleOwner(), hasNext -> {
            binding.nextButton.setEnabled(hasNext != null && hasNext);
            binding.nextButton.setAlpha(hasNext != null && hasNext ? 1.0f : 0.5f);
            updatePaginationVisibility();
        });

        viewModel.getHasPreviousPage().observe(getViewLifecycleOwner(), hasPrev -> {
            binding.previousButton.setEnabled(hasPrev != null && hasPrev);
            binding.previousButton.setAlpha(hasPrev != null && hasPrev ? 1.0f : 0.5f);
            updatePaginationVisibility();
        });
    }

    private void updatePageIndicator() {
        Integer current = viewModel.getCurrentPage().getValue();
        Integer total = viewModel.getTotalPages().getValue();
        if (current != null && total != null) {
            binding.pageIndicator.setText("Page " + current + " of " + total);
        }
    }

    private void updatePaginationVisibility() {
        Boolean hasNext = viewModel.getHasNextPage().getValue();
        Boolean hasPrev = viewModel.getHasPreviousPage().getValue();
        // Show pagination if there's more than 1 page total OR if we have pagination controls
        Integer total = viewModel.getTotalPages().getValue();
        if (total != null && total > 1) {
            binding.paginationLayout.setVisibility(View.VISIBLE);
        } else if (Boolean.TRUE.equals(hasNext) || Boolean.TRUE.equals(hasPrev)) {
            binding.paginationLayout.setVisibility(View.VISIBLE);
        } else {
            binding.paginationLayout.setVisibility(View.GONE);
        }
    }

    @OptIn(markerClass = UnstableApi.class) @Override
    public void onAudiobookClick(Audiobook audiobook) {
        // Navigate to audiobook detail
        Intent intent = new Intent(requireContext(), AudiobookDetailActivity.class);
        intent.putExtra("audiobook_url", audiobook.getUrl());
        intent.putExtra("audiobook_title", audiobook.getTitle());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
