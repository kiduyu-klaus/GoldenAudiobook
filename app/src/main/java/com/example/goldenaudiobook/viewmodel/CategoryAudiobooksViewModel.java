package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Audiobook;

import java.util.List;

/**
 * ViewModel for Category Audiobooks screen
 */
public class CategoryAudiobooksViewModel extends ViewModel {
    private final AudiobookRepository repository;

    private final MutableLiveData<List<Audiobook>> audiobooks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> categoryName = new MutableLiveData<>();

    private String currentCategoryUrl;

    public CategoryAudiobooksViewModel() {
        repository = new AudiobookRepository();
    }

    public LiveData<List<Audiobook>> getAudiobooks() {
        return audiobooks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getCategoryName() {
        return categoryName;
    }

    public void loadCategoryAudiobooks(String categoryUrl, String name) {
        this.currentCategoryUrl = categoryUrl;
        this.categoryName.setValue(name);
        isLoading.setValue(true);
        error.setValue(null);

        repository.getAudiobooksByCategory(categoryUrl, new AudiobookRepository.DataCallback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> data) {
                audiobooks.postValue(data);
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void refresh() {
        if (currentCategoryUrl != null) {
            loadCategoryAudiobooks(currentCategoryUrl, categoryName.getValue());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
