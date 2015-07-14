/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package no.npolar.util;

import java.util.ArrayList;
import java.util.Iterator;
import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.db.CmsPublishList;
import org.opencms.db.CmsDefaultUsers;
import org.opencms.main.CmsEvent;
import org.opencms.main.I_CmsEventListener;
//import org.opencms.main.CmsEventManager;
import org.opencms.main.OpenCms;
import org.opencms.main.CmsException;
import org.opencms.module.CmsModule;
import org.opencms.module.I_CmsModuleAction;
import org.opencms.report.I_CmsReport;
//import org.opencms.file.CmsUser;
import org.opencms.file.CmsResource;
//import org.opencms.file.CmsResourceFilter;
//import org.opencms.file.CmsProject;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.loader.CmsLoaderException;

import org.opencms.file.CmsObject;
//import org.opencms.file.CmsFile;

//import org.apache.commons.logging.Log;
//import org.opencms.main.CmsLog;

//import javax.servlet.ServletException;
import java.util.List;
//import org.opencms.file.CmsPropertyDefinition;
import org.opencms.db.CmsResourceState;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.relations.CmsRelationType;
/**
 * 
 * @author Paul-Inge Flakstad <flakstad at npolar.no>
 */
public class Actions implements I_CmsModuleAction, I_CmsEventListener {
    protected static final String DEFAULT_SITE = "/";
    protected static final String PROJECT_OFFLINE_NAME = "Offline";
    
    public void initialize(CmsObject adminCms, CmsConfigurationManager configurationManager, CmsModule module) {
        OpenCms.getEventManager().addCmsEventListener(this);
    }
    
    public void moduleUninstall(CmsModule module) {
        // Do nothing
    }
    
    public void moduleUpdate(CmsModule module) {
        // Do nothing
    }
    
    public void publishProject(CmsObject cms, CmsPublishList publishList, int publishTag, I_CmsReport report) {
        // Do nothing
    }
    
    public void shutDown(CmsModule module) {
        // Do nothing
    }
    
    public void cmsEvent(CmsEvent event) {        
        if (event.getType() == I_CmsEventListener.EVENT_RESOURCE_CREATED /*||
            e.getType() == I_CmsEventListener.EVENT_RESOURCE_DELETED ||
            e.getType() == I_CmsEventListener.EVENT_RESOURCE_COPIED ||
            e.getType() == I_CmsEventListener.EVENT_RESOURCE_MODIFIED ||
            e.getType() == I_CmsEventListener.EVENT_RESOURCE_MOVED*/) {
            
            // Get the source resource
            CmsResource eventResource = (CmsResource)event.getData().get("resource");
            if (eventResource != null) {
                if (eventResource.getTypeId() == CmsResourceTypeImage.getStaticTypeId()) {
                    CmsDefaultUsers defaultUsers = new CmsDefaultUsers();
                    CmsObject cmso = null;
                    List subFolders = null;
                    CmsImageProcessor imgPro = new CmsImageProcessor();
                    boolean isThumbnail = false;
                    CmsResource imageFolderResource = null;
                    CmsResource thumb = null;
                    //CmsUser user = null;
                    try {
                        cmso = OpenCms.initCmsObject(defaultUsers.getUserExport());
                        cmso.loginUser("Imagehandler", "asdfølkj");
                        cmso.getRequestContext().setCurrentProject(cmso.readProject(PROJECT_OFFLINE_NAME));
                        cmso.getRequestContext().setSiteRoot(DEFAULT_SITE);
                    } catch (CmsException cmse) {
                        throw new  NullPointerException("Error initializing CmsObject / user / project upon capturing event 'image resource created': " + cmse.getMessage());
                    }

                    try {
                        List eventResourceRelationSources = cmso.getRelationsForResource(cmso.getSitePath(eventResource), CmsRelationFilter.SOURCES);
                        Iterator relationsItr = eventResourceRelationSources.iterator();
                        while (relationsItr.hasNext()) {
                            if (((CmsRelation)relationsItr.next()).getType().equals(CmsRelationType.valueOf(CmsImageProcessor.RELATION_TYPE_SCALED_IMAGE_VERSION_NAME)))
                                isThumbnail = true;
                        }
                        if (isThumbnail) 
                            return;
                        imageFolderResource = cmso.readResource(CmsResource.getParentFolder(cmso.getSitePath(eventResource)));
                    } catch (CmsException cmse) {
                        throw new  NullPointerException("Error reading image parent folder upon capturing event 'image resource created': " + cmse.getMessage());
                    }

                    try {
                        if (imageFolderResource.getTypeId() == 
                                OpenCms.getResourceManager().getResourceType(CmsImageProcessor.RESOURCE_TYPE_NAME_IMAGEGALLERY).getTypeId()) { // If the image folder is a gallery
                            try {
                                subFolders = cmso.getSubFolders(cmso.getSitePath(imageFolderResource)); // Get the subfolders (thumbnail folders)
                                try {
                                    if (subFolders.size() > 0) { // Image's folder has subfolders
                                        try {
                                            // Need to steal the lock and remove it, so that the relation can be added
                                            cmso.changeLock(cmso.getSitePath(eventResource));
                                            cmso.unlockResource(cmso.getSitePath(eventResource));
                                        } catch (Exception e) {
                                            throw new NullPointerException("Error controlling lock for resource '" 
                                                    + cmso.getSitePath(eventResource) + "': " + e.getMessage());
                                        }
                                        imgPro.generateAllThumbnails(cmso, eventResource);
                                    } // if (image folder has sub-folder(s))
                                } catch (Exception e) {
                                    throw new NullPointerException("Error when generating thumbnail image upon capturing event 'image resource created' " + 
                                            "(thumb=" + thumb + "): " + e.getMessage());
                                } 
                            } catch (CmsException cmse) {
                                throw new  NullPointerException("Error reading subfolders of image folder upon capturing event 'image resource created' (: " + 
                                        cmse.getMessage());
                            } 
                        } // if (image folder is imagegallery)
                    }
                    catch (CmsLoaderException cmsle) {
                        throw new NullPointerException("Could not read the resource type id for '" + 
                                cmso.getSitePath(imageFolderResource) + "': " + cmsle.getMessage());
                    }
                } // if (resource type == image)        
            } // if (r != null)
        } // if (event == resource created)
        
        /*
         // FUNKE IKKJE, INGENTING SKJER:
         else if (event.getType() == I_CmsEventListener.EVENT_RESOURCES_MODIFIED) {
            List eventResources = (List)event.getData().get("resources");
            if (eventResources != null) {
                Iterator resourcesItr = eventResources.iterator();
                while (resourcesItr.hasNext()) {
                    CmsResource eventResource = (CmsResource)resourcesItr.next();
                    if (eventResource != null) {
                        deleteThumbnailsForImage(eventResource);                        
                    } else {
                        throw new NullPointerException("Modified resource was null.");
                    }
                }
            }
        }*/
        
        /* // FUNKE IKKJE, FEIL (KAN IKKJE UNDELETE F.EX.:
         else if (event.getType() == I_CmsEventListener.EVENT_RESOURCE_MODIFIED) {
            CmsResource eventResource = (CmsResource)event.getData().get("resource");
            if (eventResource != null) {
                deleteThumbnailsForImage(eventResource);
            } else {
                throw new NullPointerException("Modified resource was null.");
            }
        }
        */
    }
    
