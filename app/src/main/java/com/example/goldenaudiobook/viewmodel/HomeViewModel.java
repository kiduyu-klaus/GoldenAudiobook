package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Audiobook;

import java.util.List;

/**
 * ViewModel for Home screen displaying random audiobooks
 */
public class HomeViewModel extends ViewModel {
    private final AudiobookRepository repository;

    private final MutableLiveData<List<Audiobook>> audiobooks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> totalPages = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> hasNextPage = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasPreviousPage = new MutableLiveData<>(false);

    public HomeViewModel() {
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

    public LiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public LiveData<Integer> getTotalPages() {
        return totalPages;
    }

    public LiveData<Boolean> getHasNextPage() {
        return hasNextPage;
    }

    public LiveData<Boolean> getHasPreviousPage() {
        return hasPreviousPage;
    }

    public void loadRandomAudiobooks() {
        isLoading.setValue(true);
        error.setValue(null);

        repository.getRandomAudiobooks(new AudiobookRepository.DataCallback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> data) {
                audiobooks.postValue(data);
                isLoading.postValue(false);
                // Update pagination state
                currentPage.postValue(repository.getCurrentHomePage());
                totalPages.postValue(repository.getTotalHomePages());
                hasNextPage.postValue(repository.hasNextHomePage());
                hasPreviousPage.postValue(repository.getCurrentHomePage() > 1);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Load next page of random audiobooks
     */
    public void loadNextPage() {
        if (Boolean.TRUE.equals(hasNextPage.getValue())) {
            isLoading.setValue(true);
            error.setValue(null);

            repository.getRandomAudiobooksPage(
                    repository.hasNextHomePage() ? "next" : null,
                    new AudiobookRepository.DataCallback<List<Audiobook>>() {
                        @Override
                        public void onSuccess(List<Audiobook> data) {
                            audiobooks.postValue(data);
                            isLoading.postValue(false);
                            // Update pagination state
                            currentPage.postValue(repository.getCurrentHomePage());
                            totalPages.postValue(repository.getTotalHomePages());
                            hasNextPage.postValue(repository.hasNextHomePage());
                            hasPreviousPage.postValue(true);
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
     * Load previous page of random audiobooks
     */
    public void loadPreviousPage() {
        if (Boolean.TRUE.equals(hasPreviousPage.getValue())) {
            isLoading.setValue(true);
            error.setValue(null);

            String previousUrl = repository.getPreviousHomePageUrl();
            if (previousUrl != null) {
                repository.getRandomAudiobooksPage(
                        previousUrl,
                        new AudiobookRepository.DataCallback<List<Audiobook>>() {
                            @Override
                            public void onSuccess(List<Audiobook> data) {
                                audiobooks.postValue(data);
                                isLoading.postValue(false);
                                // Update pagination state
                                currentPage.postValue(repository.getCurrentHomePage());
                                totalPages.postValue(repository.getTotalHomePages());
                                hasNextPage.postValue(repository.hasNextHomePage());
                                hasPreviousPage.postValue(repository.getCurrentHomePage() > 1);
                            }

                            @Override
                            public void onError(Exception e) {
                                error.postValue(e.getMessage());
                                isLoading.postValue(false);
                            }
                        });
            }
        }
    }

    public void refresh() {
        loadRandomAudiobooks();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
