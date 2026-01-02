package com.example.goldenaudiobook.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.AudioTrack;
import com.example.goldenaudiobook.service.AudioPlaybackService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * ViewModel for Floating Player - shared across all fragments
 * Manages playback state and persists data for app restarts
 */
@UnstableApi public class FloatingPlayerViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "floating_player_prefs";
    private static final String KEY_CURRENT_AUDIOBOOK_URL = "current_audiobook_url";
    private static final String KEY_CURRENT_AUDIOBOOK_TITLE = "current_audiobook_title";
    private static final String KEY_CURRENT_AUDIOBOOK_AUTHOR = "current_audiobook_author";
    private static final String KEY_CURRENT_AUDIOBOOK_IMAGE = "current_audiobook_image";
    private static final String KEY_CURRENT_TRACK_INDEX = "current_track_index";
    private static final String KEY_PLAYBACK_POSITION = "playback_position";
    private static final String KEY_IS_PLAYING = "is_playing";
    private static final String KEY_AUDIO_URLS = "audio_urls";
    private static final String KEY_TRACK_NAMES = "track_names";

    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> isPlayerVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<Audiobook> currentAudiobook = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentTrackIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<List<AudioTrack>> trackList = new MutableLiveData<>(new ArrayList<>());

    private AudioPlaybackService playbackService;
    private boolean serviceBound = false;

    public FloatingPlayerViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        restoreState();
    }

    /**
     * Restore playback state from SharedPreferences
     */
    private void restoreState() {
        String audiobookUrl = prefs.getString(KEY_CURRENT_AUDIOBOOK_URL, null);
        if (audiobookUrl != null) {
            String title = prefs.getString(KEY_CURRENT_AUDIOBOOK_TITLE, "");
            String author = prefs.getString(KEY_CURRENT_AUDIOBOOK_AUTHOR, "");
            String imageUrl = prefs.getString(KEY_CURRENT_AUDIOBOOK_IMAGE, "");
            int trackIndex = prefs.getInt(KEY_CURRENT_TRACK_INDEX, 0);
            long position = prefs.getLong(KEY_PLAYBACK_POSITION, 0);

            Audiobook audiobook = new Audiobook();
            audiobook.setUrl(audiobookUrl);
            audiobook.setTitle(title);
            audiobook.setAuthor(author);
            audiobook.setImageUrl(imageUrl);

            // Restore audio URLs and track names
            String audioUrlsStr = prefs.getString(KEY_AUDIO_URLS, "");
            String trackNamesStr = prefs.getString(KEY_TRACK_NAMES, "");

            if (!audioUrlsStr.isEmpty()) {
                List<String> audioUrls = parseStringList(audioUrlsStr);
                List<String> trackNames = parseStringList(trackNamesStr);
                audiobook.setAudioUrls(audioUrls);
                audiobook.setTrackNames(trackNames);

                List<AudioTrack> tracks = new ArrayList<>();
                for (int i = 0; i < audioUrls.size(); i++) {
                    String name = (trackNames != null && i < trackNames.size())
                            ? trackNames.get(i)
                            : "Track " + (i + 1);
                    tracks.add(new AudioTrack(i + 1, name, audioUrls.get(i)));
                }
                trackList.postValue(tracks);
            }

            currentAudiobook.postValue(audiobook);
            currentTrackIndex.postValue(trackIndex);
            currentPosition.postValue(position);
            isPlayerVisible.postValue(true);
        }
    }

    private List<String> parseStringList(String str) {
        List<String> result = new ArrayList<>();
        if (str != null && !str.isEmpty()) {
            String[] parts = str.split("\\|@@\\|");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }
        }
        return result;
    }

    /**
     * Save current playback state to SharedPreferences
     */
    public void saveState() {
        Audiobook book = currentAudiobook.getValue();
        if (book != null) {
            prefs.edit()
                    .putString(KEY_CURRENT_AUDIOBOOK_URL, book.getUrl())
                    .putString(KEY_CURRENT_AUDIOBOOK_TITLE, book.getTitle())
                    .putString(KEY_CURRENT_AUDIOBOOK_AUTHOR, book.getAuthor())
                    .putString(KEY_CURRENT_AUDIOBOOK_IMAGE, book.getImageUrl())
                    .putInt(KEY_CURRENT_TRACK_INDEX, currentTrackIndex.getValue() != null ? currentTrackIndex.getValue() : 0)
                    .putLong(KEY_PLAYBACK_POSITION, currentPosition.getValue() != null ? currentPosition.getValue() : 0)
                    .putBoolean(KEY_IS_PLAYING, isPlaying.getValue() != null ? isPlaying.getValue() : false)
                    .apply();

            // Save audio URLs and track names
            List<AudioTrack> tracks = trackList.getValue();
            if (tracks != null && !tracks.isEmpty()) {
                StringBuilder audioUrls = new StringBuilder();
                StringBuilder trackNames = new StringBuilder();
                for (int i = 0; i < tracks.size(); i++) {
                    if (i > 0) {
                        audioUrls.append("|@@|");
                        trackNames.append("|@@|");
                    }
                    audioUrls.append(tracks.get(i).getUrl());
                    trackNames.append(tracks.get(i).getTitle());
                }
                prefs.edit()
                        .putString(KEY_AUDIO_URLS, audioUrls.toString())
                        .putString(KEY_TRACK_NAMES, trackNames.toString())
                        .apply();
            }
        }
    }

    /**
     * Clear saved state (when playback is stopped)
     */
    public void clearState() {
        prefs.edit().clear().apply();
        isPlayerVisible.postValue(false);
        currentAudiobook.postValue(null);
        trackList.postValue(new ArrayList<>());
    }

    // Getters for LiveData
    public LiveData<Boolean> getIsPlayerVisible() { return isPlayerVisible; }
    public LiveData<Boolean> getIsExpanded() { return isExpanded; }
    public LiveData<Audiobook> getCurrentAudiobook() { return currentAudiobook; }
    public LiveData<Integer> getCurrentTrackIndex() { return currentTrackIndex; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<List<AudioTrack>> getTrackList() { return trackList; }

    // Setters for updating state
    public void setPlayerVisible(boolean visible) {
        isPlayerVisible.setValue(visible);
    }

    public void setExpanded(boolean expanded) {
        isExpanded.setValue(expanded);
    }

    public void setCurrentAudiobook(Audiobook audiobook) {
        currentAudiobook.setValue(audiobook);
        isPlayerVisible.setValue(audiobook != null);

        if (audiobook != null && audiobook.getAudioUrls() != null) {
            List<AudioTrack> tracks = new ArrayList<>();
            List<String> trackNames = audiobook.getTrackNames();
            for (int i = 0; i < audiobook.getAudioUrls().size(); i++) {
                String name = (trackNames != null && i < trackNames.size())
                        ? trackNames.get(i)
                        : "Track " + (i + 1);
                tracks.add(new AudioTrack(i + 1, name, audiobook.getAudioUrls().get(i)));
            }
            trackList.setValue(tracks);
        } else {
            trackList.setValue(new ArrayList<>());
        }
    }

    public void setCurrentTrackIndex(int index) {
        currentTrackIndex.setValue(index);
    }

    public void setIsPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    public void setCurrentPosition(long position) {
        currentPosition.setValue(position);
    }

    public void setDuration(long dur) {
        duration.setValue(dur);
    }

    public void setTrackList(List<AudioTrack> tracks) {
        trackList.setValue(tracks);
    }

    // Playback controls
    public void togglePlayPause() {
        if (playbackService != null && serviceBound) {
            playbackService.togglePlayPause();
        }
    }

    public void playTrack(int trackIndex) {
        if (playbackService != null && serviceBound) {
            playbackService.playTrack(trackIndex);
        }
    }

    public void seekTo(long position) {
        if (playbackService != null && serviceBound) {
            playbackService.seekTo(position);
        }
    }

    public void playNextTrack() {
        if (playbackService != null && serviceBound) {
            playbackService.playNextTrack();
        }
    }

    public void playPreviousTrack() {
        if (playbackService != null && serviceBound) {
            playbackService.playPreviousTrack();
        }
    }

    public void stopPlayback() {
        if (playbackService != null && serviceBound) {
            playbackService.stopPlayback();
        }
        clearState();
    }

    /**
     * Resume playback from saved state
     */
    public void resumePlayback() {
        Audiobook book = currentAudiobook.getValue();
        if (book != null && book.getAudioUrls() != null && !book.getAudioUrls().isEmpty() && playbackService != null && serviceBound) {
            Integer trackIndex = currentTrackIndex.getValue();
            Long position = currentPosition.getValue();
            int index = trackIndex != null ? trackIndex : 0;
            long pos = position != null ? position : 0;

            playbackService.loadAudiobook(book, index);
            if (pos > 0) {
                playbackService.seekTo(pos);
            }
            playbackService.play();
        }
    }

    /**
     * Check if the given audiobook matches the current playing audiobook
     */
    public boolean isCurrentAudiobook(Audiobook audiobook) {
        if (audiobook == null || currentAudiobook.getValue() == null) {
            return false;
        }
        String currentUrl = currentAudiobook.getValue().getUrl();
        String newUrl = audiobook.getUrl();
        return currentUrl != null && newUrl != null && currentUrl.equals(newUrl);
    }

    /**
     * Get the current track index, returns 0 if null
     */
    public int getCurrentTrackIndexSafe() {
        Integer index = currentTrackIndex.getValue();
        return index != null ? index : 0;
    }

    /**
     * Get the current position, returns 0 if null
     */
    public long getCurrentPositionSafe() {
        Long position = currentPosition.getValue();
        return position != null ? position : 0L;
    }

    /**
     * Check if playback was playing before
     */
    public boolean wasPlaying() {
        Boolean playing = isPlaying.getValue();
        return playing != null && playing;
    }

    // Service binding
    public void setPlaybackService(AudioPlaybackService service) {
        this.playbackService = service;

        // If we have saved state and service just connected, try to restore playback
        // Use post to ensure state is fully restored first
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (service != null && hasSavedState()) {
                restorePlaybackFromState();
            }
        }, 200);
    }

    public void setServiceBound(boolean bound) {
        this.serviceBound = bound;

        // If service just became bound and we have saved state, restore playback
        if (bound && playbackService != null && hasSavedState()) {
            // Use post to ensure state is fully restored
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (hasSavedState()) {
                    restorePlaybackFromState();
                }
            }, 200);
        }
    }

    /**
     * Restore playback from saved state
     */
    public void restorePlaybackFromState() {
        if (playbackService == null || !serviceBound) {
            Log.e("FloatingPlayerVM", "Cannot restore playback: service not bound");
            return;
        }

        Audiobook book = currentAudiobook.getValue();
        if (book == null || book.getAudioUrls() == null || book.getAudioUrls().isEmpty()) {
            Log.e("FloatingPlayerVM", "Cannot restore playback: no audiobook or audio URLs");
            // Try to rebuild from SharedPreferences directly
            if (hasSavedState()) {
                Log.d("FloatingPlayerVM", "Trying to rebuild audiobook from SharedPreferences");
                rebuildAndRestoreFromPrefs();
            }
            return;
        }

        Integer trackIndex = currentTrackIndex.getValue();
        Long position = currentPosition.getValue();
        int index = trackIndex != null ? trackIndex : 0;
        long pos = position != null ? position : 0;
        Boolean playing = isPlaying.getValue();

        Log.d("FloatingPlayerVM", "Restoring playback - track: " + index + ", position: " + pos + ", wasPlaying: " + playing);

        // Load audiobook into service
        playbackService.loadAudiobook(book, index);

        // Restore position and playing state
        if (pos > 0) {
            playbackService.seekTo(pos);
        }

        if (playing != null && playing) {
            playbackService.play();
        }
    }

    /**
     * Rebuild audiobook from SharedPreferences and restore playback
     */
    private void rebuildAndRestoreFromPrefs() {
        String audioUrlsStr = prefs.getString(KEY_AUDIO_URLS, "");
        String trackNamesStr = prefs.getString(KEY_TRACK_NAMES, "");

        if (audioUrlsStr.isEmpty()) {
            Log.e("FloatingPlayerVM", "Cannot rebuild: no audio URLs in prefs");
            return;
        }

        List<String> audioUrls = parseStringList(audioUrlsStr);
        List<String> trackNames = trackNamesStr.isEmpty() ? new ArrayList<>() : parseStringList(trackNamesStr);

        String title = prefs.getString(KEY_CURRENT_AUDIOBOOK_TITLE, "");
        String author = prefs.getString(KEY_CURRENT_AUDIOBOOK_AUTHOR, "");
        String url = prefs.getString(KEY_CURRENT_AUDIOBOOK_URL, "");
        String imageUrl = prefs.getString(KEY_CURRENT_AUDIOBOOK_IMAGE, "");
        int trackIndex = prefs.getInt(KEY_CURRENT_TRACK_INDEX, 0);
        long position = prefs.getLong(KEY_PLAYBACK_POSITION, 0);
        boolean wasPlaying = prefs.getBoolean(KEY_IS_PLAYING, false);

        Audiobook audiobook = new Audiobook();
        audiobook.setUrl(url);
        audiobook.setTitle(title);
        audiobook.setAuthor(author);
        audiobook.setImageUrl(imageUrl);
        audiobook.setAudioUrls(audioUrls);
        audiobook.setTrackNames(trackNames);

        // Update ViewModel state
        currentAudiobook.postValue(audiobook);
        currentTrackIndex.postValue(trackIndex);
        currentPosition.postValue(position);
        isPlayerVisible.postValue(true);

        // Build track list
        List<AudioTrack> tracks = new ArrayList<>();
        for (int i = 0; i < audioUrls.size(); i++) {
            String name = (trackNames != null && i < trackNames.size())
                    ? trackNames.get(i)
                    : "Track " + (i + 1);
            tracks.add(new AudioTrack(i + 1, name, audioUrls.get(i)));
        }
        trackList.postValue(tracks);

        // Restore playback
        Log.d("FloatingPlayerVM", "Rebuilt audiobook, restoring playback");
        if (playbackService != null && serviceBound) {
            playbackService.loadAudiobook(audiobook, trackIndex);
            if (position > 0) {
                playbackService.seekTo(position);
            }
            if (wasPlaying) {
                playbackService.play();
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Only save state if there's actual playback data
        Audiobook book = currentAudiobook.getValue();
        if (book != null && book.getUrl() != null) {
            saveState();
        }
    }

    /**
     * Factory for creating FloatingPlayerViewModel
     */
    public static class Factory extends ViewModelProvider.AndroidViewModelFactory {
        Application application;
        public Factory(@NonNull Application application) {
            super(application);
            this.application = application;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(FloatingPlayerViewModel.class)) {
                return (T) new FloatingPlayerViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    /**
     * Check if there is saved state to restore
     */
    public boolean hasSavedState() {
        String audiobookUrl = prefs.getString(KEY_CURRENT_AUDIOBOOK_URL, null);
        return audiobookUrl != null && !audiobookUrl.isEmpty();
    }

    /**
     * Get saved track index for restoration
     */
    public int getSavedTrackIndex() {
        return prefs.getInt(KEY_CURRENT_TRACK_INDEX, 0);
    }

    /**
     * Get saved position for restoration
     */
    public long getSavedPosition() {
        return prefs.getLong(KEY_PLAYBACK_POSITION, 0);
    }

    /**
     * Check if playback was saved as playing
     */
    public boolean wasSavedAsPlaying() {
        return prefs.getBoolean(KEY_IS_PLAYING, false);
    }
}
