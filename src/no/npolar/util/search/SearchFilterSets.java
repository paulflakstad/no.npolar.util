package no.npolar.util.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.util.CmsStringUtil;

/**
 * Groups 0-N {@link SearchFilterSet search filter sets} together, typically so 
 * that they can be presented within the same "available filtering" section of
 * a page.
 * <p>
 * Creates as a clone of no.npolar.data.api.**** then modified to become more
 * generic.
 * <p>
 * In essence just a wrapper for the list that holds the filter set instances, 
 * created to make sorting and getting individual filter sets by name easier.
 * <p>
 * ToDo: Year filtering + toggle filter's visibility (see publications JSP)
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class SearchFilterSets {
    /** Sort order: By title. */
    public static final int SORT_ORDER_TITLE = 1;
    /** Sort order: By relevancy. */
    public static final int SORT_ORDER_RELEVANCY = 2;
    /** Holds all search filter sets */
    private List<SearchFilterSet> sets = null;
    
    /**
     * Creates a new, blank group of filter sets.
     */
    public SearchFilterSets() {
        sets = new ArrayList<SearchFilterSet>();
    }
    
    /**
     * Adds a filter set.
     * 
     * @param set
     * @return This instance, updated.
     */
    public SearchFilterSets add(SearchFilterSet set) {
        try { sets.add(set); } catch (Exception e) {}
        return this;
    }
    
    /**
     * Removes a filter set.
     * 
     * @param set
     * @return This instance, updated.
     */
    public SearchFilterSets remove(SearchFilterSet set) {
        try { sets.remove(set); } catch (Exception e) {}
        return this;
    }
    
    /**
     * Gets the filter set at the given index.
     * 
     * @param index
     * @return The filter set at the given index.
     */
    public SearchFilterSet get(int index) {
        return sets.get(index);
    }
    
    /**
     * Gets the number contained filter sets.
     * 
     * @return 
     */
    public int size() {
        return sets.size();
    }
    
    /**
     * Gets the list of contained filter sets.
     * 
     * @return 
     */
    public List<SearchFilterSet> get() {
        return sets;
    }
    
    /**
     * Gets the iterator for the list of contained filter sets.
     * 
     * @return 
     */
    public Iterator<SearchFilterSet> iterator() {
        return sets.iterator();
    }
    
    /**
     * Checks if the list of contained filter sets is empty.
     * 
     * @return 
     */
    public boolean isEmpty() {
        return sets.isEmpty();
    }
    
    /**
     * Removes a specific filter set, identified by the given name, from this list.
     * <p>
     * It is possible (though against reason) for names to be non-unique within
     * the list of filter sets. (I.e. two filter sets could have the same name.)
     * In that case, the first encountered filter set with the given name is 
     * removed.
     * 
     * @param name The name of the filter set.
     * @return The removed filter set, or null of nothing was removed.
     */
    public SearchFilterSet removeByName(String name) {
        Iterator<SearchFilterSet> i = sets.iterator();
        while (i.hasNext()) {
            try {
                SearchFilterSet sfs = i.next();
                if (sfs == null)
                    continue;
                
                if (sfs.getName().equals(name)) {
                    i.remove();
                    return sfs;
                }
            } catch (Exception e) {
                // Should NEVER happen
                return null;
            }
        }
        return null;
    }
    
    /**
     * Gets a specific filter set, identified by the given name, from this list.
     * <p>
     * It is possible (though against reason) for names to be non-unique within
     * the list of filter sets. (I.e. two filter sets could have the same name.)
     * In that case, the first encountered filter set with the given name is 
     * returned.
     * 
     * @param name The name of the filter set.
     * @return The filter set identified by the given name, or null if none.
     */
    public SearchFilterSet getByName(String name) {
        Iterator i = sets.iterator();
        while (i.hasNext()) {
            SearchFilterSet sfs = null;
            try {
                Object o = i.next();
                if (o == null)
                    continue;
                sfs = (SearchFilterSet)o;
                if (sfs.getName().equals(name)) {
                    return sfs;
                }
            } catch (Exception e) {
                // Should NEVER happen
                return null;
            }
        }
        return null;
    }
    
    /**
     * Orders this list of filter sets according to the given array which 
     * describes the order.
     * <p>
     * The (facet) name at index 0 in the array is given the highest 
     * relevancy, the name at index 1 the second highest, and so on.
     * <p>
     * Any names not mentioned in the given array will retain their existing 
     * relevancy.
     * 
     * @param namesInOrder The order description.
     * @return The list of filter sets, after the ordering modification.
     */
    public SearchFilterSets order(String ... namesInOrder) {
        int rel = 99;
        try {
            for (String name : namesInOrder) {
            //for (int i = 0; i < namesInOrder.length; i++) {
                try { 
                    //System.out.println("Trying to set relevancy=" + rel + " for filter set with name '" + namesInOrder[i] + "' ...");
                    getByName(name).setRelevancy(rel--);
                    //getByName(namesInOrder[i]).setRelevancy(rel--);
                } catch (Exception e) {} // Exception = no match on that name
            }
        } catch (Exception e) {
            // ???
        }
        sort(SORT_ORDER_RELEVANCY);
        return this;
    }
    
    /**
     * Sorts the list of filter sets according to the given sort order, which 
     * should be one of the SORT_ORDER_XXX constants of this class.
     * 
     * @param sortOrder The sort order.
     * @return This instance, updated.
     */
    public SearchFilterSets sort(int sortOrder) {
        if (sortOrder == SORT_ORDER_RELEVANCY) {
            Collections.sort(sets, SearchFilterSet.COMPARATOR_RELEVANCY);
        } else if (sortOrder == SORT_ORDER_TITLE) {
            Collections.sort(sets, SearchFilterSet.COMPARATOR_TITLE);
        }
        return this;
    }
    
    /**
     * Sorts the list of filter sets using the given comparator.
     * 
     * @param comp The comparator to use in the sort operation.
     */
    public void sort(Comparator comp) {
        Collections.sort(sets, comp);
    }
    
    /**
     * Gets the widget component representing this filter set.
     * <p>
     * For use in an existing filtering section (which typically provides 
     * toggling, wrappers, etc.).
     * 
     * @param cms An initialized CMS action element, needed to construct valid links.
     * @param labels Used for translations of filter texts, by calling {@link ResourceBundle#getString(java.lang.String)}, passing the filter text.
     * @return  the widget component representing this filter set.
     */
    public String toHtml(CmsJspActionElement cms, ResourceBundle labels) {
        String s = "";
        try {
            if (!this.isEmpty()) {
                Iterator<SearchFilterSet> iFilterSets = this.iterator();
                s += "<div class=\"layout-group quadruple layout-group--quadruple filter-widget\">";
                //s += "<div class=\"boxes\">";
                while (iFilterSets.hasNext()) {
                    SearchFilterSet filterSet = iFilterSets.next();
                    List<SearchFilter> filters = filterSet.getFilters();

                    if (filters != null) {
                        s += "<div class=\"layout-box filter-set\">";
                        s += "<h3 class=\"filters-heading filter-set__heading\">";
                        s += filterSet.getTitle();
                        s += "<span class=\"filter__num-matches\"> (" + filterSet.size() + ")</span>";
                        s += "</h3>";
                        s += "<ul class=\"filter-set__filters\">";
                        try {
                            // Iterate through the filters in this set
                            Iterator<SearchFilter> iFilters = filters.iterator();
                            while (iFilters.hasNext()) {
                                SearchFilter filter = iFilters.next();
                                // The visible filter text (initialize this as the term)
                                String filterText = filter.getTerm();

                                // Try to fetch a better (and localized) text for the filter
                                try {
                                    filterText = labels.getString( filterSet.labelKeyFor(filter) );
                                } catch (Exception skip) {}

                                // The filter
                                s += "<li><a href=\"" + cms.link(cms.getRequestContext().getUri() + "?" + CmsStringUtil.escapeHtml(filter.getUrlPartParameters())) + "\""
                                                    + " class=\"filter" + (filter.isActive() ? " filter--active" : "") + "\""
                                                    + " rel=\"nofollow\""
                                                    + ">" 
                                                    //+ (filter.isActive() ? "<span style=\"background:red; border-radius:3px; color:white; padding:0 0.3em;\" class=\"remove-filter\">X</span> " : "")
                                                    + filterText
                                                    + "<span class=\"filter__num-matches\"> (" + filter.getCount() + ")</span>"
                                                + "</a></li>";
                            }
                        } catch (Exception filterE) {
                            s += "<!-- " + filterE.getMessage() + " -->";
                        }
                        s += "</ul>";
                        s += "</div>"; // .filter-set
                    }
                }
                //s += "</div>";
                s += "</div>"; // .layout-group
            }
        } catch (Exception e) {
            s += "<!-- Error constructing filters: " + e.getMessage() + " -->";
        }
        return s;   
    }
    
    /**
     * Gets the HTML to start a toggleable filtering section.
     * 
     * @param togglerText The text to display on the toggler, e.g. "Filters".
     * @return  the HTML to end a toggleable filtering section.
     * @see #getFiltersWrapperHtmlEnd() 
     */
    public String getFiltersWrapperHtmlStart(String togglerText) {
        return "\n<div class=\"search-panel__filters\">" 
                + "\n<a"
                    + " aria-controls=\"search-filters\""
                    + " class=\"toggler toggler--filters-toggle\""
                    + " href=\"#search-filters\""
                    + " tabindex=\"0\""
                + ">" + togglerText + "</a>"
                + "\n<div"
                    + " class=\"toggleable toggleable--filters\""
                    + " id=\"search-filters\""
                + ">";
        /*return "\n<div class=\"search-widget search-widget--filters\">" 
                + "\n<a class=\"cta cta--filters-toggle\" tabindex=\"0\">" + togglerText + "</a>"
                + "\n<div class=\"filters-wrapper\">";*/
    }
    
    /**
     * Gets the HTML to end a toggleable filtering section.
     * 
     * @return  the HTML to end a toggleable filtering section.
     * @see #getFiltersWrapperHtmlStart(java.lang.String) 
     */
    public String getFiltersWrapperHtmlEnd() {
        return "\n</div>" // .toggleable
                + "\n</div>"; // .search-panel__filters
        /*return "\n</div>" // .filters-wrapper
                + "\n</div>"; // .search-widget--filters*/
    }
    
    /**
     * Gets the complete HTML for a toggleable filtering section, including the 
     * wrappers, toggler and filters, ready to use.
     * 
     * @param togglerText The text to display on the toggler, e.g. "Filters".
     * @param cms An initialized CMS action element, needed to construct valid links.
     * @param labels Used for translations of filter texts, by calling {@link ResourceBundle#getString(java.lang.String)}, passing the filter text.
     * @return the complete HTML for a toggleable filtering section, ready to use.
     * @see #toHtml(org.opencms.jsp.CmsJspActionElement, java.util.ResourceBundle) 
     */
    public String toHtml(String togglerText, CmsJspActionElement cms, ResourceBundle labels) {
        String s = "";
        try {
            s += getFiltersWrapperHtmlStart(togglerText);
            s += toHtml(cms, labels);
            s += getFiltersWrapperHtmlEnd();
        } catch (Exception e) {
            s += "<!-- Error constructing filters: " + e.getMessage() + " -->";
        }
        return s;
    }
}