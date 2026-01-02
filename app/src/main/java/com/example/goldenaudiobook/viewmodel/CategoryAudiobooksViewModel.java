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
    private final MutableLiveData<Boolean> hasNextPage = new MutableLiveData<>(false);

    private String currentCategoryUrl;
    private String nextPageUrl;

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

    public LiveData<Boolean> getHasNextPage() {
        return hasNextPage;
    }

    public void loadCategoryAudiobooks(String categoryUrl, String name) {
        this.currentCategoryUrl = categoryUrl;
        this.categoryName.setValue(name);
        this.nextPageUrl = null;
        isLoading.setValue(true);
        error.setValue(null);
        hasNextPage.setValue(false);

        repository.getAudiobooksByCategory(categoryUrl, new AudiobookRepository.DataCallback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> data) {
                audiobooks.postValue(data);
                isLoading.postValue(false);
                // Check if there's a next page
                String nextUrl = repository.getCategoryNextPageUrl();
                hasNextPage.postValue(nextUrl != null && !nextUrl.isEmpty());
                nextPageUrl = nextUrl;
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Load next page of category audiobooks (Older Posts)
     */
    public void loadNextPage() {
        if (nextPageUrl != null && !nextPageUrl.isEmpty()) {
            isLoading.setValue(true);
            error.setValue(null);

            repository.getCategoryAudiobooksPage(
                    currentCategoryUrl,
                    nextPageUrl,
                    new AudiobookRepository.DataCallback<List<Audiobook>>() {
                        @Override
                        public void onSuccess(List<Audiobook> data) {
                            audiobooks.postValue(data);
                            isLoading.postValue(false);
                            // Check for more pages
                            String newNextUrl = repository.getCategoryNextPageUrl();
                            hasNextPage.postValue(newNextUrl != null && !newNextUrl.isEmpty());
                            nextPageUrl = newNextUrl;
                        }

                        @Override
                        public void onError(Exception e) {
                            error.postValue(e.getMessage());
                            isLoading.postValue(false);
                        }
                    });
        }
    }

    /**
     * Check if next page is available
     */
    public boolean canLoadNextPage() {
        return nextPageUrl != null && !nextPageUrl.isEmpty();
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
