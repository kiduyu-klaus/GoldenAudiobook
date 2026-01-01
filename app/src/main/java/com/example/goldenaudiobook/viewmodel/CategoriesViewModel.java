package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Category;

import java.util.List;

/**
 * ViewModel for Categories screen
 */
public class CategoriesViewModel extends ViewModel {
    private final AudiobookRepository repository;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public CategoriesViewModel() {
        repository = new AudiobookRepository();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadCategories() {
        isLoading.setValue(true);
        error.setValue(null);

        repository.getCategories(new AudiobookRepository.DataCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                categories.postValue(data);
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
