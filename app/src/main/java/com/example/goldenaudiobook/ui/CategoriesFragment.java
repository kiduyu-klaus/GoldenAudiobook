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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.adapter.CategoryAdapter;
import com.example.goldenaudiobook.databinding.FragmentCategoriesBinding;
import com.example.goldenaudiobook.model.Category;
import com.example.goldenaudiobook.viewmodel.CategoriesViewModel;

/**
 * Categories Fragment displaying all audiobook categories in a grid
 */
public class CategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private FragmentCategoriesBinding binding;
    private CategoriesViewModel viewModel;
    private CategoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);

        setupRecyclerView();
        observeViewModel();

        // Load data
        viewModel.loadCategories();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter(this);

        // Use GridLayoutManager with 2 columns for phones
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                adapter.submitList(categories);
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
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
    }

    @Override
    public void onCategoryClick(Category category) {
        // Navigate to category audiobooks
        Bundle args = new Bundle();
        args.putString("categoryUrl", category.getUrl());
        args.putString("categoryName", category.getName());

//        CategoryAudiobooksFragment fragment = new CategoryAudiobooksFragment();
//        fragment.setArguments(args);  // Set arguments on the fragment

        Navigation.findNavController(requireView())
                .navigate(R.id.categoryAudiobooksFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
