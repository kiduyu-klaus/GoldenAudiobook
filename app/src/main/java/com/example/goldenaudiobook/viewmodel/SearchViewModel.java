package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Audiobook;

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
        isLoading.setValue(true);
        error.setValue(null);
        hasSearched.setValue(true);

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
            }

            @Override
            public void onError(Exception e) {
                isLoading.postValue(false);
                error.postValue("Search failed: " + e.getMessage());
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
    }
}
