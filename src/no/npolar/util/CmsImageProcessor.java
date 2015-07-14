package no.npolar.util;

import no.npolar.util.exception.*;
import org.opencms.loader.CmsImageScaler;
import org.opencms.db.CmsDefaultUsers;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsFile;
import org.opencms.file.types.*;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.main.*;
import org.opencms.main.CmsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.relations.CmsRelationType;
import org.opencms.util.CmsUUID;

/**
 * CmsImageProcessor provides useful functions for processing images, primarily
 * when generating thumbnails.
 * 
 * When scaling images in OpenCms the "normal" way,
 * there is no path option, meaning you cannot decide yourself where the scaled
 * image is placed. Also, processing time may be of concern, as OpenCms does not
 * save the scaled image as a separate CmsResource, but instead caches it. 
 * The method generateThumbnail() in this class provides a simple solution to place
 * scaled images at specific target locations/folders.
 * 
 * @author Paul-Inge Flakstad
 */
public class CmsImageProcessor extends CmsImageScaler {
    public static final String THUMBNAIL_IMAGE_TITLE_VALUE = "Auto-created thumbnail";
    public static final String THUMBNAIL_FOLDER_TITLE_VALUE = "Auto-created thumbnails";
    public static final String RESOURCE_TYPE_NAME_FOLDER = "folder";
    public static final String RESOURCE_TYPE_NAME_IMAGE = "image";
    public static final String RESOURCE_TYPE_NAME_IMAGEGALLERY = "imagegallery";
    //public static final String PROPERTY_IMAGE_SIZE_NAME = "image.size";
    //public static final String PROPERTY_TITLE_NAME = "Title";
    public static final String FOLDER_URI_ENDING = "/";
    public static final String RELATION_TYPE_SCALED_IMAGE_VERSION_NAME = "SCALED_IMAGE_VERSION";
    public static final int IMAGE_SCALE_TYPE = 3;
    
    /**
     * Creates a new, empty image processor object.
     */
    public CmsImageProcessor() {
        super();
    }
    
    /**
     * Creates a new image scaler for the given image contained in the byte array.
     * 
     * Please note:The image itself is not stored in the scaler, only the width 
     * and height dimensions of the image. To actually scale an image, you need 
     * to use scaleImage(CmsFile). This constructor is commonly used only to extract 
     * the image dimensions, for example when creating a String value for the 
     * CmsPropertyDefinition.PROPERTY_IMAGE_SIZE property.
     * 
     * In case the byte array can not be decoded to an image, or in case of other 
     * errors, isValid() will return false.
     * 
     * @param content  The image to calculate the dimensions for.
     * @param rootPath  The root path of the resource (for error logging)
     */
    public CmsImageProcessor(byte[] content, String rootPath) {
        super(content, rootPath);
    }
    
    /**
     * Creates a new image scaler by reading the property 
     * CmsPropertyDefinition.PROPERTY_IMAGE_SIZE  from the given resource.
     * 
     * In case of any errors reading or parsing the property, isValid() will 
     * return false.
     * 
     * @param cms  the OpenCms user context to use when reading the property
     * @param res  the resource to read the property from
     */
    public CmsImageProcessor(CmsObject cms, CmsResource res) {
        super(cms, res);        
    }
    
    /**
     * Creates a new image scaler based on the given http request.
     * 
     * @param request  the http request to read the parameters from
     * @param maxScaleSize  the maximum scale size (width or height) for the image
     * @param maxBlurSize  the maximum size of the image (width * height) to apply blur (may cause "out of memory" for large images)
     */
    public CmsImageProcessor(HttpServletRequest request, int maxScaleSize, int maxBlurSize) {
        super(request, maxScaleSize, maxBlurSize);
    }
    
    /**
     * Creates a new image scaler based on the given parameter String.
     * 
     * @param parameters  the scale parameters to use
     */
    public CmsImageProcessor(String parameters) {
        super(parameters);
    }
    
    /**
     * Creates a new image scaler based on the given base scaler and the given 
     * width and height.
     * 
     * @param base  the base scaler to initialize the values with
     * @param width  the width to set for this scaler
     * @param height  the height to set for this scaler
     */
    protected CmsImageProcessor(CmsImageScaler base, int width, int height) {
        super(base, width, height);
    }
    
