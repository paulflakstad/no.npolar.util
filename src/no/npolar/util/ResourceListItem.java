package no.npolar.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.opencms.jsp.CmsJspActionElement;

/**
 *
 * @author flakstad
 */
public class ResourceListItem {
    private String title    = null;
    private String teaser   = null;
    private String uri      = null;
    private String imageUri = null;
    private String imageAlt = null;
    private Date timestamp  = null;
    
    public ResourceListItem(String title, String uri) {
        this.title = title;
        this.uri = uri;
    }
    
    public void setTeaser(String teaser) {
        this.teaser = teaser;
    }
    public void setImage(String imageUri, String imageAlt) {
        this.imageUri = imageUri;
        this.imageAlt = imageAlt;
    }
    public void setTimestamp(long timestamp) {
        try {
            this.timestamp = new Date(timestamp);
        } catch (Exception e) {
            throw new NumberFormatException("Unable to create Date object from the long value '" + String.valueOf(timestamp) + "': " + e.getMessage());
        }
    }
    
    public String getTimestamp(SimpleDateFormat sdf) {
        return sdf.format(this.timestamp);
    }
    
    public String getImageUri() {
        return this.imageUri;
    }
    
    public String getImgTag(int width, CmsJspActionElement cms, String cssClass) {
        String imageTag = "<img src=\"";
        CmsImageProcessor imgPro = null;
        if (width > 0) {
            if (this.imageUri.startsWith("/")) { // Local image, do a downscale
                imgPro = new CmsImageProcessor();
                imgPro.setType(3);
                imgPro.setQuality(100);
                imgPro.setWidth(width);
                imageTag += CmsAgent.getTagAttributesAsMap(cms.img(this.imageUri, imgPro.getReScaler(imgPro), null, false)).get("src");
            }
            else { // Not local image, 
                imageTag += this.imageUri + "\" width=\"" + width;
            }
        } 
        else {
            imageTag += this.imageUri;
        }
        return imageTag.concat("\" alt=\"" + this.imageAlt + "\"" + (cssClass != null ? " class=\"".concat(cssClass).concat("\"") : "") + " />");
    }
    
    public String getImgTag(CmsJspActionElement cms) {
        return getImgTag(0, cms, null);
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public String getTeaser() {
        return this.teaser;
    }
    
    public String getUri() {
        return this.uri;
    }
}
