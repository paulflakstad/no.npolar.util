package no.npolar.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author flakstad
 */
public class ResourceList {
    private List<ResourceListItem> items    = null;
    
    private boolean displayTeaser           = false;
    private boolean mergeTeaser             = false;
    private String dateProperty             = null;
    private String bylineProperty           = null;
    private SimpleDateFormat dateFormat     = null;
    private int imageWidth                  = -1;
    private String title                    = null;
    private String text                     = null;
    
    
    /**
     * Creates a new resource list with no teaser, byline, timestamp or image.
     * @see ResourceList(boolean, boolean, int, java.lang.String, java.lang.String, java.lang.String) 
     */
    public ResourceList() {
        this(false, false, -1, null, null, null);
    }    
    
    /**
     * Creates a new resource list with no byline, timestamp or image.
     * @see ResourceList(boolean, boolean, int, java.lang.String, java.lang.String, java.lang.String) 
     */
    public ResourceList(boolean displayTeaser, boolean mergeTeaser) {
        this(displayTeaser, mergeTeaser, -1, null, null, null);
    }    
    
    /**
     * Creates a new resource list with no byline or timestamp.
     * @see ResourceList(boolean, boolean, int, java.lang.String, java.lang.String, java.lang.String) 
     */
    public ResourceList(boolean displayTeaser, boolean mergeTeaser, int imageWidth) {
        this(displayTeaser, mergeTeaser, imageWidth, null, null, null);
    }
    
    /**
     * Creates a new resource list with no byline.
     * @see ResourceList(boolean, boolean, int, java.lang.String, java.lang.String, java.lang.String) 
     */
    public ResourceList(boolean displayTeaser, boolean mergeTeaser, int imageWidth, String dateFormat, String dateProperty) {
        this(displayTeaser, mergeTeaser, imageWidth, dateFormat, dateProperty, null);
    }
    
    /**
     * Creates a new resource list.
     * @param displayTeaser If true, a teaser should be displayed for each list item.
     * @param mergeTeaser If true, a teaser should be part of each list item, as the link title.
     * @param imageWidth If negative, the list should not contain images. If 0, images should not be scaled. If any positive value, images should be scaled to that width.
     * @param dateFormat The date format to format the timestamp by. Set to null to indicate no timestamp.
     * @param dateProperty The name of the property to use when evaluating timestamps.
     * @param bylineProperty The name of the property to fetch byline details from.
     */
    public ResourceList(boolean displayTeaser, boolean mergeTeaser, int imageWidth, String dateFormat, String dateProperty, String bylineProperty) {
        items = new ArrayList<ResourceListItem>();
        this.imageWidth = imageWidth;
        this.dateProperty = dateProperty;
        this.displayTeaser = displayTeaser;
        this.mergeTeaser = mergeTeaser;
        if (dateFormat != null) {
            try {
                this.dateFormat = new SimpleDateFormat(dateFormat);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to create a date format with pattern '" + dateFormat + "': " + e.getMessage());
            }
        } else {

        }
        this.bylineProperty = bylineProperty;
    }
    
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setText(String text) {
        this.text = text;
    }
    public void addItem(ResourceListItem item) {
        items.add(item);
    }
    public List<ResourceListItem> getItems() {
        return items;
    }
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }
    public String getDateProperty() {
        return dateProperty;
    }
    public String getBylineProperty() {
        return bylineProperty;
    }
    public int getImageWidth() {
        return imageWidth;
    }
    public boolean isDisplayTeaser() {
        return displayTeaser;
    }
    public boolean isDisplayTimestamp() {
        return dateFormat != null;
    }
    public boolean isDisplayByline() {
        return bylineProperty != null;
    }
    public boolean isDisplayImages() {
        return imageWidth > -1;
    }
    public boolean isMergeTeaser() {
        return mergeTeaser;
    }
}