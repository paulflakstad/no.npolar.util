package no.npolar.util.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a SERP; a collection of search hits that together form the 
 * complete search result.
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute <flakstad at npolar.no>
 */
public class SearchResults {
    /** Comparator that compares hits by their score. */
    public Comparator<SearchResultHit> SCORE_COMP = new Comparator<SearchResultHit>() {
        public int compare(SearchResultHit thisResult, SearchResultHit thatResult) {
            if (thisResult.getScore() < thatResult.getScore())
                return 1;
            else if (thisResult.getScore() > thatResult.getScore())
                return -1;
            return 0;
        }
    };
    
    /** The search hits in this SERP. */
    private List<SearchResultHit> hits = null;
    
    /** The sources represented in this SERP. */
    private List<SearchSource> sources = null;
    
    /**
     * Creates a new, blank SERP.
     */
    public SearchResults() {
        hits = new ArrayList<SearchResultHit>();
        sources = new ArrayList<SearchSource>();
    }
    
    /**
     * Adds a hit to this SERP.
     * <p>
     * The source of the hit will be added to the list of sources, it necessary.
     * 
     * @param hit The hit to add.
     * @return The result of the {@link List#add(java.lang.Object)} operation.
     */
    public boolean add(SearchResultHit hit) {
        String hitSource = hit.getSource();
        if (hitSource != null) {
            hitSource = hitSource.trim();
            if (!hitSource.isEmpty()) { 
                SearchSource source = new SearchSource(hitSource);
                if (!sources.contains(source)) {
                    sources.add(source);
                }
                sources.get(sources.indexOf(source)).addOneHit();
            }
        }
        return hits.add(hit);
    }
    
    /**
     * Gets the size of this SERP (the number of hits it contains).
     * 
     * @return The size of this SERP (the number of hits it contains).
     */
    public int size() {
        return hits.size();
    }
    
    /**
     * Removes a hit from this SERP.
     * 
     * @param hit The hit to remove.
     * @return The result of the {@link List#remove(java.lang.Object)} operation.
     */
    public boolean remove(SearchResultHit hit) {
        return hits.remove(hit);
    }
    
    /**
     * Clears all hits, and their sources, from this SERP.
     */
    public void clear() {
        hits.clear();
        sources.clear();
    }
    
    /**
     * Gets an iterator for the hits in this SERP.
     * 
     * @return An iterator for the hits in this SERP.
     */
    public Iterator<SearchResultHit> iterator() {
        return hits.iterator();
    }
    
    /**
     * Gets the hits in this SERP, unsorted.
     * 
     * @return The hits in this SERP, unsorted.
     */
    public List<SearchResultHit> getHits() {
        return hits;
    }
    
    /**
     * Gets the hits in this SERP, sorted.
     * 
     * @return The hits in this SERP, sorted.
     */
    public List<SearchResultHit> getResults() {
        Collections.sort(hits, SCORE_COMP);
        return hits;
    }
    
    /**
     * Gets all sources represented in this SERP.
     * 
     * @return All sources represented in this SERP.
     */
    public List<SearchSource> getSources() {
        return sources;
    }
}