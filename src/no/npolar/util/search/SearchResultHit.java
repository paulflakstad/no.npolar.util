package no.npolar.util.search;

import java.util.Comparator;

/**
 * Represents a single hit in a SERP.
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class SearchResultHit implements Comparator<SearchResultHit> {
    private String title = null;
    private String snippet = null;
    private String category = null;
    private String uri = null;
    private String displayUri = null;
    private String source = null;
    private int score = -1;
    
    public static final int TITLE = 0;
    public static final int SNIPPET = 1;
    public static final int CATEGORY = 2;
    public static final int URI = 3;
    public static final int DISPLAY_URI = 4;
    public static final int SOURCE = 5;
    public static final int SCORE = 6;
        
    /*public static final Comparator<SearchResultHit> SCORE_COMP = new Comparator<SearchResultHit>() {
        public int compare(SearchResultHit thisResult, SearchResultHit thatResult) {
            if (thisResult.getScore() < thatResult.getScore())
                return -1;
            else if (thisResult.getScore() > thatResult.getScore())
                return 1;
            return 0;
        }
    };*/
    
    /**
     * Creates a new hit, using the given details.
     * 
     * @param title The title.
     * @param snippet The snippet.
     * @param category The category (if any).
     * @param uri The URI.
     * @param displayUri The URI to show the end-user.
     * @param source The source in which this hit was found.
     * @param score The score, typically a number between 1 (worst) and 100 (best).
     */
    public SearchResultHit(String title, 
                            String snippet,
                            String category,
                            String uri,
                            String displayUri,
                            String source,
                            int score) {
        this.title = title;
        this.snippet = snippet;
        this.category = category;
        this.uri = uri;
        this.displayUri = displayUri;
        this.source = source;
        this.score = score;
    }

    /**
     * Gets a detail from this hit - usage like Calendar#get(Calendar#DATE)
     * 
     * @param type The type - pre-defined by this class, e.g. {@link #TITLE}.
     * @return The detail (if any).
     */
    public String get(int type) {
        switch (type) {
            case TITLE:
                return this.title;
            case SNIPPET:
                return this.snippet;
            case CATEGORY:
                return this.category;
            case URI:
                return this.uri;
            case DISPLAY_URI:
                return this.displayUri;
            case SOURCE:
                return this.source;
            case SCORE:
                return String.valueOf(this.score);
            default:
                return null;
        }
    }
    
    /**
     * Gets the title.
     * 
     * @return The title.
     */
    public String getTitle() { return title; }
    
    /**
     * Gets the snippet.
     * 
     * @return The snippet.
     */
    public String getSnippet() { return snippet; }
    
    /**
     * Gets the category (if any).
     * 
     * @return the category (if any).
     */
    public String getCategory() { return category; }
    
    /**
     * Gets the URI.
     * 
     * @return the URI.
     */
    public String getUri() { return uri; }
    
    /**
     * Gets the source.
     * 
     * @return The source.
     */
    public String getSource() { return source; }
    
    /**
     * Gets the score.
     * 
     * @return the score.
     */
    public int getScore() { return score; }
    
    /**
     * Compares two instances by score.
     * 
     * @param thisResult
     * @param thatResult
     * @return 
     */
    @Override
    public int compare(SearchResultHit thisResult, SearchResultHit thatResult) {
        if (thisResult.getScore() < thatResult.getScore())
            return -1;
        else if (thisResult.getScore() > thatResult.getScore())
            return 1;
        return 0;
    }
}
