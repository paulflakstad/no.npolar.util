package no.npolar.util.contentnotation;

/**
 * Interface implemented by specific content notation classes.
 * @author flakstad
 */
public interface I_ContentNotationObject {
    public String getId();
    public String getText();
    public void setId(String id);
    public void setText(String text);
    public String getReplacement(String toReplace);
    public String getDefaultIdPrefix();
    public boolean isAllowedReoccur();
    public boolean isHoverBoxNotation();
    @Override
    public String toString();
}
