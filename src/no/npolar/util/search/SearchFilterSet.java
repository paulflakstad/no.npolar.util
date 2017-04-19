package no.npolar.util.search;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
//import java.util.Locale;
//import java.util.ResourceBundle;

/**
 * Represents a set/group of search filters.
 * <p>
 * Creates as a clone of no.npolar.data.api.**** then modified to become more
 * generic.
 * <p>
 * A search filter set comprise 0-N {@link SearchFilter <em>filters</em>} that 
 * the user can apply or remove in order to limit or expand items in a listing, 
 * in particular the results of a search.
 * <p>
 * In terms of the Data Centre, filters represent facets, and each search filter 
 * set corresponds to one entry in the "facets" field.
 * <p>
 * Presentation-wise, a search filter set is basically a named (e.g. "category") 
 * list of links (the filters), plus some additional features like: 
 * <ul>
 * <li>relevancy/weight (for ordering multiple filter sets),</li> 
 * <li>comparators (for sorting)</li>
 * </ul>
 * 
 * @see SearchFilter
 * @see SearchFilterSets
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class SearchFilterSet {
    /** The filters in this set. */
    protected List<SearchFilter> filters = null;
    //** The language setting. */
    //protected Locale locale = null;
    //** Translations used in localization. */
    //protected ResourceBundle translations = null;
    /** The ("code") name of this set, for example "cat". */
    private String name = null;
    /** The ("displayed") title of this set, for example "Category". */
    private String title = null;
    /** The relevancy weight - used whenever we need to order multiple sets appearing together. */
    private int relevancyWeight = 1;
    
    /** Compares sets by title. */
    public static final Comparator<SearchFilterSet> COMPARATOR_TITLE =
            new Comparator<SearchFilterSet>() {
                @Override
                public int compare(SearchFilterSet obj1, SearchFilterSet obj2) {
                    return obj1.getTitle().compareTo(obj2.getTitle());
                }
            };
    
    /** Compares sets by relevancy weight. */
    public static final Comparator<SearchFilterSet> COMPARATOR_RELEVANCY =
            new Comparator<SearchFilterSet>() {
                @Override
                public int compare(SearchFilterSet obj1, SearchFilterSet obj2) {
                    try {
                        if (obj2.getRelevancy() > obj1.getRelevancy()) {
                            return 1;
                        } else if (obj2.getRelevancy() < obj1.getRelevancy()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    } catch (Exception e) { 
                        return 0;
                    }
                }
            };
    
    /**
     * Constructs a new filter set with the given name, and no custom title.
     * <p>
     * For Data Centre interaction, the expected name would be the facet 
     * identifier, as returned from the service. (E.g.: "topics" or 
     * "published-year_sort".)
     * 
     * @param name The name. Data Centre filter sets should use the facet identifier.
     */
    public SearchFilterSet(String name) {
        this(name, null);
    }
    
    /**
     * Constructs a new filter set with the given name and locale.
     * <p>
     * For Data Centre interaction, the expected name would be the facet 
     * identifier, as returned from the service. (E.g.: "topics" or 
     * "published-year_sort".)
     * 
     * @param name The name. Data Centre filter sets should use the facet identifier.
     * @param title The "nice name" that the end-user will see. If <code>null</code>, the name is used as title.
     */
    public SearchFilterSet(String name, String title) {
        this.name = name;
        this.title = title == null ? name : title;
        filters = new ArrayList<SearchFilter>();
    }
    
    /*
     * Constructs a new filter set with the given name and locale.
     * <p>
     * For Data Centre interaction, the expected name would be the facet 
     * identifier, as returned from the service. (E.g.: "topics" or 
     * "published-year_sort".)
     * 
     * @param name The name. Data Centre filter sets should use the facet identifier.
     * @param locale The locale to use in translations.
     *
    public SearchFilterSet(String name, Locale locale, ResourceBundle translations) {
        this(name, locale);
        this.translations = translations;
    }*/
    
    /**
     * Gets the filters in this filter set.
     * 
     * @return The filters in this filter set.
     */
    public List<SearchFilter> getFilters() {
        return filters;
    }
    
    /**
     * Gets the label key for the given filter, which is typically used for 
     * (localized) labeling.
     * <p>
     * E.g.: If this filter set is for topics (has name="topic") and the given 
     * filter is for atmosphere (has term="atmosphere"), then the returned key 
     * would be "topic.atmosphere".
     * <p>
     * The returned label key can then be used to retrieve a human-readable 
     * and/or localized string suitable for use in end-user presentation.
     * <p>
     * Example usage:
     * <pre>
     *  String labelKey = mySearchFilterSet.labelKeyFor( mySearchFilter );
     *  String niceFilterText = myLabels.getString( labelKey ); // myLabels is a ResourceBundle
     * </pre>
     * 
     * @param filter The filter to create a label key for.
     * @return The label key for the given filter.
     * @see no.npolar.data.api.Labels
     */
    public String labelKeyFor(SearchFilter filter) {
        String s = this.getName().concat(".").concat(filter.getTerm());
        /*
        try {
            s = Labels.normalizeServiceString(this.getName()).concat(".").concat(Labels.normalizeServiceString(filter.getTerm()));
        } catch (Exception e) {
            // uh-oh
        }
        //*/
        return s;
    }
    
    /**
     * Gets the name of this filter set.
     * <p>
     * When interacting with the Data Centre, the name should be identical to 
     * the facet identifier, as provided by the Data Centre's API, e.g.: 
     * "topics" or "year-published_sort").
     * 
     * @return The filter set's name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the title of this set.
     * <p>
     * If no title is set, the name is returned.
     * 
     * @return The filter set's title.
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the relevancy weight.
     * <p>
     * The relevancy weight is typically used for ordering filter sets, when 
     * many sets are to appear together.
     * <p>
     * If not modified, the filter set has relevancy weight = 1 (default).
     * 
     * @return The filter set's relevancy weight.
     */
    public int getRelevancy() {
        return relevancyWeight;
    }
    
    /**
     * Sets the relevancy weight.
     * <p>
     * The relevancy weight is typically used for ordering filter sets when 
     * many sets are to appear together.
     * <p>
     * If not modified, the filter set has relevancy weight = 1 (default).
     * 
     * @param weight The new relevancy weight.
     * @return The filter set, updated.
     */
    public SearchFilterSet setRelevancy(int weight) {
        relevancyWeight = weight;
        return this;
    }
    
    /**
     * Adds a filter to this set.
     * 
     * @param filter The filter to add.
     * @return The filter set, updated.
     * @see List#add(java.lang.Object) 
     */
    public SearchFilterSet add(SearchFilter filter) {
        filters.add(filter);
        return this;
    }
    
    /**
     * Adds a filter to this set, at the given index.
     * 
     * @param index The index at which to add the given filter.
     * @param filter The filter to add.
     * @return The filter set, updated.
     * @see List#add(int, java.lang.Object)
     */
    public SearchFilterSet add(int index, SearchFilter filter) {
        filters.add(index, filter);
        return this;
    }
    
    /**
     * Gets an iterator for the filters in this filter set.
     * 
     * @return an iterator for the filters in this filter set.
     * @see List#iterator() 
     */
    public Iterator<SearchFilter> iterator() {
        return filters.iterator();
    }
    
    /**
     * Gets the number of filters in this filter set.
     * 
     * @return the number of filters in this filter set.
     * @see List#size() 
     */
    public int size() {
        return filters.size();
    }
    
    /**
     * Adds all the given filters to this set.
     * 
     * @param filters The filters to add.
     * @return The filter set, updated.
     * @see List#addAll(java.util.Collection)  
     */
    public SearchFilterSet addAll(List<SearchFilter> filters) {
        this.filters.addAll(filters);
        return this;
    }
    
    /**
     * Removes the given filter from this filter set.
     * 
     * @param filter The filter to remove.
     * @return The filter set, updated.
     * @see List#remove(java.lang.Object) 
     */
    public SearchFilterSet remove(SearchFilter filter) {
        filters.remove(filter);
        return this;
    }
    
    /**
     * Removes all the given filters from this filter set.
     * 
     * @param filters The filters to remove.
     * @return The filter set, updated.
     * @see List#removeAll(java.util.Collection) 
     */
    public SearchFilterSet removeAll(List<SearchFilter> filters) {
        this.filters.removeAll(filters);
        return this;
    }
    
    /**
     * @see List#contains(java.lang.Object) 
     */
    public boolean contains(SearchFilter filter) {
        return filters.contains(filter);
    }
    
    /**
     * @see List#containsAll(java.util.Collection) 
     */
    public boolean containsAll(List<SearchFilter> filters) {
        return filters.containsAll(filters);
    }
    
    /**
     * @see List#isEmpty() 
     */
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    /**
     * @see List#indexOf(java.lang.Object) 
     */
    public int indexOf(SearchFilter filter) {
        return filters.indexOf(filter);
    }
    
    /**
     * Clears this filter set.
     * 
     * @return The filter set, updated.
     * @see List#clear() 
     */
    public SearchFilterSet clear() {
        filters.clear();
        return this;
    }
    
    /**
     * Gets the filter at the given index of this filter set.
     * 
     * @param index The index to lookup.
     * @return the filter at the given index of this filter set.
     * @see List#get(int) 
     */
    public SearchFilter get(int index) {
        return filters.get(index);
    }
    
    /**
     * Gets a filter set that contains a sub-list of the filters in this filter 
     * set.
     * 
     * @param startIndex The start index.
     * @param endIndex The end index.
     * @return a filter set that contains a sub-list of the filters in this filter set.
     * @see List#subList(int, int) 
     */
    public SearchFilterSet subSet(int startIndex, int endIndex) {
        SearchFilterSet set = new SearchFilterSet(title);
        set.addAll(filters.subList(startIndex, endIndex));
        return set;
    }
    
    /**
     * Gets the filters in this filter set, as an array.
     * 
     * @return the filters in this filter set, as an array.
     * @see List#toArray() 
     */
    public SearchFilter[] toArray() {
        return (SearchFilter[])filters.toArray();
    }
}
