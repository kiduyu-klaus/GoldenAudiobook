package com.example.goldenaudiobook.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.AudiobookAdapter;
import com.example.goldenaudiobook.databinding.FragmentCategoryAudiobooksBinding;
import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.viewmodel.CategoryAudiobooksViewModel;

/**
 * Fragment displaying audiobooks for a specific category
 */
public class CategoryAudiobooksFragment extends Fragment implements AudiobookAdapter.OnAudiobookClickListener {

    private FragmentCategoryAudiobooksBinding binding;
    private CategoryAudiobooksViewModel viewModel;
    private AudiobookAdapter adapter;

    private String categoryUrl;
    private String categoryName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryUrl = getArguments().getString("categoryUrl");
            categoryName = getArguments().getString("categoryName");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryAudiobooksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CategoryAudiobooksViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();

        // Load data
        if (categoryUrl != null) {
            viewModel.loadCategoryAudiobooks(categoryUrl, categoryName);
        }
    }

    private void setupToolbar() {
        if (categoryName != null) {
            binding.toolbar.setTitle(categoryName);
        }
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

        viewModel.getCategoryName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) {
                binding.toolbar.setVisibility(View.GONE);
                //binding.toolbar.setTitle(name);
            } else {
                binding.toolbar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAudiobookClick(Audiobook audiobook) {
        // Navigate to audiobook detail
        android.content.Intent intent = new android.content.Intent(requireContext(), AudiobookDetailActivity.class);
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