    protected void deleteThumbnailsForImage(CmsResource eventResource) {
        if (eventResource.getState() == CmsResourceState.STATE_DELETED &&
                eventResource.getTypeId() == CmsResourceTypeImage.getStaticTypeId()) {
            CmsDefaultUsers defaultUsers = new CmsDefaultUsers();
            CmsObject cmso = null;
            try {
                cmso = OpenCms.initCmsObject(defaultUsers.getUserExport());
                cmso.loginUser("Imagehandler", "asdfølkj");
                cmso.getRequestContext().setCurrentProject(cmso.readProject(PROJECT_OFFLINE_NAME));
                cmso.getRequestContext().setSiteRoot(DEFAULT_SITE);
            } catch (CmsException cmse) {
                throw new  NullPointerException("Error initializing CmsObject / user / project upon capturing event 'image resource created': " + cmse.getMessage());
            }
            //if (!resourceIsThumbnail(cmso, eventResource)) {
                try {
                    List thumbnails = this.getImageThumbnails(cmso, eventResource);
                    if (!thumbnails.isEmpty()) {
                        Iterator itr = thumbnails.iterator();
                        while (itr.hasNext()) {
                            cmso.deleteResource(cmso.getSitePath((CmsResource)itr.next()), CmsResource.DELETE_PRESERVE_SIBLINGS);
                        }
                    }
                } catch (Exception e) {
                    throw new NullPointerException("Could not delete image thumbnail(s): " + e.getMessage());
                }
            //}
        }
    }
    
    public List getImageThumbnails(CmsObject cmso, CmsResource image) throws CmsException {
        ArrayList thumbs = new ArrayList();
        if (CmsResourceTypeImage.getStaticTypeId() != image.getTypeId()) {
            return thumbs;
        }
        List relationsFromImage = cmso.getRelationsForResource(cmso.getSitePath(image), CmsRelationFilter.TARGETS);
        Iterator i = relationsFromImage.iterator();
        CmsRelation rel = null;
        while (i.hasNext()) {
            rel = (CmsRelation)i.next();
            if (rel.getType().getName().equals(CmsImageProcessor.RELATION_TYPE_SCALED_IMAGE_VERSION_NAME))
                thumbs.add(cmso.readResource(rel.getTargetPath()));
        }
        return thumbs;
    }
    
    protected boolean resourceIsThumbnail(CmsObject cmso, CmsResource resource) {
        if (resource.getTypeId() == CmsResourceTypeImage.getStaticTypeId())
            return false;
        try {
            List eventResourceRelationSources = cmso.getRelationsForResource(cmso.getSitePath(resource), CmsRelationFilter.SOURCES);
            if (eventResourceRelationSources == null)
                return false;
            
            Iterator relationsItr = eventResourceRelationSources.iterator();
            while (relationsItr.hasNext()) {
                if (((CmsRelation)relationsItr.next()).getType().equals(CmsRelationType.valueOf(CmsImageProcessor.RELATION_TYPE_SCALED_IMAGE_VERSION_NAME)))
                    return true;
            }
            return false;
        } catch (CmsException cmse) {
            throw new  NullPointerException("Error determining if image is thumbnail: " + cmse.getMessage());
        }
    }
}