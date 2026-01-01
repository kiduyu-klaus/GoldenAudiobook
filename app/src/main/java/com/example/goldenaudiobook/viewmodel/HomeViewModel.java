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

    public void loadRandomAudiobooks() {
        isLoading.setValue(true);
        error.setValue(null);

        repository.getRandomAudiobooks(new AudiobookRepository.DataCallback<List<Audiobook>>() {
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
        loadRandomAudiobooks();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
