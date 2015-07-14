package no.npolar.util;
import java.util.HashMap;

/**
 * This is an override of the HashMap class, with the difference being that 
 * while HashMap operates with the general Object type, this class assumes that 
 * both keys and values are Strings. When parsing XML, String representations of 
 * any type are typical. By replacing the regular HashMap with this one, tiresome
 * typecasting can be avoided.
 * 
 * @author Paul-Inge Flakstad
 */
public class StringMap extends HashMap {
    public StringMap() {
        super();
    }
    
    public StringMap(StringMap otherMap) {
        super(otherMap);
    }
    
    public StringMap(int initialCapacity) {
        super(initialCapacity);
    }
    
    public StringMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }
    
    /**
     * Returns the value to which the specified key is mapped, or null if this 
     * map contains no mapping for the key. 
     * @param key  The key
     * @return  The value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public String getString(String key) {
        return (String)(super.get((Object)key));
    }
    
    /**
     * Associates the specified value with the specified key in this map. If the 
     * map previously contained a mapping for the key, the old value is replaced.
     * @param key  The key with which the specified value is to be associated
     * @param value  The value to be associated with the specified key
     */
    public Object put(Object key, Object value) {
        Object o = super.get(key);
        super.put((String)key, (String)value);
        return o;
    }
}
