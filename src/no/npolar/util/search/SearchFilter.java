package no.npolar.util.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import no.npolar.util.RequestUtil;
import org.opencms.json.JSONObject;

/**
 * Represents a search filter - a link to filter a search result or listing.
 * <p>
 * Creates as a clone of no.npolar.data.api.**** then modified to become more
 * generic.
 * <p>
 * Example usage: 
 * <ul>
 * <li>Facet filtering when interacting with the Data Centre.</li>
 * <li>Category filtering within OpenCms</li>
 * </ul>
 * <p>
 * An obvious counterpart is the "facets" field in JSON responses from the Data 
 * Centre.
 * <p>
 * A search filter is basically a <strong>term</strong>, a <strong>URI</strong> 
 * and a <strong>hit counter</strong>, plus some additional features, like:
 * <ul>
 * <li>Methods to evaluate if a filter is currently active or not</li>
 * <li>...</li>
 * </ul>
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class SearchFilter {
    protected String term = null;
    protected String title = null;
    protected int count = -1;
    protected String uri = null;
    protected String filterField = null;
    protected boolean isActive = false;
    protected String referenceUri = null;
    protected String prefix = null;
    protected String valueDelimiter = null;
    
    private Map<String, String> params = null;
    
    public static final String DEFAULT_VALUE_DELIMITER = ",";
    
    public static class Key {
        /** JSON key: Term. */
        public static final String TERM = "term";
        /** JSON key: Count. */
        public static final String COUNT = "count";
        /** JSON key: URI. */
        public static final String URI = "uri";
        /** JSON key: Title. */
        public static final String TITLE = "title";
    }
    
    /**
     * Creates a new filter for the given field, based on the given details.
     * 
     * @param filterField The field this filter is filtering on, e.g. "category". Often a facet name.
     * @param term The term for this filter.
     * @param title The displayed title/text for this filter, if different than the term.
     * @param count The number of matches for this filter.
     * @param uri The URI for this filter.
     * @param referenceUri The URI that is used to decide if this filter is active or not. Typically the URI of the currently displayed page.
     */
    public SearchFilter(String filterField, String term, String title, int count, String uri, String referenceUri) {
        if (filterField == null || term == null || count < 0 || uri == null) {
            throw new NullPointerException("Invalid constructor argument(s)."
                    + " Term and URI must be not null, and count must be non-negative.");
        }
        this.filterField = filterField;
        this.term = term;
        this.count = count;
        this.uri = uri;
        this.title = title;
        this.referenceUri = referenceUri;
        
        init();
    }
    
    /**
     * Creates a new filter for the given field, based on the given details.
     * 
     * @param filterField The field this filter is filtering on, e.g. "category". Often a facet name.
     * @param term The term for this filter.
     * @param title The displayed title/text for this filter, if different than the term.
     * @param count The number of matches for this filter.
     * @param uri The URI for this filter.
     * @param isActive Tells whether or not this filter is active.
     */
    public SearchFilter(String filterField, String term, String title, int count, String uri, boolean isActive) {
        if (filterField == null || term == null || count < 0 || uri == null) {
            throw new NullPointerException("Invalid constructor argument(s)."
                    + " Term and URI must be not null, and count must be non-negative.");
        }
        this.filterField = filterField;
        this.term = term;
        this.count = count;
        this.uri = uri;
        this.title = title;
        this.isActive = isActive;
        this.referenceUri = null;
    }
    
    /**
     * Creates a new filter for the given field, based on the given details.
     * 
     * @param filterField The field this filter is filtering on, e.g. "category". Often a facet name.
     * @param term The term for this filter.
     * @param title The displayed title/text for this filter, if different than the term.
     * @param count The number of matches for this filter.
     * @param uri The URI for this filter.
     */
    public SearchFilter(String filterField, String term, String title, int count, String uri) {
        this(filterField, term, title, count, uri, null);
    }
    
    /**
     * Creates a new filter for the given field, based on the given details.
     * 
     * @param filterField The field this filter is filtering on, e.g. "category". Often a facet name.
     * @param term The term for this filter.
     * @param count The number of matches for this filter.
     * @param uri The URI for this filter.
     */
    public SearchFilter(String filterField, String term, int count, String uri) {
        this(filterField, term, term, count, uri);
    }    
    
    /**
     * Creates a new filter for the given field, based on the given filter object.
     * 
     * @param filterField The field this filter is filtering on, e.g. "category". Often a facet name.
     * @param filterObject A JSON representation of the filter.
     */
    public SearchFilter(String filterField, JSONObject filterObject) {
        /*
        try {
            this.filterField = filterField;
            if (filterField == null)
                throw new NullPointerException("A filter field is required when creating filters.");
            
            this.term = filterObject.getString(Key.TERM);
            this.count = filterObject.getInt(Key.COUNT);
            this.uri = filterObject.getString(Key.URI);
            // Title 
            try { 
                this.title = filterObject.getString(Key.TITLE); 
            } catch (Exception ee) {
                this.title = filterObject.getString(Key.TERM);
            }
        } catch (Exception e) {
            throw new NullPointerException("Invalid JSON object."
                    + " Term and URI must be not null, and count must be non-negative.");
        }
        
        init();
        //*/
    }
    
    /**
     * Creates a new filter for the given field, based on the given filter object
     * and with a state evaluated against the given service URI.
     * 
     * @param filterField The field this filter is filtering on (e.g. "category") - normally the facet name.
     * @param filterObject A JSON representation of the filter.
     * @param referenceUri The URI that is used to decide if this filter is active or not. Typically the URI of the currently displayed page.
     */
    public SearchFilter(String filterField, JSONObject filterObject, String referenceUri) {
        /*
        try {
            this.filterField = filterField;
            if (filterField == null)
                throw new NullPointerException("A filter field is required when creating filters.");
            
            this.term = filterObject.getString(Key.TERM);
            this.count = filterObject.getInt(Key.COUNT);
            
            //
            // NOTE: examples in the next two comments (about referenceUri & uri)
            // are based on 2 topic filters being active on the current page: 
            // "marine" and "biology"
            //
            
            // The URI of the current page (or: the currently active filtering)
            // Will contain "filter-topics=marine,biology"
            this.referenceUri = referenceUri;
            
            // The URI for this filter, which when requested will either 
            //  - enable (if currently inactive) this filter, OR
            //  - disable (if currently active) this filter.
            //
            // The (non-active) filter for enabling "ecology" will contain "filter-topics=marine,biology,ecology" 
            // The (active) filter for disabling "marine" will contain "filter-topics=biology"
            // The (active) filter for disabling "biology" will contain "filter-topics=marine"
            this.uri = filterObject.getString(Key.URI); 
            
            
        } catch (Exception e) {
            throw new NullPointerException("Invalid JSON object."
                    + " Term and URI must be not null, and count must be non-negative.");
        }
        
        //System.out.println("Created filter: " + filterField + "." + term + (isActive ? " (ACTIVE)" : ""));
        //System.out.println("  URL is " + uri);
        init();
        //*/
    }
    
    /**
     * Sets a field prefix that this filter will use in its parameter name, if 
     * applicable.
     * <p>
     * The given string will prefix the filter field when constructing the 
     * associated parameter name, and in the assessment of pre-existing values 
     * in the reference URI's query string. 
     * <p>
     * Useful for example in Data Centre filters, which need a prefix of 
     * "filter-", resulting in filter parameter names like "filter-topic" or 
     * "filter-type" (for filters that apply to the "topic" or "type" fields).
     * 
     * @param prefix The prefix to use, often "filter-".
     * @return This instance, updated.
     * @see no.npolar.data.api.APIService.Param#MOD_FILTER
     */
    public SearchFilter setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }
    
    /**
     * Gets the field prefix used by this filter.
     * 
     * @return The field prefix used by this filter, e.g. "filter-", or an empty string if none.
     */
    public String getPrefix() {
        return prefix == null ? "" : prefix;
    }
    /**
     * Gets the parameter name for this filter.
     * <p>
     * If no prefix has been set, it will be equivalent to the name of the 
     * filter field, e.g. "topic".
     * <p>
     * Otherwise, the prefix will be used, e.g. "filter-topic".
     * 
     * @return The parameter name for this filter.
     */
    public String getParameterName() {
        return getPrefix().concat(filterField);
    }
    
    /**
     * Gets the value string for this filter, containing 0-N values.
     * <p>
     * E.g. "marine" or "marine,biology,geology" (assuming this filter filters 
     * on a field "topic" or similar).
     * 
     * @return The value string for this filter, containing 0-N values (empty string if none).
     */
    public String getValueString() {
        try {
            return params.get(getParameterName());
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Gets a list of the individual values for this filter.
     * 
     * @return a list of the individual values for this filter.
     */
    public List<String> getValues() {
        return getValues(getValueString(), getValueDelimiter());
    }
    
    /**
     * Performs additional initialization on the filter, like setting start=0
     * and evaluating the state (on/off).
     */
    private void init() {
        valueDelimiter = DEFAULT_VALUE_DELIMITER;
        params = RequestUtil.getParametersInQueryString(uri);
        // Evaluate state: is this filter currently active?
        if (referenceUri != null) {
            try {
                isActive = isActiveFor(referenceUri);
            } catch (Exception e) {
                throw new NullPointerException("Unable to evaluate state: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sets the value delimiter.
     * 
     * @param delimiter The delimiter to use for separating multiple values in this filter.
     * @return This instance, updated.
     */
    public SearchFilter setValueDelimiter(String delimiter) {
        valueDelimiter = delimiter;
        return this;
    }
    
    /**
     * Gets the value delimiter.
     * 
     * @return The value delimiter. Unless manually set, {@link #DEFAULT_VALUE_DELIMITER} is returned.
     */
    public String getValueDelimiter() {
        return valueDelimiter;
    }
    
    /**
     * Evaluates the active state of this filter, when using the given URI as 
     * reference.
     * 
     * @param uri The reference URI, typically the "current" URI.
     * @return <code>true</code> if this filter is evaluated as active in the given reference URI, <code>false</code> otherwise.
     */
    public boolean isActiveFor(String uri) {
        
        // Comparing parameter values for this filter with the reference URI:
        //  * If this filter is NOT ACTIVE
        //      => Then it will typically have 1 MORE value
        //  * If this filter is ACTIVE, 
        //      => Then it will typically have 1 LESS value (possibly none)
        //
        // This is true because any active filter should normally link to its 
        // own removal.
        
        boolean eval = false;
        
        String filterValue = getValueString();
        String referenceValue = RequestUtil.getParametersInQueryString(uri).get(getParameterName());
                
        if (filterValue.isEmpty()) {
            if (!referenceValue.isEmpty()) {
                // ACTIVE: Removes all values
                eval = true;
            } else {
                // Both empty
                // EQUAL: Filter does nothing! Active or inactive? -You tell me!
            }
        } else {
            List<String> valuesInThisFilter = getValues();
            List<String> valuesInReference = getValues(referenceValue, getValueDelimiter());

            if (valuesInThisFilter.size() > valuesInReference.size()) {
                // Size diff means filter probably adds one value
                valuesInReference.removeAll(valuesInThisFilter);
                if (valuesInReference.isEmpty()) {
                    // NOT ACTIVE: Adds more value(s)
                } else {
                    // HMMM. That's...unexpected...
                }
            } else if (valuesInThisFilter.size() < valuesInReference.size()) {
                // Size idff means filter probably removes one value
                valuesInThisFilter.removeAll(valuesInReference);
                if (valuesInThisFilter.isEmpty()) {
                    // ACTIVE: Removes value(s).
                    eval = true;
                } else {
                    // NOT ACTIVE: Alters the value altogether.
                }
            } else {
                // Equal in size...
                if (Collections.disjoint(valuesInReference, valuesInThisFilter)) {
                    // no elements in common =>
                    // NOT ACTIVE: Filter will alter the existing value.
                } else {
                    // element(s) in common
                    if (valuesInReference.containsAll(valuesInThisFilter)) {
                        // EQUAL: Filter does nothing! Active or inactive? -You tell me!
                    } else {
                        // "HALF-ACTIVE": This filter sucks! Active or inactive? -You tell me!
                    }
                }
            }
        }
        return eval;
    }
    
    /**
     * Splits all values in the given string on {@link #DEFAULT_VALUE_DELIMITER 
     * the default delimiter}, by injecting the result of a 
     * {@link String#split(java.lang.String)} into a new, mutable list.
     * 
     * @param values 0-N values, as a string - possibly delimited by {@link #DEFAULT_VALUE_DELIMITER the default delimiter}.
     * @return All values in the given string, or an empty list if none.
     * @see #getValues(String, String)
     */
    public static List<String> getValues(String values) {
        return getValues(values, DEFAULT_VALUE_DELIMITER);
    }
    
    /**
     * Splits all values in the given string on the given delimiter, by 
     * injecting the result of a {@link String#split(java.lang.String)} into a 
     * new, mutable list.
     * 
     * @param values 0-N values, as a string - possibly delimited by the given delimiter.
     * @param delimiter The delimiter character.
     * @return All values in the given string, or an empty list if none.
     * @see #getValues(String)
     */
    public static List<String> getValues(String values, String delimiter) {
        if (values == null || values.trim().length() == 0) {
            return new ArrayList<String>(0);
        }
        return new ArrayList<String>(
                Arrays.asList(values.split(Pattern.quote(delimiter)))
        );
    }
    
    /**
     * Removes a given parameter from the filter's URI.
     * 
     * @param paramName The parameter name.
     * @return The filter URI, with the parameter identified by the given name removed.
     */
    public SearchFilter removeParam(String paramName) {
        params.remove(paramName);
        updateUri();
        return this;
    }
    
    /**
     * Updates the URI, based on the parameters currently set.
     * <p>
     * This method must be invoked to reflect any changes in the parameters.
     * 
     */
    protected void updateUri() {
        uri = getUrlPartBase() + (!params.isEmpty() ? ("?" + RequestUtil.getParameterString(params)) : "");
    }
    
    /**
     * Adds a parameter (key-value pair).
     * <p>
     * Note: Overwrites any preexisting parameter using the same key.
     * 
     * @param key The parameter key
     * @param value The parameter value
     * @return This instance, updated.
     */
    public SearchFilter addParam(String key, String value) {
        params.put(key, value);
        updateUri();
        return this;
    }
    
    /**
     * Sets the "base URL" (everything preceding the query string) for this 
     * filter.
     * <p>
     * For example, calling setBaseUrl("http://bar.org/kek") will change this 
     * filter's base URL from the preexisting 
     * http://foo.com/lol?x=y to 
     * http://bar.org/kek?x=y
     * 
     * @param baseUrl The new base URL.
     * @return This instance, updated.
     */
    public SearchFilter setBaseUrl(String baseUrl) {
        String queryString = RequestUtil.getQueryString(uri);
        uri = baseUrl + (!queryString.isEmpty() ? "?".concat(queryString) : "");
        /*String[] uriAndParams = uri.split("\\?");
        uri = baseUrl + "?" + uriAndParams[1];*/
        return this;
    }
    
    /**
     * Gets the URL for this filter.
     * 
     * @return The URL for this filter.
     */
    public String getUrl() {
        return uri;
    }
    
    /**
     * Gets the parameter string from the filter's URI, that is, everything
     * after the first ? in the URI.
     * 
     * @return The parameter string from this filter's URI.
     */
    public String getUrlPartParameters() {
        return RequestUtil.getQueryString(uri);
        //return uri.split("\\?")[1];
    }
    
    /**
     * Gets the base part of this filter's URI, that is, everything before the
     * first ? in the URI.
     * 
     * @return The base part of this filter's URI.
     */
    public String getUrlPartBase() {
        return RequestUtil.getUriPartBase(uri);
    }
    
    /**
     * Gets the filter's term.
     * 
     * @return The filter's term.
     */
    public String getTerm() {
        return term;
    }
    
    /**
     * Gets the number of matches for this filter.
     * 
     * @return The number of matches for this filter.
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Gets a flag indicating whether or not this filter is active.
     * 
     * @return True if this filter is active, false if not.
     */
    public boolean isActive() {
        return this.isActive;
    }
    
    /**
     * Gets an HTML representation of this filter.
     * 
     * @return An HTML representation of this filter.
     * @see #toHtml(java.lang.String, java.lang.String) 
     */
    public String toHtml() {
        return toHtml(null, "filter--active");
    }
    
    /**
     * Gets an HTML representation of this filter, using the given class name 
     * (if any) and additionally an appended "active" class name (if any) if the
     * filter is active.
     * 
     * @param className The regular class name, e.g. "filter".
     * @param classNameActive The "active" class name, e.g. "filter--active".
     * @return An HTML representation of this filter, ready to use.
     */
    public String toHtml(String className, String classNameActive) {
        String cn = className == null ? "" : className;
        String cna = classNameActive == null ? "" : classNameActive;
        
        // ToDo: Make this an add / remove filter, based on the current uri
        
        return 
                "<a"
                + " class=\"" + (cn + (isActive() ? (" "+cna) : "")).trim() + "\""
                + " href=\"" + uri + "\">"
                    + term
                + "</a>";    
    }
}
