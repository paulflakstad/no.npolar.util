package no.npolar.util.contentnotation;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * @author flakstad
 */
public class ReferenceNotation extends ContentNotationObject {
    /** The default reference name, used as prefix in default IDs. */
    public static final String ID_PREFIX = "reference";
    /** The reference notation identifier. */
    public static final String NAME = "ref";
    /** The number associated with this reference. */
    private int number = 0;
    /**
     * Regex pattern for reference notation:
     * Match: The exact string "[ref", followed by at least one space, followed by [attribute notation followed by at least one space] one or more times, followed by "/]"
     */
    public static final String REGEX_PATTERN = "\\[" + NAME + "\\s+(" + ContentNotation.REGEX_PATTERN_ATTRIBS + "\\s+)+/\\]";
    
    /**
     * Default constructor.
     */
    public ReferenceNotation() {}
    
    /**
     * Sets the number associated with this reference.
     * 
     * @param number The number associated with this reference.
     * @return This reference instance.
     */
    public ReferenceNotation setNumber(int number) {
        this.number = number;
        return this;
    }
    
    /**
     * Gets the number associated with this reference.
     * 
     * @return the number associated with this reference.
     */
    public int getNumber() { return this.number; }
    
    /**
     * Gets the default ID prefix.
     * 
     * @return The default ID prefix.
     * @see #ID_PREFIX
     */
    public String getDefaultIdPrefix() { return ID_PREFIX; }
    
    /**
     * Gets the replacement string, i.e. the string to insert in place of this notation.
     * 
     * @param toReplace The notation to replace.
     * @return The string to insert in place of this notation.
     */
    public String getReplacement(String toReplace) {
        if (this.text == null || this.text.isEmpty()) 
            return toReplace;
        
        return "<sup>"
                    + "<a"
                    + " href=\"#" + this.getId() + "\""
                    + " data-hoverbox=\"" + StringEscapeUtils.escapeHtml(this.getText()) + "\""
                    + " class=\"reflink\""
                    + ">"
                        //+ "[" + (refs.indexOf(this)+1) + "]"
                        + "[" + this.getNumber() + "]"
                    + "</a>"
                + "</sup>";
    }
    
    /**
     * Flag indicating if reoccurrence is allowed.
     * 
     * @return Always returns true.
     */
    public boolean isAllowedReoccur() { return true; };
    
    /**
     * Flag indicating if this is a hover box notation.
     * 
     * @return Always returns true.
     */
    public boolean isHoverBoxNotation() { return true; };
}
