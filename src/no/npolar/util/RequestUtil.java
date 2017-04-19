package no.npolar.util;

import java.util.*;

/**
 * Static methods for frequently used operations related to the request.
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class RequestUtil {
    
    /**
     * Pulls parameters from the given URI string and returns them neatly in a 
     * map.
     * <p>
     * If no parameters were present, an empty map is returned.
     * <p>
     * Note that each value in the returned map may contain multiple values 
     * split by e.g. the "AND" or "OR" delimiter (see {@link APIService.Delimiter#AND}).
     * 
     * @param uri The URI string to extract parameters from.
     * @return The parameters pulled from the given URI string, or an empty map if none.
     */
    public static Map<String, String> getParametersInQueryString(String uri) {
        // The parameter map
        Map<String, String> m = new HashMap<String, String>();
        // Require that there is in fact a URI to work with
        if (uri == null || uri.isEmpty()) {
            return m; // Nope => return empty map
        }
        
        // Require that there is in fact also a query string
        String queryString = getQueryString(uri);
        if (queryString.isEmpty()) {
            return m; // Nope => return empty map
        }
        
        // Loop key-value pairs
        for (String keyValPair : queryString.split("\\&")) {
            // Separate key and value, and inject into the map
            String[] keyVal = keyValPair.split("=");
            String key = keyVal[0];
            
            // Handle cases of HTML-escaped ampersand
            if (key.startsWith("amp;")) {
                key = key.substring("amp;".length());
            }
            
            // The try/catch is vital to handles cases of "key exists but not value" properly
            try {
                m.put(key, keyVal[1]);                
            } catch (Exception e) {
                m.put(key, "");
            }
        }
        
        return m;
    }
    
    /**
     * Gets the query string from the given URI, that is, the string that comes 
     * directly <strong>after</strong> the first question mark.
     * <p>
     * If no query string (or question mark) is present, an empty string is 
     * returned.
     * 
     * @param uri The URI - may or may not contain a query string part.
     * @return The query string, if any, or an empty string if none.
     */
    public static String getQueryString(String uri) {
        try {
            return uri.substring(uri.indexOf("?")).substring(1);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Alias for {@link #getQueryString(java.lang.String)}.
     */
    public static String getUriPartParameters(String uri) {
        return getQueryString(uri);
    }
    
    /**
     * Gets the base part of this filter's URI, that is, everything that comes 
     * <strong>before</strong> the first question mark in the given URI.
     * 
     * @param uri The URI to process. May or may not contain a query string part.
     * @return The base part of the given URI.
     */
    public static String getUriPartBase(String uri) {
        return uri.split("\\?")[0];
    }
    
    /**
     * Converts the given map of parameter names and values to a string.
     * <p>
     * Any map key should be the parameter name, and its associated values.
     * <p>
     * When using a string-string map, it is assumed that any multiple values
     * are combined into a single string, using a delimiter.
     * 
     * @param keyValuePairs The parameter keys and associated values.
     * @return A string representation of the the given parameter map, ready to use in a URI.
     */
    public static String getParameterString(Map<String, String> keyValuePairs) {
        String s = "";
        Iterator<String> iKeys = keyValuePairs.keySet().iterator();
        while (iKeys.hasNext()) {
            String key = iKeys.next();
            s += key + "=" + keyValuePairs.get(key) + (iKeys.hasNext() ? "&" : "");
        }
        return s;
    }
}
