package com.example.goldenaudiobook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.goldenaudiobook.data.AudiobookRepository;
import com.example.goldenaudiobook.model.Audiobook;

/**
 * ViewModel for Audiobook Detail screen
 */
public class AudiobookDetailViewModel extends ViewModel {
    private final AudiobookRepository repository;

    private final MutableLiveData<Audiobook> audiobook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentTrackIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);

    public AudiobookDetailViewModel() {
        repository = new AudiobookRepository();
    }

    public LiveData<Audiobook> getAudiobook() {
        return audiobook;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Integer> getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    public LiveData<Long> getCurrentPosition() {
        return currentPosition;
    }

    public LiveData<Long> getDuration() {
        return duration;
    }

    public void loadAudiobookDetails(String url) {
        isLoading.setValue(true);
        error.setValue(null);

        repository.getAudiobookDetails(url, new AudiobookRepository.DataCallback<Audiobook>() {
            @Override
            public void onSuccess(Audiobook data) {
                audiobook.postValue(data);
                isLoading.postValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    public void setPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    public void setCurrentTrack(int index) {
        currentTrackIndex.setValue(index);
    }

    public void updatePlaybackPosition(long position) {
        currentPosition.setValue(position);
    }

    public void setDuration(long dur) {
        duration.setValue(dur);
    }

    public void playNextTrack() {
        Audiobook book = audiobook.getValue();
        if (book != null && book.getAudioUrls() != null) {
            Integer current = currentTrackIndex.getValue();
            if (current != null && current < book.getAudioUrls().size() - 1) {
                currentTrackIndex.setValue(current + 1);
            }
        }
    }

    public void playPreviousTrack() {
        Integer current = currentTrackIndex.getValue();
        if (current != null && current > 0) {
            currentTrackIndex.setValue(current - 1);
        }
    }

    public boolean hasNextTrack() {
        Audiobook book = audiobook.getValue();
        if (book == null || book.getAudioUrls() == null) return false;
        Integer current = currentTrackIndex.getValue();
        return current != null && current < book.getAudioUrls().size() - 1;
    }

    public boolean hasPreviousTrack() {
        Integer current = currentTrackIndex.getValue();
        return current != null && current > 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
