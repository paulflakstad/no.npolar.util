package no.npolar.util.contentnotation;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Tooltip class representation. Tooltips notations in the text must look like 
 * this (on-page / global):</p>
 * <ul>
 * <li>[tooltip id={an-id} text={My text.}]My phrase[/tooltip]</li>
 * <li>[tooltip id={an-id} text={My text.} /]</li>
 * </ul>
 * @author flakstad
 */
public class TooltipNotation extends ContentNotationObject {
    /** The default tooltip name, used as prefix in default IDs. */
    public static final String ID_PREFIX = "tooltip";
    /** The tooltip notation identifier. */
    public static final String NAME = "tooltip";
    /**
     * Regex pattern for tooltip notation:
     * Match: The exact string "[tooltip", followed by at least one space, followed by [attribute notation followed by at least one space] one or more times, followed by "]", followed by anything not "[", followed by "[/tooltip]"
     */
    protected static final String REGEX_PATTERN = "\\[" + NAME + "\\s+(" + ContentNotation.REGEX_PATTERN_ATTRIBS + "\\s*)+\\][^\\[]+\\[/" + NAME + "\\]";
    /**
     * Regex pattern for self-closing tooltip notation (used only in global definitions):
     * Match: The exact string "[tooltip", followed by at least one space, followed by [attribute notation followed by at least one space] one or more times, followed by "/]"
     */
    protected static final String REGEX_PATTERN_SELFCLOSING = "\\[" + NAME + "\\s+(" + ContentNotation.REGEX_PATTERN_ATTRIBS + "\\s+)+/\\]";
    
    /**
     * Default constructor.
     */
    public TooltipNotation() {}
    
    /**
     * Gets the replacement string, i.e. the string to insert in place of this notation.
     * @param toReplace The notation to replace.
     * @return The string to insert in place of this notation.
     */
    public String getReplacement(String toReplace) {
        if (this.text == null || this.text.isEmpty())
            return toReplace;

        toReplace = toReplace.substring(toReplace.indexOf("]") + 1);
        toReplace = toReplace.substring(0, toReplace.indexOf("[/"));

        return "<span class=\"explain-enabled\" data-hoverbox=\"" + StringEscapeUtils.escapeHtml(this.text) + "\">" + toReplace + "</span>";
    }
    
    public String getDefaultIdPrefix() { return ID_PREFIX; }
    
    public boolean isAllowedReoccur() { return true; };
    public boolean isHoverBoxNotation() { return true; };
}
