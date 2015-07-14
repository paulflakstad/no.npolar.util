package no.npolar.util.contentnotation;

import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author flakstad
 */
public class IndexNotation extends ContentNotationObject {
    /** The default page index entry name, used as prefix in default IDs. */
    public static final String ID_PREFIX = "pageindex";
    /** The index notation identifier. */
    public static final String NAME = "index";
    /** The number associated with this index. */
    private int number = 0;
    /**
     * Regex pattern for index notation:
     * Match: The exact string "[index", maybe followed by at least one space and [attribute notation followed by at least one space] one or more times, followed by "]", followed by anything not "[", followed by "[/index]"
     */
    protected static final String REGEX_PATTERN = "\\[" + NAME + "\\s*(" + ContentNotation.REGEX_PATTERN_ATTRIBS + "\\s*)*\\][^\\[]+\\[/" + NAME + "\\]";
    
    
    /**
     * Default constructor.
     */
    public IndexNotation() {}
    
    /**
     * Gets the replacement string, i.e. the string to insert in place of this notation.
     * @param toReplace The notation to replace.
     * @return The string to insert in place of this notation.
     */
    public String getReplacement(String toReplace) {
        if (this.id == null || this.id.isEmpty())
            return toReplace;
        
        String wrapped = toReplace.substring(toReplace.indexOf("]") + 1);
        wrapped = wrapped.substring(0, wrapped.indexOf("[/"));
        
        if (this.text == null || this.text.isEmpty())
            this.setText(wrapped);

        return "<a id=\"" + StringEscapeUtils.escapeHtml(this.id) + "\"></a>" + wrapped;
    }
    
    public String getDefaultIdPrefix() { return ID_PREFIX; }
    
    public boolean isAllowedReoccur() { return false; };
    public boolean isHoverBoxNotation() { return false; };
    
}
