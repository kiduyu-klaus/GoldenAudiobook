package com.example.goldenaudiobook.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Audiobook model class representing an audiobook item scraped from goldenaudiobook.net
 */
public class Audiobook implements Serializable {
    private String id;
    private String title;
    private String url;
    private String imageUrl;
    private String author;
    private String authorUrl;
    private String description;
    private String publishedDate;
    private List<String> categories;
    private List<String> audioUrls;
    private List<String> trackNames;

    public Audiobook() {
        this.categories = new ArrayList<>();
        this.audioUrls = new ArrayList<>();
        this.trackNames = new ArrayList<>();
    }

    public Audiobook(String title, String url, String imageUrl) {
        this();
        this.title = title;
        this.url = url;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public void addCategory(String category) {
        if (this.categories == null) {
            this.categories = new ArrayList<>();
        }
        this.categories.add(category);
    }

    public List<String> getAudioUrls() {
        return audioUrls;
    }

    public void setAudioUrls(List<String> audioUrls) {
        this.audioUrls = audioUrls;
    }

    public void addAudioUrl(String audioUrl) {
        if (this.audioUrls == null) {
            this.audioUrls = new ArrayList<>();
        }
        this.audioUrls.add(audioUrl);
    }

    public List<String> getTrackNames() {
        return trackNames;
    }

    public void setTrackNames(List<String> trackNames) {
        this.trackNames = trackNames;
    }

    public void addTrackName(String trackName) {
        if (this.trackNames == null) {
            this.trackNames = new ArrayList<>();
        }
        this.trackNames.add(trackName);
    }

    public int getAudioTrackCount() {
        return audioUrls != null ? audioUrls.size() : 0;
    }

    public String getDisplayAuthor() {
        if (author != null && !author.isEmpty()) {
            return author;
        }
        // Extract author from title if not set
        if (title != null && title.contains("–")) {
            return title.split("–")[0].trim();
        }
        return "Unknown Author";
    }

    public String getDisplayTitle() {
        if (title != null && title.contains("–")) {
            return title.split("–")[1].trim();
        }
        return title;
    }

    @Override
    public String toString() {
        return "Audiobook{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Audiobook audiobook = (Audiobook) o;
        return url != null && url.equals(audiobook.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
