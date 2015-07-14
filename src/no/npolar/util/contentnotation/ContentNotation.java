package no.npolar.util.contentnotation;

/**
 *
 * @author flakstad
 */
public class ContentNotation {
    /** 
     * Regex pattern for notation attributes (e.g. "id={some-identifier}"): 
     * Match: Any word character (a-z, A-X, _, 0-9) or hyphen (-), one or more times, followed by "={", followed by anything not "}" zero or more times, followed by "}"
     */
    public static final String REGEX_PATTERN_ATTRIBS = "[\\w\\-]+=\\{[^}]*\\}";
}
