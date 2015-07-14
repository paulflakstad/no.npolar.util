package no.npolar.util.contentnotation;

/**
 *
 * @author flakstad
 */
public class ContentNotationCounter {
    /** The index counter. */
    private int indices = 0;
    /** The tooltip counter. */
    private int tooltips = 0;
    /** The reference counter. */
    private int references = 0;
    
    /**
     * Creates a new counter.
     */
    public ContentNotationCounter() {
        indices = 0;
        tooltips = 0;
        references = 0;
    }
    
    /**
     * Increments the count for objects of the given type by one.
     * @param typeExample An instance of the type of object to increment the count for.
     * @return The (updated) count for objects of the given type.
     */
    public int incrementCountForType(I_ContentNotationObject typeExample) {
        if (typeExample instanceof IndexNotation)
            return ++indices;
        if (typeExample instanceof TooltipNotation)
            return ++tooltips;
        if (typeExample instanceof ReferenceNotation)
            return ++references;
        
        return -1;
    }
    
    /**
     * Gets the count for objects of the given type.
     * @param typeExample An instance of the type of object to fetch the count for.
     * @return The count for objects of the given type.
     */
    public int getCountForType(I_ContentNotationObject typeExample) {
        if (typeExample instanceof IndexNotation)
            return indices;
        if (typeExample instanceof TooltipNotation)
            return tooltips;
        if (typeExample instanceof ReferenceNotation)
            return references;
        
        return -1;
    }
    
    /**
     * Resets this counter by setting all numbers to zero.
     */
    public void clear() {
        indices = 0;
        tooltips = 0;
        references = 0;
    }
}
