package no.npolar.util;

import org.opencms.i18n.A_CmsMessageBundle;
import org.opencms.i18n.I_CmsMessageBundle;

/**
 * Convenience class to access the localized messages of this OpenCms package.<p>
 * 
 * @author Paul-Inge Flakstad  <flakstad at npolar.no>
 */
public final class Messages extends A_CmsMessageBundle {

    /** Message constant for key in the resource bundle. */
    public static final String GUI_RELATION_TYPE_REFERENCED_IMAGE_0 = "GUI_RELATION_TYPE_REFERENCED_IMAGE_0";
    
    /** Message constant for key in the resource bundle. */
    public static final String ERR_RESOURCE_NOT_FOUND_1 = "ERR_RESOURCE_NOT_FOUND_1";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_INVALID_RESOURCE_TYPE_FOR_IMAGE_1 = "ERR_INVALID_RESOURCE_TYPE_FOR_IMAGE_1";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_INVALID_FOLDER_URI_1 = "ERR_INVALID_FOLDER_URI_1";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_RESOURCE_CREATION_FAILED_2 = "ERR_RESOURCE_CREATION_FAILED_2";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_PROP_UPDATE_FAILED_2 = "ERR_PROP_UPDATE_FAILED_2";
    /** Message constant for key in the resource bundle. */
    public static final String ERR_RESOURCE_CREATED_NOT_PUBLISHED_2 = "ERR_RESOURCE_CREATED_NOT_PUBLISHED_2";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_MISSING_PROPERTY_VALUE_2 = "ERR_MISSING_PROPERTY_VALUE_2";
                
    /** Message constant for key in the resource bundle. */
    public static final String ERR_BAD_IMAGE_SIZE_PROPERTY_FORMAT_1 = "ERR_BAD_IMAGE_SIZE_PROPERTY_FORMAT_1";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_PUBLISH_RESOURCE_FAILED_1 = "ERR_PUBLISH_RESOURCE_FAILED_1";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_PUBLISH_AFTER_RELATE_FAILED_2 = "ERR_PUBLISH_AFTER_RELATE_FAILED_2";
            
    /** Message constant for key in the resource bundle. */
    public static final String ERR_ADD_RELATION_FAILED_4 = "ERR_ADD_RELATION_FAILED_4";

    /** Name of the used resource bundle. */
    private static final String BUNDLE_NAME = "no.npolar.util.messages";

    /** Static instance member. */
    private static final I_CmsMessageBundle INSTANCE = new Messages();

    /**
     * Hides the public constructor for this utility class.<p>
     */
    private Messages() {
        // hide the constructor
    }

    /**
     * Returns an instance of this localized message accessor.<p>
     * 
     * @return an instance of this localized message accessor
     */
    public static I_CmsMessageBundle get() {
        return INSTANCE;
    }

    /**
     * Returns the bundle name for this OpenCms package.<p>
     * 
     * @return the bundle name for this OpenCms package
     */
    public String getBundleName() {
        return BUNDLE_NAME;
    }
}