    /**
     * Generates a thumbnail image at a given path. If the thumbnail path is 
     * a folder, the thumbnail image is placed inside this folder, and the name
     * is kept identical to the original's name.<br />
     * 
     * Note that this method utilizes the OpenCms property "image.size" in a 
     * particular way when setting the scaler width and height; it is read from 
     * the new image's target folder. <br />
     * 
     * E.g. if the images inside the folder should be max. 200px wide,"image.size" 
     * should be set to "200". If the image should be constrained in both width
     * and height, "image.size" should be set to e.g. "w:123,h:123".
     * @param imagePath  The path to the original image
     * @param thumbnailPath  The desired path to the thumbnail image
     * @param overwrite  Whether or not to overwrite any old thumbnail image at thumbnailPath
     * @param publishDirectly  Whether or not to publish the resource after creation
     * @return  The resource that is the thumbnail image
     * @throws javax.servlet.ServletException
     * @throws org.opencms.main.CmsException
     * @throws java.lang.IllegalArgumentException
     * @throws no.npolar.util.CmsImageProcessor.MalformedPropertyValueException
     * @throws no.npolar.util.CmsImageProcessor.MissingPropertyException
     * @throws no.npolar.util.CmsImageProcessor.DeleteResourceException
     * @throws no.npolar.util.CmsImageProcessor.PublishException
     */
    /*
    public CmsResource generateThumbnail(String imagePath, String thumbnailPath, boolean overwrite, boolean publishDirectly) throws ServletException, 
                                                                                                CmsException,
                                                                                                IllegalArgumentException,
                                                                                                MalformedPropertyValueException,
                                                                                                MissingPropertyException,
                                                                                                DeleteResourceException, 
                                                                                                PublishException {
        //
        // Initial checks, examine existence and types of files/folders
        //
        CmsDefaultUsers defaultUsers = new CmsDefaultUsers();
        CmsObject cmso = null;
        CmsResource imageFolder = null;
        CmsUser user = null;
        CmsProject project = null;
        try {
            cmso = OpenCms.initCmsObject(defaultUsers.getUserGuest());
            user = cmso.getRequestContext().currentUser();
            cmso.loginUser("", "");
            cmso.getRequestContext().setCurrentProject(cmso.readProject("Offline"));
        } catch (CmsException cmse) {
            throw new NullPointerException("Error initializing CmsObject / user / project upon capturing event 'image resource created': " + 
                cmse.getMessage());
        }
        // If the original image doesn't exists, abort
        if (!cmso.existsResource(imagePath)) {
            throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + imagePath + "', but no such resource exists.");
        }
        // If the original image is in not an image, abort (imagePath could point to a resource of some other type)
        else if (cmso.readResource(imagePath).getTypeId() != CmsResourceTypeImage.getStaticTypeId()) {
            throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + imagePath + "', which is not an image.");
        }
        // If "thumbnailPath" is a folder path, create the thumbnail's full path
        if (cmso.readResource(thumbnailPath).isFolder()) {
            // If the folder does not exist, abort
            if (!cmso.existsResource(thumbnailPath)) { // thumbnailPath is a folder path at this point
                throw new IllegalArgumentException("Thumbnail was requested to be placed in the folder '" + thumbnailPath + "', but no such folder exists.");
            }
            // Create a full path to the thumbnail
            thumbnailPath = thumbnailPath.concat(CmsAgent.getResourceName(imagePath));
        }
        // If the thumbnail resource already exists
        if (cmso.existsResource(thumbnailPath)) {
            // If we don't want to overwrite any existing thumbnail
            if (!overwrite) {
                return cmso.readResource(thumbnailPath); // Return the thumbnail resource
            }
            // If we want to overwrite any existing thumbnail, delete the old one now
            else {
                try {
                    cmso.deleteResource(thumbnailPath, CmsResource.DELETE_PRESERVE_SIBLINGS ); // Delete the resource
                } catch (Exception de) {
                    throw new DeleteResourceException("The resource '" + thumbnailPath + "' could not be deleted. Overwriting failed. Exception was: " + de.getMessage());
                }
                try {
                    OpenCms.getPublishManager().publishResource(cmso, thumbnailPath); // Publish the deletion
                } catch (Exception pe) {
                    throw new PublishException("The resource '" + thumbnailPath + "' could not be published. Exception was: " + pe.getMessage());
                }
            }
        }
        
        
        // Check if the thumbnail folder exists (thumbnailPath is a file path at this point)
        String thumbnailFolder = CmsResource.getParentFolder(thumbnailPath);
        if (!cmso.existsResource(thumbnailFolder)) {
            throw new IllegalArgumentException("Thumbnail was requested to be placed in the folder '" + thumbnailFolder + "', but no such folder exists.");
        }
        
        // Get the original (fullsize) image as a CmsResource
        CmsResource original = cmso.readDefaultFile(imagePath);
        // Get the property "image.size" from the original image. This property holds the width and height of the image.
        int[] imageSize = CmsAgent.getImageSize(cmso.readPropertyObject(original, "image.size", false).getValue());
        // Error check the image's "image.size" property
        if (imageSize == null) {
            throw new MissingPropertyException("Could not find any value for property 'image.size' on the original image file '" + imagePath + "'.");
        }
        else if (imageSize.length != 2) {
            throw new MalformedPropertyValueException("Property value was not of correct format for '" + imagePath + 
                    "': 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'");
        }
        
        // Get the desired scale sizes by reading the property "image.size" on the thumbnail folder
        int[] scaleSize = CmsAgent.getImageSize(cmso, cmso.readResource(thumbnailFolder));
        // Error check the folder's "image.size" property
        if (scaleSize == null) {
            throw new MissingPropertyException("Could not find any value for property 'image.size' on thumbnail folder '" + thumbnailFolder + "'.");
        }
        
        // Return the original image if no downscale is needed
        if (scaleSize.length == 1) { // Check only width
            if (imageSize[0] <= scaleSize[0]) {
                return cmso.readResource(imagePath);
            }
        }
        else if (scaleSize.length == 2) { // Check both width and height
            if (imageSize[0] <= scaleSize[0] && imageSize[1] <= scaleSize[1]) {
                return cmso.readResource(imagePath);
            }
        }
        
        //
        // If the code reaches this point, a downscale is needed 
        //
        
        // Set the scale type and width
        this.setType(3);
        this.setWidth(scaleSize[0]);
        
        // If the scaler is limited by width only, we'll need to calculate the scaler height before setting it, keeping the aspect ratio intact
        if (scaleSize.length == 1) {
            this.setHeight(getNewHeight(scaleSize[0], imageSize[0], imageSize[1]));
        }
        // If the scaler is limited by both width and height, the scaler height attribute can be set directly
        else if (scaleSize.length == 2) {
            this.setHeight(scaleSize[1]);
        }
        
        // Read the existing fullsize image
        CmsFile imageFile = cmso.readFile(imagePath);
        // Get the raw data for a scaled version of the fullsize image
        byte[] imageRawData = this.scaleImage(imageFile);
        // Create a list for resource properties
        ArrayList resourceProperties = new ArrayList(); 
        // Create an empty property
        CmsProperty prop = new CmsProperty();
        // Set the property name and value
        prop.setName(CmsPropertyDefinition.PROPERTY_TITLE);
        prop.setValue("Auto-created thumbnail", CmsProperty.TYPE_INDIVIDUAL);
        // Add the property to the properties list
        resourceProperties.add(prop.cloneAsProperty());
        // Create the downscaled image file as a new CmsResource (virtual file)
        CmsResource thumb = cmso.createResource(thumbnailPath, 
                            org.opencms.file.types.CmsResourceTypeImage.getStaticTypeId(),
                            imageRawData,
                            resourceProperties);
        // Remove any existing locks before publishing (Note: will cause an error if inherited lock exists)
        cmso.unlockResource(thumbnailPath);
        if (publishDirectly) {
            // Publish the new file
            CmsUUID id = null;
            try {
                id = OpenCms.getPublishManager().publishResource(cmso, thumbnailPath);
            }
            catch (java.lang.Exception e) {
                throw new PublishException("Could not publish the resource '" + thumbnailPath + "', error was: " + e.getMessage());
            }
        }
        // Run garbage collector
        java.lang.Runtime.getRuntime().gc();
        System.gc();
        return thumb;
    }
    */
    
