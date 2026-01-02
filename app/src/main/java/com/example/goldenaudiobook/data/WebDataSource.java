package com.example.goldenaudiobook.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.goldenaudiobook.model.Audiobook;
import com.example.goldenaudiobook.model.Category;
import com.example.goldenaudiobook.model.NavItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data source for scraping content from goldenaudiobook.net using JSoup
 */
public class WebDataSource {
    private static final String TAG = "WebDataSource";
    private static final String BASE_URL = "https://goldenaudiobook.net/";
    private static final int TIMEOUT = 30000;

    private final ExecutorService executor;
    private final Handler mainHandler;

    public WebDataSource() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for async operations
     */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Fetch random audiobooks for home page
     */
    public void getRandomAudiobooks(Callback<List<Audiobook>> callback) {
        executor.execute(() -> {
            try {
                List<Audiobook> audiobooks = new ArrayList<>();

                // Try to fetch from main page first
                Document doc = Jsoup.connect(BASE_URL)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                        .get();

                // Parse posts - try multiple selectors
                Elements posts = doc.select("div.pt-cv-content-item");

                if (posts.isEmpty()) {
                    posts = doc.select("li.arpw-li.arpw-clearfix");
                }

                Log.i(TAG, "getRandomAudiobooks: " + posts.size());

                for (Element post : posts) {
                    Audiobook audiobook = parseAudiobookFromPost(post, "random");
                    if (audiobook != null && audiobook.getTitle() != null) {
                        audiobooks.add(audiobook);
                    }
                }

                // If no posts found from archive, try alternate selectors
                if (audiobooks.isEmpty()) {
                    audiobooks = parseFromAlternateSelectors(doc);
                }

                List<Audiobook> finalAudiobooks = audiobooks;
                mainHandler.post(() -> callback.onSuccess(finalAudiobooks));
            } catch (IOException e) {
                Log.e(TAG, "Error fetching random audiobooks", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    /**
     * Fetch audiobooks by category
     */
    public void getAudiobooksByCategory(String categoryUrl, Callback<List<Audiobook>> callback) {
        executor.execute(() -> {
            try {
                List<Audiobook> audiobooks = new ArrayList<>();

                Document doc = Jsoup.connect(categoryUrl)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                        .get();

                // Updated selectors based on the HTML structure
                Elements posts = doc.select("article[id^=post-]");
                if (posts.isEmpty()) {
                    posts = doc.select("li.ilovewp-post");
                }
                if (posts.isEmpty()) {
                    posts = doc.select("article.post");
                }
                Log.i(TAG, "getAudiobooksByCategory: "+ posts.size());
                for (Element post : posts) {
                    Audiobook audiobook = parseAudiobookFromPost(post, "category");
                    if (audiobook != null && audiobook.getTitle() != null) {
                        audiobooks.add(audiobook);
                    }
                }

                mainHandler.post(() -> callback.onSuccess(audiobooks));
            } catch (IOException e) {
                Log.e(TAG, "Error fetching category audiobooks", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Parse audiobook from post element based on audiobookcard.txt structure
     */


    private Audiobook parseAudiobookFromPost(Element post, String location) {
        try {
            Audiobook audiobook = new Audiobook();

            switch (location) {
                case "category":
                    parseCategoryPost(post, audiobook);
                    break;
                case "home":
                    parseHomePost(post, audiobook);
                    break;
                case "random":
                    parseRandomPost(post, audiobook);
                    break;
                case "search":
                    parseSearchPost(post, audiobook);
                    break;
                default:
                    // Default parsing logic
                    parseCategoryPost(post, audiobook);
                    break;
            }

            // Parse author from title if present (common across all locations)
            if (audiobook.getTitle() != null && audiobook.getTitle().contains("–")) {
                String[] parts = audiobook.getTitle().split("–");
                if (parts.length > 1) {
                    audiobook.setAuthor(parts[0].trim());
                }
            }

            return audiobook;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing audiobook from post at location: " + location, e);
            return null;
        }
    }

    private void parseHomePost(Element post, Audiobook audiobook) {
    }

    private void parseRandomPost(Element post, Audiobook audiobook) {
        // Parse title from .pt-cv-title a
        String imageurlhd="";
        Element titleElement = post.selectFirst(".pt-cv-title a");
        if (titleElement != null) {
            String title = titleElement.text().trim().replace("Audiobook", "");
            String url = titleElement.attr("href");
            audiobook.setTitle(title);
            //imageurlhd=getImageUrlHd(url);
            audiobook.setUrl(url);
        }

        // Parse image from .pt-cv-thumbnail or a.pt-cv-href-thumbnail img
        Element imgElement = post.selectFirst("img.pt-cv-thumbnail");
        Log.i(TAG, "parseRandomPost: "+imgElement);
        if (imgElement == null) {
            imgElement = post.selectFirst("a.pt-cv-href-thumbnail img");
            if (!imageurlhd.isEmpty()) {
                audiobook.setImageUrl(imageurlhd);
            }
        }

        if (imgElement != null) {
            if (!imageurlhd.isEmpty()) {
                audiobook.setImageUrl(imageurlhd);
            } else {
                //String imageUrl = getHighestResolutionImage(imgElement);
                String imageUrl = imgElement.attr("data-src");
                Log.i(TAG, "parseRandomPost imageUrl: " + imageUrl);
                if (imageUrl.isEmpty()) {
                    imageUrl = imgElement.attr("src");
                }

                if (imageUrl.contains("SL500")){
                    Log.i(TAG, "contains SL500: ");
                    audiobook.setImageUrl(removeDimensions(imageUrl));
                } else {
                    audiobook.setImageUrl(removeDimensions(imageUrl));
                }
            }
        }


        // Set default values for random/home page posts
        audiobook.addCategory("Featured");
        audiobook.setPublishedDate("Recent");
    }

    private String getImageUrlHd(String url) {
        try {
            // Fetch the detail page
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                    .get();

            // Parse image - get highest resolution from srcset
            Element imgElement = doc.selectFirst(".post-single img, .entry-content img, .post-cover img");
            if (imgElement != null) {
                String imageUrl = getHighestResolutionImage(imgElement);

                Log.i(TAG, "getImageUrlHd: "+imageUrl);
                return imageUrl;
            }

            return ""; // Return empty string if no image found

        } catch (IOException e) {
            Log.e(TAG, "Error fetching HD image from URL: " + url, e);
            return ""; // Return empty string on error
        }
    }

    private void parseCategoryPost(Element post, Audiobook audiobook) {
        try {
            String imageurlhd="";
            // Parse title from h2.title-post a
            Element titleElement = post.selectFirst("h2.title-post a");
            if (titleElement != null) {
                String title = titleElement.text().trim().replace("Audiobook", "").trim();
                String url = titleElement.attr("href");
                audiobook.setTitle(title);
                //imageurlhd=getImageUrlHd(url);
                audiobook.setUrl(url);
            }

            // Parse image from .post-cover img
            Element imgElement = post.selectFirst("img");
            Log.i(TAG, "parseAudiobookFromPost imgElement: "+imgElement);
            if (imgElement != null ) {
                if (!imageurlhd.isEmpty()) {
                    audiobook.setImageUrl(imageurlhd);
                } else {
                    String src = imgElement.attr("data-src");

                    if (src.contains("SL500")){
                        Log.i(TAG, "contains SL500: ");
                        audiobook.setImageUrl(removeDimensions(src));
                    } else {
                        audiobook.setImageUrl(removeDimensions(src));
                    }
                }
            }

            // Parse categories from .post-meta-category a
            Elements categoryElements = post.select(".post-meta-category a[rel=category tag]");
            if (!categoryElements.isEmpty()) {
                for (Element cat : categoryElements) {
                    audiobook.addCategory(cat.text());
                }
            } else {
                audiobook.addCategory("Uncategorized");
            }

            // Parse date from time.entry-date
            Element dateElement = post.selectFirst("time.entry-date");
            if (dateElement != null) {
                String date = dateElement.text().trim();
                audiobook.setPublishedDate(date);
            }

            // Parse author from title if present (format: "Author – Book Title")
            if (audiobook.getTitle() != null && audiobook.getTitle().contains("–")) {
                String[] parts = audiobook.getTitle().split("–");
                if (parts.length > 1) {
                    audiobook.setAuthor(parts[0].trim());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing audiobook from post", e);
        }
    }

    public static String removeDimensions(String url) {
        return url.replaceAll("-(\\d+)x(\\d+)(?=\\.[^.]+$)", "");
    }

    /**
     * Fetch audiobook details from detail page
     */
    public void getAudiobookDetails(String url, Callback<Audiobook> callback) {
        executor.execute(() -> {
            try {
                Document doc = Jsoup.connect(url)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                        .get();

                Audiobook audiobook = new Audiobook();

                // Parse title
                Element titleElement = doc.selectFirst("h1.title-page, h1.entry-title, .post-single h1");
                if (titleElement != null) {
                    //Chris Wooding – The Ember Blade Audiobook
                    Log.i(TAG, "getAudiobookDetails: "+titleElement.text().trim());
                    String[] titleauthor = titleElement.text().trim().split("–");
                    String title = titleauthor[1].trim();
                    String author = titleauthor[0].trim();

                    audiobook.setTitle(title.replace("Audiobook","").trim());
                    audiobook.setAuthor(author.trim());
                }

                // Parse image
                // Parse image - get highest resolution from srcset
                Element imgElement = doc.selectFirst(".post-single img, .entry-content img, .post-cover img");
                if (imgElement != null) {
                    String imageUrl = getHighestResolutionImage(imgElement);
                    audiobook.setImageUrl(imageUrl);
                }

                // Parse description/content
                Element contentElement = doc.selectFirst(".post-single .collapseomatic_content, .entry-content, .post-content");
                if (contentElement != null) {
                    // Get text content, limiting length
                    String text = contentElement.text();
                    if (text.length() > 500) {
                        text = text.substring(0, 497) + "...";
                    }
                    audiobook.setDescription(text);
                }

                // Parse audio URLs from audio elements
                Elements audioElements = doc.select("audio source[src]");
                List<String> audioUrls = new ArrayList<>();
                List<String> trackNames = new ArrayList<>();

                for (int i = 0; i < audioElements.size(); i++) {
                    Element audioSrc = audioElements.get(i);
                    String src = audioSrc.attr("src");
                    if (!src.isEmpty()) {
                        audioUrls.add(src);
                        trackNames.add("Track " + (i + 1));
                    }
                }

                // Also check for direct audio src attributes
                if (audioUrls.isEmpty()) {
                    Elements directAudio = doc.select("audio[src]");
                    for (Element audio : directAudio) {
                        String src = audio.attr("src");
                        if (!src.isEmpty() && !audioUrls.contains(src)) {
                            audioUrls.add(src);
                            trackNames.add("Track " + audioUrls.size());
                        }
                    }
                }

                // Look for iframe embedded players
                if (audioUrls.isEmpty()) {
                    Elements iframes = doc.select("iframe[src*='audio'], iframe[src*='player']");
                    for (Element iframe : iframes) {
                        String src = iframe.attr("src");
                        Log.d(TAG, "Found iframe: " + src);
                        // Extract audio URL from iframe if possible
                    }
                }

                audiobook.setAudioUrls(audioUrls);
                audiobook.setTrackNames(trackNames);

                // Parse categories from meta
                Elements categoryElements = doc.select(".post-meta-category a[rel=category tag]");
                for (Element cat : categoryElements) {
                    audiobook.addCategory(cat.text());
                }

                // Parse author
//                Element authorElement = doc.selectFirst(".post-meta a[rel=author], .author-name");
//                if (authorElement != null) {
//                    audiobook.setAuthor(authorElement.text().trim());
//                }

                // Parse date
                Element dateElement = doc.selectFirst(".entry-date, time[datetime]");
                if (dateElement != null) {
                    audiobook.setPublishedDate(dateElement.text().trim());
                }

                audiobook.setUrl(url);

                mainHandler.post(() -> callback.onSuccess(audiobook));
            } catch (IOException e) {
                Log.e(TAG, "Error fetching audiobook details", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private String getHighestResolutionImage(Element imgElement) {
        String srcset = imgElement.attr("srcset");
        Log.i(TAG, "srcset: "+srcset);

        if (srcset != null && !srcset.isEmpty()) {
            String[] sources = srcset.split(",");
            String highestResUrl = "";
            int highestWidth = 0;

            for (String source : sources) {
                source = source.trim();
                String[] parts = source.split("\\s+"); // Split by whitespace

                if (parts.length >= 2) {
                    String url = parts[0];
                    String widthStr = parts[1].replace("w", "");

                    try {
                        int width = Integer.parseInt(widthStr);
                        if (width > highestWidth) {
                            highestWidth = width;
                            highestResUrl = url;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing width from srcset", e);
                    }
                }
            }

            if (!highestResUrl.isEmpty()) {
                Log.i(TAG, "getHighestResolutionImage: "+highestResUrl);
                return highestResUrl;
            }
        }

        // Fallback to src or data-src if srcset is not available
        String src = imgElement.attr("data-src");
        if (src.isEmpty()) {
            src = imgElement.attr("src");
        }

        return src;
    }

    /**
     * Fetch all categories from the website
     */
    public void getCategories(Callback<List<Category>> callback) {
        executor.execute(() -> {
            try {
                List<Category> categories = new ArrayList<>();

                Document doc = Jsoup.connect(BASE_URL + "category/")
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                        .get();

                Elements categoryItems = doc.select("ul ul li.cat-item, li.cat-item");
                if (categoryItems.isEmpty()) {
                    categoryItems = doc.select(".categories-list li, .cat-item");
                }

                for (Element item : categoryItems) {
                    Element link = item.selectFirst("a");
                    if (link != null) {
                        String name = link.text();
                        String url = link.attr("href");
                        if (!name.isEmpty() && !url.isEmpty()) {
                            categories.add(new Category(name, url));
                        }
                    }
                }

                // If no categories found, create default categories based on common audiobook genres
                if (categories.isEmpty()) {
                    categories = getDefaultCategories();
                }

                List<Category> finalCategories = categories;
                mainHandler.post(() -> callback.onSuccess(finalCategories));
            } catch (IOException e) {
                Log.e(TAG, "Error fetching categories", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Fetch navigation items from nav.txt structure
     */
    public void getNavigationItems(Callback<List<NavItem>> callback) {
        executor.execute(() -> {
            List<NavItem> navItems = new ArrayList<>();

            // Based on nav.txt structure
            NavItem home = new NavItem("Home", BASE_URL, false);
            home.setIcon("home");
            navItems.add(home);

            NavItem bestsellers = new NavItem("Bestsellers", BASE_URL + "category/bestsellers/", true);
            bestsellers.setCategory(true);
            navItems.add(bestsellers);

            NavItem action = new NavItem("Action", BASE_URL + "category/action/", true);
            action.setCategory(true);
            navItems.add(action);

            NavItem fantasy = new NavItem("Fantasy", BASE_URL + "category/audio-fantasy/", true);
            fantasy.setCategory(true);
            navItems.add(fantasy);

            // Create parent item for Harry Potter
            NavItem harryPotter = new NavItem("Harry Potter", BASE_URL + "?s=harry+potter", false);
            NavItem jimDale = new NavItem("Jim Dale", BASE_URL + "?s=harry+potter+jim+dale", false);
            NavItem stephenFry = new NavItem("Stephen Fry", BASE_URL + "?s=harry+potter+stephen+fry", false);
            harryPotter.addSubItem(jimDale);
            harryPotter.addSubItem(stephenFry);
            navItems.add(harryPotter);

            // Create parent item for Fifty Shades
            NavItem fiftyShades = new NavItem("Fifty Shades", BASE_URL + "?s=shades+", false);
            NavItem grey = new NavItem("Grey", BASE_URL + "grey-e-l-james/", false);
            NavItem darker = new NavItem("Darker", BASE_URL + "e-l-james-darker-audiobook/", false);
            NavItem freed = new NavItem("Freed", BASE_URL + "freed-fifty-shades-freed-as-told-by-christian-audiobook/", false);
            fiftyShades.addSubItem(grey);
            fiftyShades.addSubItem(darker);
            fiftyShades.addSubItem(freed);
            navItems.add(fiftyShades);

            NavItem romance = new NavItem("Romance", BASE_URL + "category/audiobooks-romance/", true);
            romance.setCategory(true);
            navItems.add(romance);

            NavItem mystery = new NavItem("Mystery", BASE_URL + "category/mystery/", true);
            mystery.setCategory(true);
            navItems.add(mystery);

            NavItem thriller = new NavItem("Thriller", BASE_URL + "category/thriller/", true);
            thriller.setCategory(true);
            navItems.add(thriller);

            mainHandler.post(() -> callback.onSuccess(navItems));
        });
    }

    /**
     * Search for audiobooks by query
     * URL format: https://goldenaudiobook.net/?s=search+term
     */
    public void getSearchResultsAudiobooks(String searchQuery, Callback<List<Audiobook>> callback) {
        executor.execute(() -> {
            try {
                List<Audiobook> audiobooks = new ArrayList<>();

                // Construct search URL - replace spaces with +
                String encodedQuery = searchQuery.replace(" ", "+");
                String searchUrl = BASE_URL + "?s=" + encodedQuery;

                Log.d(TAG, "Searching for: " + searchQuery + " at URL: " + searchUrl);

                Document doc = Jsoup.connect(searchUrl)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Mobile Safari/537.36")
                        .get();

                // Parse posts from search results - use the structure from search results
                Elements posts = doc.select("li.ilovewp-post");
                if (posts.isEmpty()) {
                    posts = doc.select("article[id^=post-]");
                }
                if (posts.isEmpty()) {
                    posts = doc.select("article.post");
                }
                if (posts.isEmpty()) {
                    posts = doc.select(".search-result, .result-item");
                }

                Log.d(TAG, "Search found " + posts.size() + " posts for query: " + searchQuery);

                for (Element post : posts) {
                    Audiobook audiobook = parseAudiobookFromPost(post, "search");
                    if (audiobook != null && audiobook.getTitle() != null && !audiobook.getTitle().isEmpty()) {
                        audiobooks.add(audiobook);
                    }
                }

                // If no results found, try alternate selectors
                if (audiobooks.isEmpty()) {
                    Log.d(TAG, "No results found with primary selector, trying alternatives");
                    audiobooks = parseFromAlternateSelectors(doc);
                }

                List<Audiobook> finalAudiobooks = audiobooks;
                mainHandler.post(() -> callback.onSuccess(finalAudiobooks));
            } catch (IOException e) {
                Log.e(TAG, "Error searching for audiobooks: " + searchQuery, e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Parse audiobook from search results
     * Based on the search results HTML structure
     */
    private void parseSearchPost(Element post, Audiobook audiobook) {
        try {
            // Parse title from h2.title-post a (same as category structure)
            Element titleElement = post.selectFirst("h2.title-post a");
            if (titleElement == null) {
                // Try alternate selectors for search results
                titleElement = post.selectFirst(".entry-title a, .post-title a, h2 a");
            }

            if (titleElement != null) {
                String title = titleElement.text().trim().replace("Audiobook", "");
                String url = titleElement.attr("href");
                audiobook.setTitle(title);
                audiobook.setUrl(url);
                Log.d(TAG, "Search result title: " + title);
            }

            // Parse image from .post-cover img or img attachment
            Element imgElement = post.selectFirst("img");
            if (imgElement != null) {
                String imageUrl = imgElement.attr("data-src");
                if (imageUrl.isEmpty()) {

                    imageUrl = imgElement.attr("src");
                }
                if (imageUrl.contains("SL500")){
                    Log.i(TAG, "contains SL500: ");
                    audiobook.setImageUrl(removeDimensions(imageUrl));
                } else {
                    audiobook.setImageUrl(removeDimensions(imageUrl));
                    Log.d(TAG, "Search result image: " + removeDimensions(imageUrl));
                }
            }

            // Parse categories from .post-meta-category a
            Elements categoryElements = post.select(".post-meta-category a[rel=category tag]");
            if (!categoryElements.isEmpty()) {
                for (Element cat : categoryElements) {
                    audiobook.addCategory(cat.text());
                }
            } else {
                audiobook.addCategory("Search Result");
            }

            // Parse date from time.entry-date
            Element dateElement = post.selectFirst("time.entry-date");
            if (dateElement == null) {
                dateElement = post.selectFirst(".posted-on time, .entry-date");
            }

            if (dateElement != null) {
                String date = dateElement.text().trim();
                audiobook.setPublishedDate(date);
            }

            // Parse author from title if present (format: "Author – Book Title")
            if (audiobook.getTitle() != null && audiobook.getTitle().contains("–")) {
                String[] parts = audiobook.getTitle().split("–");
                if (parts.length > 1) {
                    audiobook.setAuthor(parts[0].trim());
                }
            }

            Log.d(TAG, "Parsed search result: " + audiobook.getTitle());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing search result post", e);
        }
    }

    /**
     * Parse from alternate selectors when main selector fails
     */
    private List<Audiobook> parseFromAlternateSelectors(Document doc) {
        List<Audiobook> audiobooks = new ArrayList<>();

        // Try different selectors
        Elements posts = doc.select(".arpw-random-post li, .random-post li, .widget li");
        for (Element post : posts) {
            Audiobook audiobook = parseAudiobookFromPost(post,"random");
            if (audiobook != null && audiobook.getTitle() != null) {
                audiobooks.add(audiobook);
            }
        }

        return audiobooks;
    }

    /**
     * Get default categories based on categories.txt structure
     */
    private List<Category> getDefaultCategories() {
        List<Category> categories = new ArrayList<>();

        String[][] defaultCategories = {
                {"Action", "https://goldenaudiobook.net/category/action/"},
                {"Adults", "https://goldenaudiobook.net/category/adults-audios/"},
                {"Adventure", "https://goldenaudiobook.net/category/adventure/"},
                {"Autobiography & Biographies", "https://goldenaudiobook.net/category/autobiography-biographies/"},
                {"Bestsellers", "https://goldenaudiobook.net/category/bestsellers/"},
                {"Business", "https://goldenaudiobook.net/category/business/"},
                {"Children", "https://goldenaudiobook.net/category/children/"},
                {"Classic", "https://goldenaudiobook.net/category/classic/"},
                {"Crime", "https://goldenaudiobook.net/category/crime-audiobooks/"},
                {"Fantasy", "https://goldenaudiobook.net/category/audio-fantasy/"},
                {"Historical Fiction", "https://goldenaudiobook.net/category/historical-fiction/"},
                {"History", "https://goldenaudiobook.net/category/history/"},
                {"Horror", "https://goldenaudiobook.net/category/horror/"},
                {"Humor", "https://goldenaudiobook.net/category/humors/"},
                {"Mystery", "https://goldenaudiobook.net/category/mystery/"},
                {"Romance", "https://goldenaudiobook.net/category/audiobooks-romance/"},
                {"Sci-Fi", "https://goldenaudiobook.net/category/science-fiction-audiobooks/"},
                {"Self-help", "https://goldenaudiobook.net/category/self-help/"},
                {"Spiritual & Religious", "https://goldenaudiobook.net/category/spiritual-religious/"},
                {"Teen & Young Adult", "https://goldenaudiobook.net/category/teen-and-young-adult/"},
                {"Thriller", "https://goldenaudiobook.net/category/thriller/"},
                {"Westerns", "https://goldenaudiobook.net/category/westerns/"}
        };

        for (String[] cat : defaultCategories) {
            categories.add(new Category(cat[0], cat[1]));
        }

        return categories;
    }

    /**
     * Shutdown the executor
     */
    public void shutdown() {
        executor.shutdown();
    }
}
