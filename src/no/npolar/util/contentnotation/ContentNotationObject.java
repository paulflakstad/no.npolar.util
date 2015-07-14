package no.npolar.util.contentnotation;

/**
 * Class representation of content notations.</p>
 * <p>Notations and syntax:</p>
 * <ul>
 * <li>Reference: [ref id={an-id} text={My text} /]</li>
 * <li>Tooltip: [tooltip id={an-id} text={My text.}]My phrase[/tooltip]</li>
 * <li>Index: [index]This will be part of the page index[/index]</li>
 * </ul>
 * <p><i>Note: In global files, tooltips must use the same syntax as references.</i>
 * @author flakstad
 */
public abstract class ContentNotationObject implements I_ContentNotationObject {
    /** The ID. */
    protected String id = null;
    /** The text. */
    protected String text = null;
    
    /**
     * Gets the text.
     * 
     * @return The text.
     */
    public String getText() { return text; }
    /**
     * Gets the ID.
     * 
     * @return The ID.
     */
    public String getId() { return id; }
    /**
     * Sets the ID
     * @param id The ID.
     */
    public void setId(String id) { this.id = id; }
    /**
     * Sets the text.
     * 
     * @param text The text.
     */
    public void setText(String text) { this.text = text; }
    
    /**
     * Gets a string representation of this content notation object.
     * 
     * @return A string representation of this content notation object.
     */
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() 
                + (this instanceof ReferenceNotation ? " number={" + ((ReferenceNotation)this).getNumber() + "}" : "")
                + " id={" + this.getId() + "}"
                + " text={" + trim(this.getText()) + "}" 
                + " /]";
    }
    
    /**
     * Trims the given string to a specific maximum length.
     * <p>
     * If the given string's length is over 30, it is trimmed to 25 characters 
     * and appended "...". In all other cases, the given string is returned 
     * unmodified.
     * 
     * @param s The string to trim.
     * @return The trimmed string.
     */
    private String trim(String s) {
        if (s == null || s.length() <= 30) {
            return s;
        }
        else return s.substring(0, 25).concat("...");
    }
    
    /**
     * Creates a notation object, based on the given notation string.
     * 
     * @param notationString The notation string, either a tooltip, a reference or an index notation.
     * @return A new notation object.
     */
    public static I_ContentNotationObject create(String notationString) {
        if (notationString.startsWith("[" + TooltipNotation.NAME + " ")) {
            //id = TooltipNotation.ID_PREFIX + tooltipCount;
            return new TooltipNotation();
            //matchedTooltip = true;
            //if (comments) w.print("tooltip");
        } 
        else if (notationString.startsWith("[" + ReferenceNotation.NAME + " ")) {
            //id = ReferenceNotation.ID_PREFIX + refCount;
            return new ReferenceNotation();
            //matchedReference = true;
            //if (comments) w.print("reference");
        }
        else if (notationString.startsWith("[" + IndexNotation.NAME)) {
            //id = IndexNotation.ID_PREFIX + (++indexCount);
            return new IndexNotation();
            //matchedIndex = true;
            //if (comments) w.print("index");
        } else {
            throw new NullPointerException("Unexpected match: " + notationString);
        }
    }
}
