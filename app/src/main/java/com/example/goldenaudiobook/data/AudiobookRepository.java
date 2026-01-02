package com.example.goldenaudiobook.data;

import android.util.Log;

import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.Category;
import com.example.goldenaudiobook.model.NavItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for audiobook data - abstracts data source from the rest of the app
 */
public class AudiobookRepository {
    private static final String TAG = "AudiobookRepository";

    private final WebDataSource webDataSource;

    public AudiobookRepository() {
        this.webDataSource = new WebDataSource();
    }

    /**
     * Callback interface for async operations
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    /**
     * Get random audiobooks for home page
     */
    public void getRandomAudiobooks(DataCallback<List<Audiobook>> callback) {
        webDataSource.getRandomAudiobooks(new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting random audiobooks", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get random audiobooks by page URL
     */
    public void getRandomAudiobooksPage(String url, DataCallback<List<Audiobook>> callback) {
        webDataSource.getRandomAudiobooksPage(url, new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting random audiobooks page", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get home page pagination info
     */
    public boolean hasNextHomePage() {
        return webDataSource.hasNextPage();
    }

    /**
     * Get home page previous URL
     */
    public String getPreviousHomePageUrl() {
        return webDataSource.getPreviousPageUrl();
    }

    /**
     * Get home page current page
     */
    public int getCurrentHomePage() {
        return webDataSource.getCurrentPage();
    }

    /**
     * Get home page total pages
     */
    public int getTotalHomePages() {
        return webDataSource.getTotalPages();
    }

    /**
     * Get audiobooks by category URL
     */
    public void getAudiobooksByCategory(String categoryUrl, DataCallback<List<Audiobook>> callback) {
        webDataSource.getAudiobooksByCategory(categoryUrl, new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting category audiobooks", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get all audiobooks by an author
     */
    public void getAuthorAllResultsAudiobooks(String authorUrl, DataCallback<List<Audiobook>> callback) {
        webDataSource.getAuthorAllResultsAudiobooks(authorUrl, new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting author audiobooks", e);
                callback.onError(e);
            }
        });
    }


    /**
     * Get audiobooks by category with specific page URL
     */
    public void getCategoryAudiobooksPage(String categoryUrl, String pageUrl, DataCallback<List<Audiobook>> callback) {
        webDataSource.getAudiobooksByCategoryPage(categoryUrl, pageUrl, new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting category audiobooks page", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get category next page URL
     */
    public String getCategoryNextPageUrl() {
        return webDataSource.getCategoryNextPageUrl();
    }

    /**
     * Get audiobook details
     */
    public void getAudiobookDetails(String url, DataCallback<Audiobook> callback) {
        webDataSource.getAudiobookDetails(url, new WebDataSource.Callback<Audiobook>() {
            @Override
            public void onSuccess(Audiobook result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting audiobook details", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get all categories
     */
    public void getCategories(DataCallback<List<Category>> callback) {
        webDataSource.getCategories(new WebDataSource.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting categories", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get navigation items
     */
    public void getNavigationItems(DataCallback<List<NavItem>> callback) {
        webDataSource.getNavigationItems(new WebDataSource.Callback<List<NavItem>>() {
            @Override
            public void onSuccess(List<NavItem> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting navigation items", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Search audiobooks by query
     */
    public void searchAudiobooks(String query, DataCallback<List<Audiobook>> callback) {
        webDataSource.getSearchResultsAudiobooks(query, new WebDataSource.Callback<List<Audiobook>>() {
            @Override
            public void onSuccess(List<Audiobook> result) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error searching audiobooks", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        webDataSource.shutdown();
    }
}
