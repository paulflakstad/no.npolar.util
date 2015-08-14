package no.npolar.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import no.npolar.util.exception.ImageAccessException;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.CmsJspXmlContentBean;
import org.opencms.jsp.I_CmsXmlContentContainer;
import org.opencms.loader.CmsImageScaler;
import org.opencms.main.CmsException;
import org.opencms.xml.I_CmsXmlDocument;

/**
 * @see http://http://responsiveimages.org/
 * @see http://www.smashingmagazine.com/2014/05/14/responsive-images-done-right-guide-picture-srcset/
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class ImageUtil {
    /** Size used for images which appear small on large screens, occupying up to 33.3% of the viewport width. */
    public static final int SIZE_S = 1;
    
    /** Size used for images which appear medium on large screens, occupying up to 50% of the viewport width. */
    public static final int SIZE_M = 2;
    
    /** Size used for images which appear large/full-width on large screens, occupying up to 100% of the viewport width. */
    public static final int SIZE_L = 3;
    
    /** Sizes list, used to translate a string value (e.g. "L") into an integer corresponding to one of the SIZE_X constants of this class. */
    protected static final List SIZES = Arrays.asList(new String[] { "UNDEFINED", "S", "M", "L" });
    
    /** The default image size. When no size is given, we must assume "large/full-width" (up to 100% of the viewport width). */
    public static final int DEFAULT_SIZE = SIZE_L;
    
    /** The default maximum pixel width of the image. If the original image is wider, a down-scaled (this width). */
    public static final int DEFAULT_MAX_WIDTH = 1200;
    
    /** The default maximum image width, relative to the viewport (in percent). */
    public static final int DEFAULT_MAX_VP_WIDTH = 100;
    
    /** The default minimum image width, relative to the viewport (in percent). */
    public static final int DEFAULT_MIN_VP_WIDTH = 50;
    
    /** The default breakpoint. Typically, 50em is roughly 800px. */
    public static final String DEFAULT_BREAKPOINT = "50em";
    
    /** The default quality to use when creating the various image versions. */
    public static final int DEFAULT_QUALITY = 90;
    
    /** Parameter name for fingerprint parameters. */
    public static final String PARAM_NAME_FINGERPRINT = "fingerprint";
    
    /** Crop ratio 1:1 (an even square). */
    public static final String CROP_RATIO_1_1 = "1:1";
    /** Crop ratio 4:3 (typical photo and "old TV" format). */
    public static final String CROP_RATIO_4_3 = "4:3";
    /** Crop ratio 16:9 ("widescreen" format). */
    public static final String CROP_RATIO_16_9 = "16:9";
    /** "Crop ratio" no cropping. */
    public static final String CROP_RATIO_NO_CROP = null;
    
    public static final int SCALE_TYPE_CROP = 2;
    public static final int SCALE_TYPE_NOCROP = 4;
    
    /** The name of the element that holds the image URI, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_URI = "URI";
    /** The name of the element that holds the image alt-text, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_TITLE = "Title";    
    /** The name of the element that holds the image caption, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_TEXT = "Text";    
    /** The name of the element that holds the image source, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_SOURCE = "Source";    
    /** The name of the element that holds the image type, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_TYPE = "ImageType";    
    /** The name of the element that holds the image size, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_SIZE = "Size";    
    /** The name of the element that holds the image float setting, as defined in /system/modules/no.npolar.common.pageelements/schemas/image.xsd */
    public static final String OCMS_EL_NAME_IMAGE_FLOAT = "Float";
    
    private CmsJspActionElement cms = null;
    private CmsObject cmso = null;
    private String imagePath = null;
    private String imageAlt = null;
    private String imageCaption = null;
    private String imageSource = null;
    private String imageType = null;
    private int imageSize = SIZE_L;
    private String imageFloat = null;
    private CmsImageScaler imageHandle = null;
    private I_CmsXmlContentContainer imageContainer = null;
    
    
    public ImageUtil() {
        
    }
    
    public ImageUtil(CmsJspActionElement cms, String imagePath) throws CmsException {
        this.cms = cms;
        this.cmso = cms.getCmsObject();
        this.imagePath = imagePath;
        this.imageHandle = new CmsImageScaler(cmso, cmso.readResource(imagePath));
    }
    
    /**
     * Convenience constructor specially adapted to the "standard" image element, 
     * which is part of the no.npolar.common.pageelements module.
     * 
     * @param cms
     * @param imageContainer
     * @see /system/modules/no.npolar.common.pageelements/schemas/image.xsd
     * @throws CmsException 
     */
    public ImageUtil(CmsJspXmlContentBean cms, I_CmsXmlContentContainer imageContainer) throws CmsException {
        this.imageContainer = imageContainer;
        this.cms = cms;
        this.cmso = cms.getCmsObject();
        this.imagePath = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_URI);
        this.imageAlt = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_TITLE);
        this.imageCaption = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_TEXT);
        this.imageSource = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_SOURCE);
        this.imageSize = SIZES.indexOf(cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_TEXT));
        this.imageType = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_TYPE);
        this.imageFloat = cms.contentshow(imageContainer, OCMS_EL_NAME_IMAGE_FLOAT);
        this.imageHandle = new CmsImageScaler(cmso, cmso.readResource(imagePath));
        
        if (!CmsAgent.elementExists(imagePath)) { imagePath = "NO_IMAGE_SELECTED"; }
        if (!CmsAgent.elementExists(imageAlt)) { imageAlt = ""; }
        if (!CmsAgent.elementExists(imageCaption)) { imageCaption = ""; }
        if (!CmsAgent.elementExists(imageSource)) { imageSource = ""; }
        if (!CmsAgent.elementExists(imageType)) { imageType = ""; }
        if (!CmsAgent.elementExists(imageFloat)) { imageFloat = ""; }
    }
    
    /**
     * Shorthand method: Quality = 100%, maxAbsoluteWidth = 100%, and 
     * maxViewportWidth = 100%.
     * <p>
     * Optional class and crop ratio.
     * 
     * @param figureClass Class name to use, can be null.
     * @param cropRatio The crop ratio. Must be given as "[width]:[height]", i.e.: "4:3", or a null value (indicating "don't crop"). Some typical ratios are offered by the CROP_RATIO_X static members of this class, for example {@link ImageUtil#CROP_RATIO_1_1}.
     * @return
     * @throws ImageAccessException 
     */
    public synchronized String getImage(String figureClass, String cropRatio) throws ImageAccessException {
        int maxAbsWidth = 1200;
        if (this.imageSize == SIZE_M)
            maxAbsWidth = 600;
        if (this.imageSize == SIZE_S)
            maxAbsWidth = 400;
        
        return getImage(figureClass, cropRatio, 100, maxAbsWidth, DEFAULT_MAX_VP_WIDTH, "800px");
    }
    
    
    
    /**
     * Shorthand method: No class, no cropping, 100% quality.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @return A ready-to-use figure element, complete with img (with srcset, sizes, src and alt attributes) and figcaption children.
     * @throws ImageAccessException 
     */
    public synchronized String getImage (int maxAbsoluteWidth
                                        , int maxViewportRelativeWidth
                                        , String linearBreakpoint) throws ImageAccessException {
        return this.getImage(null, null, 100, maxAbsoluteWidth, maxViewportRelativeWidth, linearBreakpoint);
    }
    
    /**
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @return A ready-to-use figure element, complete with img (with srcset, sizes, src and alt attributes) and figcaption children.
     * @throws ImageAccessException
     */
    public synchronized String getImage (String figureClass
                                        , String cropRatio
                                        , int quality
                                        , int maxAbsoluteWidth
                                        , int maxViewportRelativeWidth
                                        , String linearBreakpoint) throws ImageAccessException {
        
        this.cms = new CmsAgent(cms.getJspContext(), cms.getRequest(), cms.getResponse());
        
        String s = "<figure" + (figureClass != null && !figureClass.isEmpty() ? " class=\""+figureClass+"\"" : "") + ">";
        // Call the static function, passing arguments from this instance
        s += ImageUtil.getImage(this.cms
                                , this.imagePath
                                , this.imageAlt
                                , cropRatio
                                , maxAbsoluteWidth
                                , maxViewportRelativeWidth
                                , this.imageSize
                                , quality
                                , linearBreakpoint
                                );
        if (!this.imageCaption.isEmpty() || !this.imageSource.isEmpty()) {
            s += "<figcaption>";
            if (!this.imageCaption.isEmpty()) {
                s += this.imageCaption;
            }
            if (!this.imageSource.isEmpty()) {
                s += "<span class=\"credit\">";
                        try {
                            s += ((CmsAgent)cms).labelUnicode("label.pageelements." + this.imageType.toLowerCase());
                        } catch (Exception e) {
                            s += cms.label("label.pageelements." + this.imageType.toLowerCase());
                        }
                s += ": " + this.imageSource + "</span>";
            }
            s += "</figcaption>";
        }
        s += "</figure>";
        return s;
    }
    /**
     * Gets the image URI, with a width constraint, using the max width defined 
     * in {@link ImageUtil#DEFAULT_MAX_WIDTH}.
     * 
     * @see ImageUtil#getWidthContrainedUri(org.opencms.jsp.CmsJspActionElement, java.lang.String, int) 
     * @throws ImageAccessException
     * @throws CmsException 
     */
    public synchronized String getWidthConstrainedUri() throws ImageAccessException, CmsException {
        return this.getWidthConstrainedUri(DEFAULT_MAX_WIDTH);
    }
    /**
     * Gets the image URI, with a width constraint.
     * 
     * @see ImageUtil#getWidthContrainedUri(org.opencms.jsp.CmsJspActionElement, java.lang.String, int) 
     * @throws ImageAccessException
     * @throws CmsException 
     */
    public synchronized String getWidthConstrainedUri(int maxWidth) throws ImageAccessException, CmsException {
        return ImageUtil.getWidthConstrainedUri(cms, imagePath, maxWidth);
    }
    /**
     * Gets an image URI, with a width constraint, using the max width defined 
     * in {@link ImageUtil#DEFAULT_MAX_WIDTH}.
     * 
     * @see ImageUtil#getWidthContrainedUri(org.opencms.jsp.CmsJspActionElement, java.lang.String, int) 
     * @throws ImageAccessException
     * @throws CmsException 
     */
    public static synchronized String getWidthConstrainedUri(CmsJspActionElement cms, String imageUri) throws ImageAccessException, CmsException {
        return getWidthConstrainedUri(cms, imageUri, DEFAULT_MAX_WIDTH);
    }
    
    /**
     * Get an image URI, with a width constraint.
     * <p>
     * If the given image is wider than the given max width, a URI to a 
     * down-scaled version is returned. Otherwise, the given URI is returned.
     * 
     * @param cms An initialized action element.
     * @param imageUri The URI to the image.
     * @param maxWidth The max width.
     * @return The URI to the given image, possibly down-scaled to the given max width.
     * @throws ImageAccessException
     * @throws CmsException 
     */
    public static synchronized String getWidthConstrainedUri(CmsJspActionElement cms, String imageUri, int maxWidth) throws ImageAccessException, CmsException {
        CmsObject cmso = cms.getCmsObject();
        
        // Determine the width of the (original) image
        int imageWidth = getWidth(cmso, imageUri);
        
        // If the (original) image width exceeds the given max width, create a 
        // URI to a version scaled down to the given max width
        if (imageWidth > maxWidth) {
            CmsImageScaler scaler = new CmsImageScaler(cmso, cmso.readResource(imageUri));
            scaler.setHeight(getRescaledHeight(cmso, imageUri, maxWidth));
            scaler.setWidth(maxWidth);
            scaler.setQuality(100);
            scaler.setType(SCALE_TYPE_NOCROP);
            imageUri = (String)CmsAgent.getTagAttributesAsMap(cms.img(imageUri, scaler.getReScaler(scaler), null)).get("src");
        }
        
        return imageUri;
    }
    
    /**
     * Gets the width of the given image.
     * 
     * @param cmso An initialized CmsObject, needed to access the {@link CmsPropertyDefinition#PROPERTY_IMAGE_SIZE} property.
     * @param imageUri The image URI.
     * @return The width of the given image. A return value of 0 (zero) indicates an error.
     */
    public static int getWidth(CmsObject cmso, String imageUri) {
        int imageWidth = 0;
        try {
            CmsImageScaler imageHandle = new CmsImageScaler(cmso, cmso.readResource(imageUri));
            imageWidth = imageHandle.getWidth();
        } catch (Exception e) {
            // Log this?
        }
        return imageWidth;
    }
    
    /**
     * Gets a figure element, complete with caption, and ready for highslide 
     * enlargement.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @return A ready-to-use figure element, complete with img (with srcset, sizes, src and alt attributes) and figcaption children.
     * @throws ImageAccessException
     */
    /*public synchronized String getHighslideImage (String figureClass
                                        , String cropRatio
                                        , int quality
                                        , int maxAbsoluteWidth
                                        , int maxViewportRelativeWidth
                                        , String linearBreakpoint) throws ImageAccessException, CmsException {
        
        this.cms = new CmsAgent(cms.getJspContext(), cms.getRequest(), cms.getResponse());
        
        String s = "<figure class=\"media" + (figureClass != null && !figureClass.isEmpty() ? " "+figureClass : "") + "\">";
        s += "<a class=\"highslide\" href=\"" + cms.link(getWidthConstrainedUri(cms,imagePath)) + "\" onclick=\"return hs.expand(this);\">";
        
        // Call the static function, passing arguments from this instance
        s += ImageUtil.getImage(this.cms
                                , this.imagePath
                                , this.imageAlt
                                , cropRatio
                                , maxAbsoluteWidth
                                , maxViewportRelativeWidth
                                , this.imageSize
                                , quality
                                , linearBreakpoint
                                );
        s += "</a>";
        
        if (!this.imageCaption.isEmpty() || !this.imageSource.isEmpty()) {
            s += "<figcaption class=\"caption highslide-caption\">";
            if (!this.imageCaption.isEmpty()) {
                s += this.imageCaption;
            }
            if (!this.imageSource.isEmpty()) {
                s += "<span class=\"credit\">";
                        try {
                            s += ((CmsAgent)cms).labelUnicode("label.pageelements." + this.imageType.toLowerCase());
                        } catch (Exception e) {
                            s += cms.label("label.pageelements." + this.imageType.toLowerCase());
                        }
                s += ": " + this.imageSource + "</span>";
            }
            s += "</figcaption>";
        }
        s += "</figure>";
        
        return s;
    }
    //*/
    
    /**
     * Calculates the rescaled height of an image, based on the new width and 
     * assuming the aspect ratio should be kept intact.
     * 
     * @param cmso Initialized CMS object.
     * @param imagePath The path to the image.
     * @param rescaledWidth The new width.
     * @return The new height.
     */
    public static int getRescaledHeight(CmsObject cmso, String imagePath, int rescaledWidth) throws ImageAccessException {
        CmsImageScaler image = null;
        try {
            image = new CmsImageScaler(cmso, cmso.readResource(imagePath));
        } catch (Exception e) {
            throw new ImageAccessException("Error reading details from image '" + imagePath + "': " + e.getMessage());
        }
        double newHeight = 0.0;
        double ratio = 0.0;
        ratio = (double)image.getWidth() / rescaledWidth; // IMPORTANT! Cast one to double, or ratio will get an integer value..!!!
        newHeight = (double)image.getHeight() / ratio;
        return (int)newHeight;
    }
    
    /**
     * Produces a ready-to-use img element, complete with srcset, sizes, src and 
     * alt attributes, based on the given arguments.
     * <p>
     * A simple approach is taken: generate several versions of an image, 
     * covering a range of resolutions. Furthermore, if the image will never be 
     * full-width in viewports wider than the linear breakpoint, include info 
     * to indicate its maximum width (relative to the viewport). The latter 
     * is a hint to the browser to help it pick the appropriate version of the 
     * image. (For widths narrower than the linear breakpoint, we assume that 
     * <strong>any</strong> image may span full-width.)
     * <p>
     * <strong>Example:</strong><br />Assume 3 images are generated:
     * <ul><li>400px</li><li>800px</li><li>1200px</li></ul>
     * If the viewport width is 1080px, the browser could pick:
     * <ul>
     * <li>the 800px image, if the image will span max. 50%</li>
     * <li>the 1200px image, if the image will span full-width</li>
     * </ul>
     * If the viewport width is 720px, the browser could pick:
     * <ul>
     * <li>the 400px image, if the image will span max. 50%</li>
     * <li>the 800px image, if the image will span full-width</li>
     * </ul>
     * 
     * @param cms Needed to access the image and the VFS. Mandatory.
     * @param imageUri The path to the image in the VFS. Mandatory.
     * @param alt The alternative text. Provide a null value to use the "Description" property, or "none" / "-" to leave it empty.
     * @param cropRatio The crop ratio. Must be given as "[width]:[height]", i.e.: "4:3", or a null value (indicating "don't crop"). Some typical ratios are offered by the CROP_RATIO_X static members of this class, for example {@link ImageUtil#CROP_RATIO_1_1}.
     * @param maxAbsoluteWidth The maximum absolute image width (in pixels). If the given image is wider, a down-scaled (to this width) version will be generated and used as the largest image version. Provide -1 to use the default, {@link ImageUtil#DEFAULT_MAX_WIDTH}.
     * @param maxViewportRelativeWidth The maximum image width (in percent), relative to the viewport. Provide -1 to use the default, {@link ImageUtil#DEFAULT_MAX_VP_WIDTH}.
     * @param size The image size. Must be one of the SIZE_X static members of this class, for example {@link ImageUtil#SIZE_M}. Provide -1 to use the default, {@link ImageUtil#DEFAULT_SIZE}.
     * @param quality The rescale quality, an integer between 0 (worst) and 100 (best).
     * @param linearBreakpoint The linear/float breakpoint, i.e.: "50em", or "800px". Provide a null value to use the default, {@link ImageUtil#DEFAULT_BREAKPOINT}.
     * @return A ready-to-use img element, complete with srcset, sizes, src and alt attributes.
     * @throws ImageAccessException 
     */
    public static synchronized String getImage (
            CmsJspActionElement cms
            , String imageUri
            , String alt
            , String cropRatio
            , int maxAbsoluteWidth
            , int maxViewportRelativeWidth
            , int size
            , int quality
            , String linearBreakpoint
            )
             throws ImageAccessException
    {
        boolean isParameterizedImageUri = imageUri.indexOf("?") > 0;
        String imageResourcePath = isParameterizedImageUri ? imageUri.substring(0, imageUri.indexOf("?")) : imageUri;
        CmsObject cmso = cms.getCmsObject();
        // 1. Create a set of scaled sizes (namely S, M & L - L being the fallback)
        
        // For each version, construct its srcset entry by adding the URI
        // and the width, e.g.: "small.jpg 320w" (320 is the width of the image).
        List<String> srcset = new ArrayList<String>();
        // Generate the versions here ...
        int scaleType = cropRatio == null ? SCALE_TYPE_NOCROP : SCALE_TYPE_CROP;
        if (!cmso.existsResource(imageResourcePath , CmsResourceFilter.requireType(CmsResourceTypeImage.getStaticTypeId()))) {
            throw new ImageAccessException("Attempting to scale image '" + imageResourcePath + "', which does not exist.");
        }
        
        // Add a fingerprint to image URIs, which may be used to improve 
        // performance by leveraging caching headers:
        // https://developers.google.com/speed/docs/insights/LeverageBrowserCaching
        String fp = "";
        try {
            // Get the "date last modified" to use as fingerprint - creating image URIs like /my-image.jpg?fp=14561616159
            fp = PARAM_NAME_FINGERPRINT + "=" + String.valueOf(cms.getCmsObject().readResource(imageResourcePath).getDateLastModified());
        } catch (Exception e) {
            throw new ImageAccessException("Error reading 'date last modified' for image '" + imageResourcePath + "': " + e.getMessage());
        }
        
        if (alt == null) {
            try {
                alt = cmso.readPropertyObject(imageResourcePath, CmsPropertyDefinition.PROPERTY_DESCRIPTION, false).getValue("");
            } catch (Exception e) {
                throw new ImageAccessException("Error reading property 'Description' for image '" + imageResourcePath + "': " + e.getMessage());
            }
        } else if (alt.equalsIgnoreCase("none") || alt.equalsIgnoreCase("-")) {
            alt = "";
        }
        
        try {
            CmsImageScaler imageInfo = new CmsImageScaler(cmso, cmso.readResource(imageResourcePath));
			
            // If the given abs. width is larger than the original image's width, adjust the abs. width accordingly (equal to the original image's width)
            if (maxAbsoluteWidth > imageInfo.getWidth())
                    maxAbsoluteWidth = imageInfo.getWidth();
				
            int numImagesGenerated = 0; // Just a precaution ...
            for (int scaleWidth = 400; scaleWidth <= maxAbsoluteWidth && numImagesGenerated < 5; numImagesGenerated++) {
                // Create the URI for this srcset image
                String srcsetElement = imageUri + (isParameterizedImageUri ? "&amp;" : "?")
                                                + "__scale="
                                                + "w:" + scaleWidth
                                                + ",h:" + (getRescaledHeight(cmso, imageUri, scaleWidth))
                                                + ",t:" + scaleType
                                                + ",q:" + quality
                                                + "&amp;" + fp;
                // Optimize the image URI for online?
                //if (cms.getRequestContext().getCurrentProject().isOnlineProject())
                    srcsetElement = cms.link(srcsetElement);
                // Add the image uri to the srcset, along with the width descriptor
                srcset.add(srcsetElement + " " + scaleWidth + "w");
                
                // Break if necessary
                if (scaleWidth >= maxAbsoluteWidth)
                    break; // important! (prevents infinite loop)
                
                // Increase the scale width
                scaleWidth += 400;
                // Constrain the scale width if we exceeded the max
                if (scaleWidth > maxAbsoluteWidth)
                    scaleWidth = maxAbsoluteWidth;
            }
        } catch (Exception e) {
            throw new ImageAccessException("Error creating scaled version of image '" + imageResourcePath + "': " + e.getMessage());
        }
        // All image URIs are now in the srcset list, like so:
        // [0]: "/my/image.jpg?w:400,h:300,t:4,q:90&fp=fdsf1sdf1ds9515fsd19f1sd9f1s
        // [1]: "/my/image.jpg?w:800,h:600,t:4,q:90&fp=fdsf1sdf1ds9515fsd19f1sd9f1s
        // [2]: "/my/image.jpg?w:1200,h:900,t:4,q:90&fp=fdsf1sdf1ds9515fsd19f1sd9f1s
        
        String srcsetString = srcsetToString(srcset);
        
        String srcSize = "";
        String srcFallback = imageUri + (isParameterizedImageUri ? "&amp;" : "?")
                                + "__scale="
                                + "w:" + maxAbsoluteWidth
                                + ",h:" + (getRescaledHeight(cmso, imageUri, maxAbsoluteWidth))
                                + ",t:" + scaleType
                                + ",q:" + quality
                                + "&amp;" + fp;
        // Optimize the image URI for online?
        //if (cms.getRequestContext().getCurrentProject().isOnlineProject())
            srcFallback = cms.link(srcFallback);
        
        String sizes = "";
        if (!srcsetString.isEmpty()) {
            // The "sizes" attribute, determined by the given width info. 
            // (Use a very simple approach: if an image is NOT fullwidth, it will not 
            // occupy more than 50% of the viewport width on large screens.)
            if (size < 0) {
                size = DEFAULT_SIZE;
            }
            if (size < SIZE_L) {
                sizes += "(min-width:" + (linearBreakpoint != null ? linearBreakpoint : DEFAULT_BREAKPOINT) + ") " + DEFAULT_MIN_VP_WIDTH + "vw, "; // widths larger than the breakpoint
            }
            // The default
            sizes += (maxViewportRelativeWidth < 0 ? maxViewportRelativeWidth : DEFAULT_MAX_VP_WIDTH) + "vw";
        }
        
        
        // Construct the img tag
        String img = "<img";
        if (!srcsetString.isEmpty()) {
            img += " srcset=\"" + srcsetString + "\"";
            img += " sizes=\"" + sizes + "\"";
        }
        img += " src=\"" + srcFallback + "\"";
        img += " alt=\"" + alt.replace("\"", "\\\"") + "\"";
        img += " />";
        
        return img;
    }
    
    /**
     * Constructs a string representation of the given srcset list.
     * 
     * @param srcset The srcset list.
     * @return A string representation of the given srcset list.
     */
    protected static String srcsetToString(List<String> srcset) {
        String s = "";
        if (srcset != null && !srcset.isEmpty()) {
            Iterator<String> i = srcset.iterator();
            while (i.hasNext()) {
                s += i.next();
                if (i.hasNext())
                    s += ", ";
            }
        }
        return s;
    }
    /**
     * Convenience/shorthand method: Uses defaults for all non-given values.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @throws ImageAccessException 
     */
    public static synchronized String getImage(CmsJspActionElement cms, String imageUri, String alt, int maxWidth, int size, int quality) throws ImageAccessException {
        return getImage(cms, imageUri, alt, CROP_RATIO_NO_CROP, maxWidth, DEFAULT_MAX_VP_WIDTH, size, quality, DEFAULT_BREAKPOINT);
    }
    /**
     * Convenience/shorthand method: Uses defaults for all non-given values.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @throws ImageAccessException 
     */
    public static synchronized String getImage(CmsJspActionElement cms, String imageUri, String alt, int maxWidth, int size) throws ImageAccessException {
        return getImage(cms, imageUri, alt, maxWidth, size, DEFAULT_QUALITY);
    }
    /**
     * Convenience/shorthand method: Uses defaults for all non-given values.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @throws ImageAccessException 
     */
    public static synchronized String getImage(CmsJspActionElement cms, String imageUri, String alt, int size) throws ImageAccessException {
        return getImage(cms, imageUri, alt, DEFAULT_MAX_WIDTH, size);
    }
    /**
     * Convenience/shorthand method: Uses defaults for all non-given values.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @throws ImageAccessException 
     */
    public static synchronized String getImage(CmsJspActionElement cms, String imageUri, String alt) throws ImageAccessException {
        return getImage(cms, imageUri, alt, DEFAULT_MAX_WIDTH);
    }
    /**
     * Convenience/shorthand method: Uses defaults for all non-given values.
     * 
     * @see ImageUtil#getImage(org.opencms.jsp.CmsJspActionElement, java.lang.String, java.lang.String, java.lang.String, int, int, int, int, java.lang.String) 
     * @throws ImageAccessException 
     */
    public static synchronized String getImage(CmsJspActionElement cms, String imageUri) throws ImageAccessException {
        return getImage(cms, imageUri, null);
    }
    /**
     * Wraps the given content in a figure (with class="media") element.
     * <p>
     * This method can be used to wrap photos, videos, charts, etc. consistently.
     * <p>
     * The caption (if any) is wrapped in a figcaption element with 
     * class="caption highslide-caption".
     * 
     * @param mediaElement The element to wrap, e.g. an img element (but can be anything).
     * @param captionLede The caption lede, a short introductory bit intended to entice the reader to read the full caption.
     * @param caption The caption.
     * @param credit The credit (must include leading "Copyright: ", "Photo: ", etc. bits, if needed).
     * @param mediaExtraClass Extra class(es) that should be assigned to the outer figure element (in addition to its native "media" class).
     * @return 
     */
    public static String getMediaWrapper(String mediaElement, String captionLede, String caption, String credit, String mediaExtraClass) {
        String s = "<figure class=\"media"; 
        if (hasContent(mediaExtraClass)) {
            s += " " + mediaExtraClass;
        }
        s += "\">";
        
        s += mediaElement; // E.g. <img src="cat.jpg" alt="lolcat" />
        
        if (hasContent(captionLede) || hasContent(caption) || hasContent(credit)) {
            s += "<figcaption class=\"caption highslide-caption\">";
            if (hasContent(captionLede)) {
                s += "<span class=\"figure-caption-lede\">" + captionLede + "</span>";
            }
            if (hasContent(caption)) {
                s += caption;
            }
            if (hasContent(credit)) {
                s += "<span class=\"credit figure-credit\">" + credit + "</span>";
            }
            s += "</figcaption>";
        }
        s += "</figure>";
        return s;
    }
    
    public static boolean hasContent(String s) {
        return s != null && s.trim().length() > 0;
    }
}
