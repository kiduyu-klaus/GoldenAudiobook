package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Audiobook;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Search functionality
 */
public class SearchViewModel extends ViewModel {

    private final AudiobookRepository repository;
    private final MutableLiveData<List<Audiobook>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> currentQuery = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasSearched = new MutableLiveData<>(false);
    private final MutableLiveData<String> nextPageUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);

    private int currentPage = 1;

    public SearchViewModel() {
        this.repository = new AudiobookRepository();
    }

    public LiveData<List<Audiobook>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getCurrentQuery() {
        return currentQuery;
    }

    public LiveData<Boolean> getHasSearched() {
        return hasSearched;
    }

    public LiveData<String> getNextPageUrl() {
        return nextPageUrl;
    }

    public LiveData<Boolean> getIsLoadingMore() {
        return isLoadingMore;
    }

    /**
     * Search for audiobooks with the given query
     */
    public void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            error.setValue("Please enter a search term");
            return;
        }

        String trimmedQuery = query.trim();
        currentQuery.setValue(trimmedQuery);
        currentPage = 1;
        isLoading.setValue(true);
        error.setValue(null);
        hasSearched.setValue(true);
        nextPageUrl.setValue(null);

        repository.searchAudiobooks(trimmedQuery, new AudiobookRepository.DataCallback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                isLoading.postValue(false);
                if (result.isEmpty()) {
                    error.postValue("No results found for \"" + trimmedQuery + "\"");
                    searchResults.postValue(result);
                } else {
                    error.postValue(null);
                    searchResults.postValue(result);
                }
                // After initial search, check for next page
                checkForNextPage(trimmedQuery, 1);
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                error.postValue("Search failed: " + e.getMessage());
            }
        });
    }

    /**
     * Load more search results (pagination)
     */
    public void loadMore() {
        String url = nextPageUrl.getValue();
        if (url == null || url.isEmpty()) {
            return;
        }

        isLoadingMore.setValue(true);

        repository.getSearchResultsFromUrl(url, new AudiobookRepository.DataCallback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                isLoadingMore.postValue(false);

                // Append new results to existing list
                List<Audiobook> currentResults = searchResults.getValue();
                if (currentResults == null) {
                    currentResults = new ArrayList<>();
                }

                List<Audiobook> updatedResults = new ArrayList<>(currentResults);
                updatedResults.addAll(result);
                searchResults.postValue(updatedResults);

                // Check for more pages
                checkForNextPage(currentQuery.getValue(), currentPage + 1);
            }

            @Override
            public void onError(Exception e) {
                isLoadingMore.postValue(false);
                error.postValue("Failed to load more results: " + e.getMessage());
            }
        });
    }

    /**
     * Check if there are more pages available
     */
    private void checkForNextPage(String query, int page) {
        if (query == null || query.isEmpty()) {
            return;
        }

        currentPage = page;

        repository.getNextPageUrl(query, page, new AudiobookRepository.DataCallback<String>() {
            @Override
            public void onSuccess(String result) {
                nextPageUrl.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                // No more pages available
                nextPageUrl.postValue(null);
            }
        });
    }

    /**
     * Clear the current search results
     */
    public void clearResults() {
        searchResults.setValue(null);
        error.setValue(null);
        hasSearched.setValue(false);
        currentQuery.setValue(null);
        nextPageUrl.setValue(null);
        currentPage = 1;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
