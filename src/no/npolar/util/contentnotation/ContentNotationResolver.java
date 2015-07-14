package no.npolar.util.contentnotation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;
import no.npolar.data.api.Publication;
import no.npolar.data.api.PublicationService;
import no.npolar.util.CmsAgent;

/**
 *
 * @author flakstad
 */
public class ContentNotationResolver {
    /** A list of paths to files containing globally defined notations. */
    private List<String> globalFilePaths = null;
    /** A list of all globally defined notation objects. Global notations are defined in external files. */
    private List<I_ContentNotationObject> globalEntities = null;
    /** A list of all notation objects used in the scope of this resolver. The scope is typically a single request. */
    private List<I_ContentNotationObject> scopeEntities = null;
    /** Maps text segments to their corresponding notation objects. */
    private Map<String, I_ContentNotationObject> replacementMap = null;
    
    /** Counter for unique (per page) notation objects. */
    private ContentNotationCounter counter = null;
    /** The session attribute name used to identify the notation resolver object. */
    public static final String SESS_ATTR_NAME = "cn_resolver";
    
    /**
     * Creates a new content notation resolver, ready to use.</p>
     * <p>The resolver is typically created <i>(and often loaded with global definitions)</i> 
     * near the top in the master template, then stored in the session <i>(so all 
     * detail templates can access it)</i>, then cleared and removed from the session 
     * near the bottom of the master template.</p>
     * <p><strong>Note:</strong> When global definition files are used, loadGlobals(...) should be 
     * invoked immediately after creating the resolver.
     * 
     * @see ContentNotationResolver#loadGlobals(no.npolar.util.CmsAgent, java.lang.String) 
     */
    public ContentNotationResolver() {
        // Initialize lists and counters
        globalEntities = new ArrayList<I_ContentNotationObject>();
        scopeEntities = new ArrayList<I_ContentNotationObject>();
        replacementMap = new LinkedHashMap<String, I_ContentNotationObject>();
        globalFilePaths = new ArrayList<String>();
        counter = new ContentNotationCounter();
    }
    
    /**
     * Checks if the given string has the syntax of a UUID.
     * 
     * @param maybeUUID
     * @return 
     */
    protected boolean isUUID(String maybeUUID) {
        if (maybeUUID == null || maybeUUID.isEmpty())
            return false;
        
        return Pattern.compile("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}").matcher(maybeUUID).find();
    }
    
    /**
     * Resolves content notations in the given string.
     * <p>
     * This method will modify the given source by replacing the notations with 
     * actual HTML code.
     * 
     * @param source The string to resolve content notations for.
     * @return The given string, modified with resolved content notations.
     */
    public synchronized String resolve(String source) {
        String s = source;

        // Regex pattern that will match any type of notation
        Pattern p = Pattern.compile(TooltipNotation.REGEX_PATTERN + "|" + ReferenceNotation.REGEX_PATTERN + "|" + IndexNotation.REGEX_PATTERN);
        Matcher m = p.matcher(s);

        // Start search ...
        while (m.find()) {

            String notationString = s.substring(m.start(), m.end());
            
            // Use the found string to create the notation object
            I_ContentNotationObject notationObject = ContentNotationObject.create(notationString);
            // Update any attributes that exist (for example ID)
            resolveAttributes(notationObject, notationString);
            // Set a default ID, if necessary
            if (notationObject.getId() == null) {
                notationObject.setId(notationObject.getDefaultIdPrefix() + counter.getCountForType(notationObject)); // Not safe to incrementCountForType yet, so use getCountForType() on the counter here
            }
            
            
            if (notationObject.isAllowedReoccur()) {
                // Check scope list - is it already there?
                I_ContentNotationObject existingNotationObj = getEntityById(scopeEntities, notationObject.getId());
                if (existingNotationObj != null) {
                    // Found in scope: Use that one
                    notationObject = existingNotationObj;
                    //System.out.println("Found " + cno.toString() + " in scope.");
                } 
                else {
                    // Notation object not found in the scope
                    
                    // Check globals
                    existingNotationObj = getEntityById(globalEntities, notationObject.getId());
                    if (existingNotationObj != null) {
                        // Found in globals: Use that one
                        notationObject = existingNotationObj;
                    }
                    
                    
                    // If this is a reference, and no existing reference has been found,
                    // check the publication API. The notation ID could be an ID in the pub. "database" http://api.npolar.no/publication/?q=
                    if (notationObject instanceof ReferenceNotation && existingNotationObj == null) {
                        try {
                            if (isUUID(notationObject.getId())) {
                                String referenceString = new PublicationService(new Locale("en")).getPublication(notationObject.getId()).toString();
                                notationObject.setText(referenceString);
                            }
                        } catch (Exception e) {
                            
                        }
                    }
                    
                    // The notation object is now either completely new, or 
                    // fetched from the globals list. Either way, it must have 
                    // its counter updated and be added to the scope list.
                    
                    // Increment counter
                    counter.incrementCountForType(notationObject);
                    
                    // Update "number" if this is a reference notation:
                    if (notationObject instanceof ReferenceNotation) {
                        ((ReferenceNotation)notationObject).setNumber(counter.getCountForType(notationObject));
                    } 
                    
                    // Place the notation object in the scope list
                    scopeEntities.add(notationObject);
                }
            }
                
            else {
                // Non-reoccurring notation (index notation): Update counter, (set the ID), then add directly to scope list
                counter.incrementCountForType(notationObject);
                if (notationObject instanceof IndexNotation) {
                    notationObject.setId(notationObject.getDefaultIdPrefix() + counter.getCountForType(notationObject));
                }
                scopeEntities.add(notationObject);
            }
                
            // Store replacement mapping: "Any text like THAT (match) should be modified using THIS notation object"
            replacementMap.put(notationString, notationObject);
        }
        // Finished search

        // Apply modifications
        s = applyMods(s);

        return s;
    }
    
