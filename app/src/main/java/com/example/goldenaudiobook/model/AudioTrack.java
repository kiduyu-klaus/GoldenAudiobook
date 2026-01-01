package com.example.goldenaudiobook.model;

/**
 * Represents an audio track within an audiobook
 */
public class AudioTrack {
    private int trackNumber;
    private String title;
    private String url;
    private long duration;

    public AudioTrack() {
    }

    public AudioTrack(int trackNumber, String title, String url) {
        this.trackNumber = trackNumber;
        this.title = title;
        this.url = url;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return "Track " + trackNumber;
    }

    @Override
    public String toString() {
        return "AudioTrack{" +
                "trackNumber=" + trackNumber +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
