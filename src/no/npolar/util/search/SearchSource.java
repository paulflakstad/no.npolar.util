package no.npolar.util.search;

/**
 * Represents a searchable source, e.g. "Personalhåndboka" or "Isblink".
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class SearchSource implements Comparable {
    /** The name of the source. */
    private String name = null;
    /** The number of hits in this source. */
    private int numHits = -1;
    
    /**
     * Creates a new source with the given name, and with zero hits.
     * 
     * @param name The source name.
     */
    public SearchSource(String name) {
        this.name = name;
        numHits = 0;
    }
    
    /**
     * Gets the name of the source.
     * 
     * @return The source name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Increments the number of hits for this source by 1.
     * 
     * @return The updated number of hits for this source.
     */
    public int addOneHit() {
        return ++numHits;
    }
    
    /**
     * Gets the number of hits for this source.
     * 
     * @return The number of hits for this source.
     */
    public int getNumHits() {
        return numHits;
    }
    
    /**
     * Compares this source's name with the name of the given source.
     * 
     * @param that The source to compare with (must be another instance of this class).
     * @return The result of the name comparison.
     */
    public int compareTo(Object that) {
        if (that instanceof SearchSource) {
            return this.name.compareTo(((SearchSource)that).name);
        }
        throw new IllegalArgumentException("Only other SearchSource instances can be compared to this SearchSource.");
    }
    
    /**
     * Checks if this source's name is equal to the name of the given source.
     * 
     * @param that The source to compare with (should be another instance of this class).
     * @return <code>true</code> if the given source's name is equal to the name of this source, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object that) {
        if (that == null || !(that instanceof SearchSource))
            return false;
        return this.name.equals(((SearchSource)that).name);
    }
    
    /**
     * Gets the hash code for this source.
     * <p>
     * The hash code is made up of the hash code of the name, plus the number of
     * hits.
     * 
     * @return The hash code for this source.
     */
    @Override
    public int hashCode() {
        return name.hashCode() + numHits;
    }
}