    /**
     * Generates a thumbnail image at a given path. If the thumbnail path is 
     * a folder, the thumbnail image is placed inside this folder, and the name
     * is kept identical to the original's name.
     * 
     * Note that this method utilizes the OpenCms property "image.size" in a 
     * particular way when setting the scaler width and height; it is read from 
     * the new image's target folder.
     * 
     * E.g. if the images inside the folder should be max. 200px wide,"image.size" 
     * should be set to "200". If the image should be constrained in both width
     * and height, "image.size" should be set to e.g. "w:123,h:123".
     * @param cmso  CmsObject needed to access OpenCms methods
     * @param imagePath  The path to the original image
     * @param thumbnailPath  The desired path to the thumbnail image
     * @param overwrite  Whether or not to overwrite any old thumbnail image at thumbnailPath
     * @param publishDirectly  Whether or not to publish the resource after creation
     * @return  The resource that is the thumbnail image
     * @throws javax.servlet.ServletException
     * @throws org.opencms.main.CmsException
     * @throws java.lang.IllegalArgumentException
     * @throws no.npolar.util.CmsImageProcessor.MalformedPropertyValueException
     * @throws no.npolar.util.CmsImageProcessor.MissingPropertyException
     * @throws no.npolar.util.CmsImageProcessor.DeleteResourceException
     * @throws no.npolar.util.CmsImageProcessor.PublishException
     */
     public CmsResource generateThumbnail(CmsObject cmso, String imagePath, String thumbnailPath, boolean overwrite, boolean publishDirectly) throws ServletException, 
                                                                                                CmsException,
                                                                                                IllegalArgumentException,
                                                                                                MalformedPropertyValueException,
                                                                                                MissingPropertyException,
                                                                                                DeleteResourceException, 
                                                                                                PublishException {
        //
        // Initial checks, examine existence and types of files/folders
        //
        
        // If the original resource doesn't exists, abort
        if (!cmso.existsResource(imagePath)) {
            throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + imagePath + "', but no such resource exists.");
        }
        // If the original "image" is in not an image, abort (imagePath could point to a resource of some other type)
        else if (cmso.readResource(imagePath).getTypeId() != CmsResourceTypeImage.getStaticTypeId()) {
            throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + imagePath + "', which is not an image.");
        }
        // If "thumbnailPath" is a folder path, create the thumbnail's full path
        boolean pathIsFolderPath = thumbnailPath.endsWith("/");//cmso.readResource(thumbnailPath).isFolder();
        
        if (pathIsFolderPath) {
            // If the folder does not exist, abort
            if (!cmso.existsResource(thumbnailPath)) { // thumbnailPath is a folder path at this point
                throw new IllegalArgumentException("Thumbnail was requested to be placed in the folder '" + thumbnailPath + "', but no such folder exists.");
            }
            // Create a full path to the thumbnail
            thumbnailPath = thumbnailPath.concat(CmsAgent.getResourceName(imagePath));
        }
        // thumbnailPath is now definitively not a path to a folder
        
        // If the thumbnail resource already exists
        if (cmso.existsResource(thumbnailPath)) {
            // If we don't want to overwrite any existing thumbnail
            if (!overwrite) {
                return cmso.readResource(thumbnailPath); // Return the thumbnail resource
            }
            // If we want to overwrite any existing thumbnail, delete the old one now
            else {
                try {
                    cmso.deleteResource(thumbnailPath, CmsResource.DELETE_PRESERVE_SIBLINGS ); // Delete the resource
                } catch (Exception de) {
                    throw new DeleteResourceException("The resource '" + thumbnailPath + "' could not be deleted. Overwriting failed. Exception was: " + de.getMessage());
                }
                try {
                    OpenCms.getPublishManager().publishResource(cmso, thumbnailPath); // Publish the deletion
                } catch (Exception pe) {
                    // Doing nothing, this should be caused by an unpublished parent folder. Puslishing will have to be done manually
                    //throw new PublishException("The resource '" + thumbnailPath + "' could not be published. Exception was: " + pe.getMessage());
                }
            }
        }
        
        // Check if the thumbnail folder exists (thumbnailPath is a file path at this point)
        String thumbnailFolder = CmsResource.getParentFolder(thumbnailPath);
        
        if (!cmso.existsResource(thumbnailFolder)) {
            throw new IllegalArgumentException("Thumbnail was requested to be placed in the folder '" + thumbnailFolder + "', but no such folder exists.");
        }
        
        CmsResource thumb = null;
        // Get the original (fullsize) image as a CmsResource
        CmsResource original = cmso.readResource(imagePath);//readDefaultFile(imagePath);
        // Get the property "image.size" from the original image. This property holds the width and height of the image.
        int[] imageSize = CmsAgent.getImageSize(cmso.readPropertyObject(original, "image.size", false).getValue());
        // Error check the image's "image.size" property
        if (imageSize == null) {
            throw new MissingPropertyException("Could not find any value for property 'image.size' on the original image file '" + imagePath + "'.");
        }
        else if (imageSize.length != 2) {
            throw new MalformedPropertyValueException("Property value was not of correct format for '" + imagePath + 
                    "': 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'");
        }
        
        // Get the desired scale sizes by reading the property "image.size" on the thumbnail folder
        int[] scaleSize = CmsAgent.getImageSize(cmso, cmso.readResource(thumbnailFolder));
        // Error check the folder's "image.size" property
        if (scaleSize == null) {
            throw new MissingPropertyException("Could not find any value for property 'image.size' on thumbnail folder '" + thumbnailFolder + "'.");
        }
        
        // Return the original image if no downscale is needed
        if (scaleSize.length == 1) { // Check only width
            if (imageSize[0] <= scaleSize[0]) {
                return cmso.readResource(imagePath);
            }
        }
        else if (scaleSize.length == 2) { // Check both width and height
            if (imageSize[0] <= scaleSize[0] && imageSize[1] <= scaleSize[1]) {
                return cmso.readResource(imagePath);
            }
        }
        
        //
        // If the code reaches this point, a downscale is needed 
        //
        
        // Set the scale type and width
        this.setType(3);
        this.setWidth(scaleSize[0]);
        
        // If the scaler is limited by width only, we'll need to calculate the scaler height before setting it, keeping the aspect ratio intact
        if (scaleSize.length == 1) {
            this.setHeight(getNewHeight(scaleSize[0], imageSize[0], imageSize[1]));
        }
        // If the scaler is limited by both width and height, the scaler height attribute can be set directly
        else if (scaleSize.length == 2) {
            this.setHeight(scaleSize[1]);
        }
        
        // Read the existing fullsize image
        CmsFile imageFile = cmso.readFile(imagePath);
        // Get the raw data for a scaled version of the fullsize image
        byte[] imageRawData = this.scaleImage(imageFile);
        // Create a list for resource properties
        ArrayList resourceProperties = new ArrayList(); 
        // Create an empty property
        CmsProperty prop = new CmsProperty();
        // Set the property name and value
        prop.setName(CmsPropertyDefinition.PROPERTY_TITLE);
        prop.setValue(THUMBNAIL_IMAGE_TITLE_VALUE, CmsProperty.TYPE_INDIVIDUAL);
        // Add the property to the properties list
        resourceProperties.add(prop.cloneAsProperty());
        
        // Create the downscaled image file as a new CmsResource (virtual file)
        try {
            //thumb =  cmso.createResource("/lekestue/images/thumbnails/myfile.txt", 
            thumb =  cmso.createResource(thumbnailPath, 
                                    OpenCms.getResourceManager().getResourceType("image").getTypeId(),
                                    imageRawData,
                                    resourceProperties);// Create the resource as a text file
            //thumb.setType(OpenCms.getResourceManager().getResourceType("image").getTypeId()); // Then set it to be of type image
            //return thumb;
        } catch (Exception e) {
            throw new NullPointerException("Thumbnail resource creation failed. " + e.getMessage());
        }
        
        // Remove any existing locks before publishing (Note: will cause an error if inherited lock exists)
        cmso.unlockResource(thumbnailPath);
        if (publishDirectly) {
            // Publish the new file
            CmsUUID id = null;
            try {
                id = OpenCms.getPublishManager().publishResource(cmso, thumbnailPath);
            }
            catch (java.lang.Exception e) {
                // Doing nothing, this should be caused by an unpublished parent folder. Puslishing will have to be done manually
                //throw new PublishException("Could not publish the resource '" + thumbnailPath + "', error was: " + e.getMessage());
            }
        }
        // Run garbage collector
        java.lang.Runtime.getRuntime().gc();
        System.gc();
        return thumb;
    }
    
    public CmsResource generateThumbnail(CmsAgent cms, String fullsizePath, String thumbnailFolder)throws ServletException, 
                                                                            CmsException,
                                                                            IllegalArgumentException,
                                                                            MalformedPropertyValueException,
                                                                            MissingPropertyException,
                                                                            DeleteResourceException, 
                                                                            PublishException {
        return generateThumbnail(cms.getCmsObject(), fullsizePath, thumbnailFolder);
    }
    /**
     * Generates a thumbnail image in a given folder. If the folder does not 
     * exist, it will be created. The image.size property for this folder will
     * be fetched from the CmsImageProcessor instance itself, so it must be set
     * prior to calling this method, using setWidtht() and/or setHeight(). A 
     * relation of type REFERENCED_IMAGE will also be created from the fullsize
     * to the thumbnail image. This will only happen if the relation type has 
     * been defined.
     * 
     * Note that this method utilizes the OpenCms property "image.size" in a 
     * particular way: if the images inside the folder should be max. 200px wide,
     * "image.size" should be set to "200". If the image should be constrained 
     * in both width and height, "image.size" should be set to e.g. "w:123,h:123".
     * 
     * @param cms  Action element object needed to access pageContext and CmsObject
     * @param fullsizePath the path to the image to create a thumbnail of
     * @param thumbnailFolder the path to the thumbnail folder (the folder will be created if it doesn't exist)
     * @return the created thumbnail image resource
     * @throws javax.servlet.ServletException
     * @throws org.opencms.main.CmsException
     * @throws java.lang.IllegalArgumentException
     * @throws no.npolar.util.CmsImageProcessor.MalformedPropertyValueException
     * @throws no.npolar.util.CmsImageProcessor.MissingPropertyException
     * @throws no.npolar.util.CmsImageProcessor.DeleteResourceException
     * @throws no.npolar.util.CmsImageProcessor.PublishException
     */
    public CmsResource generateThumbnail(CmsObject cmso, 
                                            String fullsizePath, 
                                            String thumbnailFolder) throws ServletException, 
                                                                            CmsException,
                                                                            IllegalArgumentException,
                                                                            MalformedPropertyValueException,
                                                                            MissingPropertyException,
                                                                            DeleteResourceException, 
                                                                            PublishException {
        //
        // Initial checks, examine existence and types of files/folders
        //
        //CmsObject cmso = cms.getCmsObject();
        
        // NB: Cannot use CmsResource's isFolder() method, because the resource might not exist yet
        boolean pathIsFolderPath = thumbnailFolder.endsWith(FOLDER_URI_ENDING);
        // If the supplied folder path is not a folder path
        if (!pathIsFolderPath) {
            throw new IllegalArgumentException(Messages.get().container(Messages.ERR_INVALID_FOLDER_URI_1, thumbnailFolder).key());
            //throw new IllegalArgumentException("The URI '" + thumbnailFolder + "' is not a valid folder URI. (A folder URI must end with '/'.)");
        }
        
        // Create a full path to the thumbnail
        String thumbnailPath = thumbnailFolder.concat(CmsAgent.getResourceName(fullsizePath));
        
        // If a resource with the desired thumbnail path already exists, return it
        if (cmso.existsResource(thumbnailPath)) {
            return cmso.readResource(thumbnailPath);
        }
        
        // CmsResource.STATE_UNCHANGED:
        // Indicates if a resource is unchanged in the offline version when compared to the online version.
        boolean fullsizeImageIsStateUnchanged = cmso.readResource(fullsizePath).getState().equals(CmsResource.STATE_UNCHANGED);
        
        //boolean onlineProject = cms.getRequestContext().currentProject().isOnlineProject();
        
        //if (onlineProject) {
        //    cmso.loginUser("", "");
        //    cmso.getRequestContext().setCurrentProject(cmso.readProject("Offline"));
        //    cmso.getRequestContext().setSiteRoot(cms.getRequestContext().getSiteRoot());
        //}
        
        // If the original resource doesn't exists, abort
        if (!cmso.existsResource(fullsizePath)) {
            throw new IllegalArgumentException(Messages.get().container(Messages.ERR_RESOURCE_NOT_FOUND_1, fullsizePath).key());
            //throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + fullsizePath + "', but no such resource exists.");
        }
        // If the original "image" is in not an image, abort (imagePath could point to a resource of some other type)
        else if (cmso.readResource(fullsizePath).getTypeId() != CmsResourceTypeImage.getStaticTypeId()) {
            throw new IllegalArgumentException(Messages.get().container(Messages.ERR_INVALID_RESOURCE_TYPE_FOR_IMAGE_1, fullsizePath).key());
            //throw new IllegalArgumentException("Thumbnail was requested to be generated from '" + fullsizePath + "', which is not an image.");
        }
        
        // If the thumbnail folder does not exist, create it
        if (!cmso.existsResource(thumbnailFolder)) {
            CmsResource thumbnailFolderResource = null;

            try {
                thumbnailFolderResource = cmso.createResource(thumbnailFolder, 
                                                                OpenCms.getResourceManager().getResourceType(RESOURCE_TYPE_NAME_FOLDER).getTypeId());
                cmso.lockResource(cmso.getSitePath(thumbnailFolderResource));
                cmso.writeResource(thumbnailFolderResource);
                cmso.unlockResource(cmso.getSitePath(thumbnailFolderResource));
            } catch (Exception e) {
                throw new ServletException(Messages.get().container(Messages.ERR_RESOURCE_CREATION_FAILED_2, thumbnailFolder, e.getMessage()).key());
                //throw new ServletException("Thumbnail folder creation failed: " + e.getMessage());
            }
            try {
                //CmsProperty prop = cmso.readPropertyObject(tf, "image.size", false);
                //prop.setValue(Integer.toString(imgPro.getWidth()), CmsProperty.TYPE_INDIVIDUAL);
                cmso.lockResource(cmso.getSitePath(thumbnailFolderResource));
                cmso.writePropertyObject(cmso.getSitePath(thumbnailFolderResource), 
                                            new CmsProperty(CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, 
                                                            Integer.toString(this.getWidth()), 
                                                            Integer.toString(this.getWidth())));
                cmso.writeResource(thumbnailFolderResource);
                cmso.writePropertyObject(cmso.getSitePath(thumbnailFolderResource),
                                            new CmsProperty(CmsPropertyDefinition.PROPERTY_TITLE, 
                                                            null, 
                                                            THUMBNAIL_FOLDER_TITLE_VALUE));
                cmso.unlockResource(cmso.getSitePath(thumbnailFolderResource));
            } catch (Exception e) {
                throw new ServletException(Messages.get().container(Messages.ERR_PROP_UPDATE_FAILED_2, thumbnailFolder, e.getMessage()).key());
                //throw new ServletException("Thumbnail property update failed: " + e.getMessage());
            }
            try {
                OpenCms.getPublishManager().publishResource(cmso, cmso.getSitePath(thumbnailFolderResource));
            } catch (Exception e) {
                throw new PublishException(Messages.get().container(Messages.ERR_RESOURCE_CREATED_NOT_PUBLISHED_2, e.getMessage()).key());
                //throw new PublishException("Thumbnail folder was created, but not published: " + e.getMessage());
            }
        }
        
        //
        // The thumbnail folder now exists
        //
        
        CmsResource thumbnailResource = null;
        // Get the original (fullsize) image as a CmsResource
        CmsResource fullsizeResource = cmso.readResource(fullsizePath);//readDefaultFile(imagePath);
        // Get the property "image.size" from the original image. This property holds the width and height of the image.
        int[] imageSize = CmsAgent.getImageSize(cmso.readPropertyObject(fullsizeResource, 
                                                                        CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, false).getValue());
        // Error check the image's "image.size" property
        if (imageSize == null) {
            throw new PublishException(Messages.get().container(Messages.ERR_MISSING_PROPERTY_VALUE_2, 
                                                                CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, fullsizePath).key());
            //throw new MissingPropertyException("Could not find any value for property 'image.size' on the original image file '" + fullsizePath + "'.");
        }
        else if (imageSize.length != 2) {
            throw new PublishException(Messages.get().container(Messages.ERR_BAD_IMAGE_SIZE_PROPERTY_FORMAT_1, fullsizePath).key());
            //throw new MalformedPropertyValueException("Property value was not of correct format for '" + fullsizePath + 
            //        "': 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'");
        }
        
        // Get the desired scale sizes by reading the property "image.size" on the thumbnail folder
        int[] scaleSize = CmsAgent.getImageSize(cmso, cmso.readResource(thumbnailFolder));
        // Error check the folder's "image.size" property
        if (scaleSize == null) {
            throw new MissingPropertyException(Messages.get().container(Messages.ERR_MISSING_PROPERTY_VALUE_2, 
                                                                        CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, thumbnailFolder).key());
            //throw new MissingPropertyException("Could not find any value for property 'image.size' on thumbnail folder '" + thumbnailFolder + "'.");
        }
        
        // Return the original image if no downscale is needed
        if (scaleSize.length == 1) { // Check only width
            if (imageSize[0] <= scaleSize[0]) {
                return cmso.readResource(fullsizePath);
            }
        }
        else if (scaleSize.length == 2) { // Check both width and height
            if (imageSize[0] <= scaleSize[0] && imageSize[1] <= scaleSize[1]) {
                return cmso.readResource(fullsizePath);
            }
        }
        
        //
        // If the code reaches this point, a downscale is needed
        //
        
        // Set the scale type and width
        this.setType(IMAGE_SCALE_TYPE);
        this.setWidth(scaleSize[0]);
        
        // If the scaler is limited by width only, we'll need to calculate the scaler height before setting it, keeping the aspect ratio intact
        if (scaleSize.length == 1) {
            this.setHeight(getNewHeight(scaleSize[0], imageSize[0], imageSize[1]));
        }
        // If the scaler is limited by both width and height, the scaler height attribute can be set directly
        else if (scaleSize.length == 2) {
            this.setHeight(scaleSize[1]);
        }
        
        // Read the existing fullsize image
        CmsFile imageFile = cmso.readFile(fullsizePath);
        // Get the raw data for the downscaled version of the fullsize image
        byte[] imageRawData = this.scaleImage(imageFile);
        // Create a list for resource properties
        ArrayList resourceProperties = new ArrayList(); 
        // Create an empty property
        CmsProperty prop = new CmsProperty();
        // Set the property name and value
        prop.setName(CmsPropertyDefinition.PROPERTY_TITLE);
        prop.setValue(THUMBNAIL_IMAGE_TITLE_VALUE, CmsProperty.TYPE_INDIVIDUAL);
        // Add the property to the properties list
        resourceProperties.add(prop.cloneAsProperty());
        
        // Create the downscaled image as a new CmsResource of type "image"
        try { 
            thumbnailResource =  cmso.createResource(thumbnailPath, 
                                    OpenCms.getResourceManager().getResourceType(RESOURCE_TYPE_NAME_IMAGE).getTypeId(),
                                    imageRawData,
                                    resourceProperties);
        } catch (Exception e) {
            throw new NullPointerException(Messages.get().container(Messages.ERR_RESOURCE_CREATION_FAILED_2, thumbnailPath, e.getMessage()).key());
            //throw new NullPointerException("Thumbnail resource creation failed: " + e.getMessage());
        }
        
        //
        // Thumbnail resource creation done, ready to publish
        //
        
        // Remove any existing locks before publishing 
        // (Note: will cause an error if inherited lock exists)
        if (!cmso.getLock(thumbnailPath).isUnlocked())
            cmso.unlockResource(thumbnailPath);
        /*if (!cmso.getLock(thumbnailPath).isInherited()) {
            cmso.unlockResource(thumbnailPath);
            
            // Publish thumbnail if fullsize image is published
            if (fullsizeImageIsStateUnchanged) {
                CmsUUID id = null;
                try {
                    id = OpenCms.getPublishManager().publishResource(cmso, thumbnailPath);
                }
                catch (java.lang.Exception e) {
                    throw new PublishException(Messages.get().container(Messages.ERR_PUBLISH_RESOURCE_FAILED_1, e.getMessage()).key());
                    //throw new PublishException("Could not publish the resource '" + thumbnailPath + "': " + e.getMessage());
                }
            }
        }*/
        
        //
        // Create a relation between the image and the thumbnail, if possible
        //
        CmsRelationType referencedImage = null;
        try {
            referencedImage = CmsRelationType.valueOf(RELATION_TYPE_SCALED_IMAGE_VERSION_NAME);
        } catch (IllegalArgumentException iae) {
            // Do nothing, relation type REFERENCED_IMAGE has not been configured
        }
        if (referencedImage != null) {
            try {
                if (cmso.getLock(fullsizePath).isNullLock())
                    cmso.lockResource(fullsizePath);
                else {
                    throw new NullPointerException("Unable to set lock on resource '" + fullsizePath + "'.");
                }
                cmso.addRelationToResource(fullsizePath, thumbnailPath, referencedImage.getName());
                if (!cmso.getLock(fullsizePath).isUnlocked())
                    cmso.unlockResource(fullsizePath);
                // Publish the fullsize image if it was unchanged prior to adding the relation
                /*if (fullsizeImageIsStateUnchanged && !cmso.getLock(fullsizePath).isInherited()) {
                    cmso.unlockResource(fullsizePath);
                    try {
                        OpenCms.getPublishManager().publishResource(cmso, fullsizePath);
                    }
                    catch (java.lang.Exception e) {
                        throw new PublishException(Messages.get().container(Messages.ERR_PUBLISH_AFTER_RELATE_FAILED_2, 
                                                                            fullsizePath, e.getMessage()).key());
                        //throw new PublishException("Could not publish the resource '" + fullsizePath + 
                        //        "' after having added relation to thumbnail: " + e.getMessage());
                    }
                }*/
            } catch (CmsException cmse) {
                throw new CmsException(Messages.get().container(Messages.ERR_ADD_RELATION_FAILED_4, 
                                                                new Object[] {referencedImage.getName(), fullsizePath, thumbnailPath, cmse.getMessage()}));
                //throw new NullPointerException("Could not add a " + referencedImage.getName() + " type relation between " + fullsizePath + " and " + thumbnailPath + ": " + cmse.getMessage());
                //throw new CmsException("Could not add a relation between the fullsize image and its thumbnail: " + cmse.getMessage());
            }
        }
        // Run garbage collector
        java.lang.Runtime.getRuntime().gc();
        System.gc();
        
        //if (onlineProject) 
        //    cms.getRequestContext().setCurrentProject(cmso.readProject("Online"));
        
        return thumbnailResource;
    }
    
    /**
     * Generates a thumbnail image in each folder that is on the same level as the
     * original image. In other words: this method assumes that all folders residing 
     * inside the parent folder of the original image are thumbnail folders.
     * @param imagePath  The path to the original image
     * @param overwrite  Whether or not to overwrite any existing thumbnail images
     * @return  List of generated thumbnails. Each item in the list is a CmsResource.
     * @throws javax.servlet.ServletException
     * @throws org.opencms.main.CmsException
     * @throws java.lang.IllegalArgumentException
     * @throws no.npolar.util.CmsImageProcessor.MalformedPropertyValueException
     * @throws no.npolar.util.CmsImageProcessor.MissingPropertyException
     * @throws no.npolar.util.CmsImageProcessor.DeleteResourceException
     * @throws no.npolar.util.CmsImageProcessor.PublishException
     */
    /*
    public List generateAllThumbnails(String imagePath, boolean overwrite, boolean publishDirectly)throws ServletException, 
                                                                                            CmsException,
                                                                                            IllegalArgumentException,
                                                                                            MalformedPropertyValueException,
                                                                                            MissingPropertyException,
                                                                                            DeleteResourceException, 
                                                                                            PublishException {
        CmsDefaultUsers defaultUsers = new CmsDefaultUsers();
        CmsObject cmso = null;
        CmsResource imageFolder = null;
        CmsUser user = null;
        CmsProject project = null;
        try {
            cmso = OpenCms.initCmsObject(defaultUsers.getUserGuest());
            user = cmso.getRequestContext().currentUser();
            cmso.loginUser("", "");
            cmso.getRequestContext().setCurrentProject(cmso.readProject("Offline"));
        } catch (CmsException cmse) {
            throw new NullPointerException("Error initializing CmsObject / user / project upon capturing event 'image resource created': " + cmse.getMessage());
        }
        
        // List of subfolders + iterator
        List thumbsFolders          = cmso.getSubFolders(CmsResource.getFolderPath(imagePath));
        Iterator i                  = thumbsFolders.iterator();
        // Some containers and help variables
        CmsResource thumbnailFolder = null;
        CmsResource thumb           = null;
        List thumbs                 = new ArrayList();
        // Loop over all the subfolders, put a thumbnail in each
        while (i.hasNext()) {
            thumbnailFolder = (CmsResource)i.next();
            thumb = generateThumbnail(cmso, imagePath, cmso.getSitePath(thumbnailFolder), overwrite, publishDirectly);
            thumbs.add(thumb);
        }
        // Return the list of thumbnails
        return thumbs;
    }
    */
    
    /**
     * Generates a thumbnail image in each folder that is on the same level as the
     * original image. In other words: this method assumes that all folders residing 
     * inside the parent folder of the original image are thumbnail folders.
     * @param cmso  CmsObject reference needed to access OpenCms methods
     * @param imagePath  The path to the original image
     * @param overwrite  Whether or not to overwrite any existing thumbnail images
     * @param publishDirectly  Whether or not to publish the resource after creation
     * @return  List of generated thumbnails. Each item in the list is a CmsResource.
     * @throws javax.servlet.ServletException
     * @throws org.opencms.main.CmsException
     * @throws java.lang.IllegalArgumentException
     * @throws no.npolar.util.CmsImageProcessor.MalformedPropertyValueException
     * @throws no.npolar.util.CmsImageProcessor.MissingPropertyException
     * @throws no.npolar.util.CmsImageProcessor.DeleteResourceException
     * @throws no.npolar.util.CmsImageProcessor.PublishException
     */
    public List generateAllThumbnails(CmsObject cmso, String imagePath, boolean overwrite, boolean publishDirectly)throws ServletException, 
                                                                                            CmsException,
                                                                                            IllegalArgumentException,
                                                                                            MalformedPropertyValueException,
                                                                                            MissingPropertyException,
                                                                                            DeleteResourceException, 
                                                                                            PublishException {
        // List of subfolders + iterator
        List thumbsFolders          = cmso.getSubFolders(CmsResource.getFolderPath(imagePath));
        Iterator i                  = thumbsFolders.iterator();
        // Some containers and help variables
        CmsResource thumbnailFolder = null;
        CmsResource thumb           = null;
        List thumbs                 = new ArrayList();
        // Loop over all the subfolders, put a thumbnail in each
        while (i.hasNext()) {
            thumbnailFolder = (CmsResource)i.next();
            thumb = generateThumbnail(cmso, imagePath, cmso.getSitePath(thumbnailFolder), overwrite, publishDirectly);
            thumbs.add(thumb);
        }
        // Return the list of thumbnails
        return thumbs;
    }
    
    public List generateAllThumbnails(CmsObject cmso, CmsResource imageResource) throws ServletException, 
                                                                                            CmsException,
                                                                                            IllegalArgumentException,
                                                                                            MalformedPropertyValueException,
                                                                                            MissingPropertyException,
                                                                                            DeleteResourceException, 
                                                                                            PublishException {
        String imagePath            = cmso.getSitePath(imageResource);
        // List of subfolders + iterator
        List thumbsFolders          = cmso.getSubFolders(CmsResource.getFolderPath(imagePath));
        Iterator i                  = thumbsFolders.iterator();
        // Some containers and help variables
        CmsResource thumbnailFolder = null;
        CmsResource thumb           = null;
        List thumbs                 = new ArrayList();
        // Loop over all the subfolders, put a thumbnail in each
        while (i.hasNext()) {
            thumbnailFolder = (CmsResource)i.next();
            thumb = generateThumbnail(cmso, imagePath, cmso.getSitePath(thumbnailFolder));
            thumbs.add(thumb);
        }
        // Return the list of thumbnails
        return thumbs;
    }
    
    /**
     * Calculates a new height based on the new width, preserving the aspect ratio.
     * @param newWidth  The new width
     * @param width  The original width
     * @param height  The original height
     * @return  The new height
     */
    public int getNewHeight(int newWidth, int width, int height ) {
        double newHeight = 0.0;
        double ratio = 0.0;
        ratio = (double)width / newWidth; // IMPORTANT! Cast one to double, or ratio will get an integer value..!!!
        newHeight = (double)height / ratio;
        return (int)newHeight;
    }
}