    /**
     * Resolves content notation attributes in the given string and sets the 
     * appropriate values on the given content notation object.
     * @param obj The content notation object to (possibly) modify.
     * @param text The text to resolve content notation attributes for.
     * @return The (possibly modified) content notation object.
     */
    private I_ContentNotationObject resolveAttributes(I_ContentNotationObject obj, String text) {
        Pattern p = Pattern.compile(ContentNotation.REGEX_PATTERN_ATTRIBS);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String innerMatch = text.substring(m.start(), m.end());
            String[] innerMatchParts = innerMatch.split("=\\{");

            if (innerMatchParts.length != 2) {
                throw new NullPointerException("Syntax error on notation attribute. Expected 'key={value}', but found '" + innerMatch + "'");
            }
            String attribName = innerMatchParts[0];
            String attribVal = innerMatchParts[1].substring(0, innerMatchParts[1].length() - 1);

            // Is the "id" attribute set?
            if (attribName.equals("id")) { obj.setId(attribVal); }
            // Is the "text" attribute set?
            else if (attribName.equals("text")) { obj.setText(attribVal); }
        }
        return obj;
    }
    
    /**
     * Modifies the given string by replacing segments according to the current replacement map.</p>
     * <p>If the current replacement map is empty, or contains no match in the given string, no modifications will occur.
     * @param source The string to modify.
     * @return The (potentially) modified string.
     */
    private synchronized String applyMods(String source) {
        String s = source;
        if (replacementMap != null && !replacementMap.isEmpty()) {
            Iterator<String> i = replacementMap.keySet().iterator();
            while (i.hasNext()) {
                // Get the text to replace
                String textToReplace = i.next(); 
                // Get the corresponding notation object
                I_ContentNotationObject replacement = (I_ContentNotationObject)replacementMap.get(textToReplace);
                int breaker = 0;
                while (s.contains(textToReplace) && breaker++ < 100) {
                    s = s.replace(textToReplace, replacement.getReplacement(textToReplace));
                }
            }

        }
        return s;
    }
    
    /**
     * Loads global notations, resolved from the file defined by the given path.
     * @param cms An initialized CmsAgent.
     * @param globalFilePath The path to the file containing global notation definitions.
     */
    public void loadGlobals(CmsAgent cms, String globalFilePath) {
        if (!globalFilePaths.contains(globalFilePath)) {
            if (globalFilePath != null && !globalFilePath.isEmpty()) {
                try {
                    globalEntities.addAll(resolveGlobals(cms.getContent(globalFilePath, "body", cms.getRequestContext().getLocale())));
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException("An error occurred while attempting to resolve global notations from file '" + globalFilePath + "': " + iae.getMessage());
                } catch (Exception e) {
                    // Ignore (assume this means the global file does not exist)
                }
            }
            globalFilePaths.add(globalFilePath);
        }
    }
    
    /**
     * Loads global notations, resolved from the RFS file defined by the given path.
     * @param globalFilePath The path to the RFS file containing global notation definitions.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void loadGlobals(String globalFilePath) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(globalFilePath));
        
        String line = null;
        String text = "";
        while ((line = reader.readLine()) != null) {
            text += line;
        }
        
        if (!globalFilePaths.contains(globalFilePath)) {
            if (globalFilePath != null && !globalFilePath.isEmpty()) {
                try {
                    globalEntities.addAll(resolveGlobals(text));
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException("An error occurred while attempting to resolve global notations from file '" + globalFilePath + "': " + iae.getMessage());
                } catch (Exception e) {
                    // Ignore (assume this means the global file does not exist)
                }
            }
            globalFilePaths.add(globalFilePath);
        }
    }
    
    /**
     * Resolves global content notations by resolving the given source string.
     * @param source A string containing content notations.
     */
    private List<I_ContentNotationObject> resolveGlobals(String source) {
        List<I_ContentNotationObject> list = new ArrayList<I_ContentNotationObject>();
        String regex = ReferenceNotation.REGEX_PATTERN + "|" + TooltipNotation.REGEX_PATTERN_SELFCLOSING;
        Matcher m = Pattern.compile(regex).matcher(source);

        // Start search ...
        while (m.find()) {
            String match = source.substring(m.start(), m.end());
            
            I_ContentNotationObject cno = ContentNotationObject.create(match);
            resolveAttributes(cno, match);
            if (cno.getId() == null)
                throw new NullPointerException("ID is required for global notations, but was missing here: '" + match + "'. Please correct missing ID and try again.");
            
            //I_ContentNotationObject existing = getEntityById(list, cno.getId()); // Will return null if no object with that ID exists in the list
            if (getEntityById(list, cno.getId()) == null) {
                list.add(cno);
            } else {
                throw new IllegalArgumentException("ID '" + cno.getId() + "' is already used. Fix the duplicate notation ID and try again.");
            }
        }
        return list;
    }
    
    /**
     * Get the notation object identified by the given ID from the given list. If no such notation object exists, null is returned.
     * @param list The list to search.
     * @param id The ID defining the notation object to look for.
     * @return The notation object contained in the given list and identified by the given ID, or null.
     */
    public I_ContentNotationObject getEntityById(List<I_ContentNotationObject> list, String id) {
        if (id == null)
            return null;
        
        Iterator<I_ContentNotationObject> i = list.iterator();
        while (i.hasNext()) {
            I_ContentNotationObject o = i.next();
            if (o.getId().equals(id))
                return o;
        }
        return null;
    }
    
    /**
     * Gets all ReferenceNotation instances currently present in this resolver's scope list.
     * Typically used when constructing a list of references to use on the page.
     * @return All ReferenceNotation instances currently present in this resolver's scope list.
     */
    public List<ReferenceNotation> getReferenceList() {
        List<ReferenceNotation> list = new ArrayList<ReferenceNotation>();
        Iterator<I_ContentNotationObject> itr = scopeEntities.iterator();
        while (itr.hasNext()) {
            I_ContentNotationObject hbo = itr.next();
            if (hbo instanceof ReferenceNotation)
                list.add((ReferenceNotation)hbo);
        }
        return list;
    }
    
    /**
     * Gets all IndexNotation instances currently present in this resolver's scope list.
     * Typically used when constructing a page index.
     * @return All IndexNotation instances currently present in this resolver's scope list.
     */
    public List<IndexNotation> getIndexList() {
        List<IndexNotation> list = new ArrayList<IndexNotation>();
        Iterator<I_ContentNotationObject> itr = scopeEntities.iterator();
        while (itr.hasNext()) {
            I_ContentNotationObject cno = itr.next();
            if (cno instanceof IndexNotation)
                list.add((IndexNotation)cno);
        }
        return list;
    }
    
    /**
     * Gets all TooltipNotation instances currently present in this resolver's scope list.
     * @return All TooltipNotation instances currently present in this resolver's scope list.
     */
    public List<TooltipNotation> getTooltipList() {
        List<TooltipNotation> list = new ArrayList<TooltipNotation>();
        Iterator<I_ContentNotationObject> itr = scopeEntities.iterator();
        while (itr.hasNext()) {
            I_ContentNotationObject cno = itr.next();
            if (cno instanceof TooltipNotation)
                list.add((TooltipNotation)cno);
        }
        return list;
    }
    
    /**
     * Gets a list containing paths to this resolver's global definition files.
     * @return A list containing paths to this resolver's global definition files.
     */
    public List<String> getGlobalFilePaths() { return this.globalFilePaths; }
    
    /**
     * Clears all this resolver's lists and counters.
     */
    public void clear() {
        globalEntities.clear();
        scopeEntities.clear();
        replacementMap.clear();
        counter.clear();
    }
    
    /**
     * Attempts to getCountForType a content notation resolver from the given session.
     * @param session The session object to examine.
     * @return The content notation resolver found in the given session.
     */
    public static ContentNotationResolver getFromSession(HttpSession session) {
        return (ContentNotationResolver)session.getAttribute(SESS_ATTR_NAME);
    }
}