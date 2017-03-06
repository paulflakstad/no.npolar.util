package no.npolar.util.init;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsResourceInitException;
import org.opencms.main.I_CmsResourceInit;
import org.opencms.main.OpenCms;

/**
 * Initializer for URIs that have no corresponding VFS resource, but do have 
 * a defined default renderer template.
 * <p>
 * For example: Employees, publications and projects - all of which have key 
 * details stored in the Data Centre – can have a default rendering produced,
 * based purely on their Data Centre entry. To access it, we need only an ID.
 * <p>
 * Consider for example employees:
 * <p>
 * All key data for an employee are stored in the Data Centre, and in order to 
 * create a default employee page, we need only the person template and the Data 
 * Centre ID. No person file should be necessary. (It wouldn't be used for  
 * anything, and would be just a placeholder - a "dummy" file.)
 * <p>
 * However, we still want the option to expand on and/or customize the default 
 * person page. These cases are the ones when we <em>actually</em> do need a 
 * person file in the VFS. That file would be created as the index file of a 
 * folder named exactly like the Data Centre ID - e.g. 
 * <code>[employees root folder]/jane.doe/index.html</code>
 * <p>
 * This class facilitates such an approach: It evaluates the URI and the VFS, 
 * and sets some request attributes, if necessary, which in turn can be read by 
 * the rendering template (e.g. person template), and other system files.
 * <p>
 * To activate this initializer in OpenCms, it must be added to the config file 
 * <code>opencms-system.xml</code>, as a child of the <code>resourceinit</code>
 * node, using the fully qualified class name.
 * <p>
 * To use it, the property {@link #PROPERTY_TEMPLATE_DEFAULT} must be defined on
 * an actual parent/ancestor folder in the OpenCms VFS. It should point to a 
 * template file in the VFS (e.g. 
 * <code>/system/modules/no.npolar.common.person/elements/person.jsp</code>) 
 * , which must be capable of handling the default rendering, using the request 
 * attributes set by this class.
 * 
 * @see http://www.opencms.org/export/sites/opencms/en/events/opencms_days_2009/slides/t8.pdf
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 */
public class DefaultRendererInit implements I_CmsResourceInit {

    /** Class logger. */
    private final static Log LOG = LogFactory.getLog(DefaultRendererInit.class);
    
    /** The name of the request attribute that will hold the external ID, for example an API ID. */
    public static final String ATTR_NAME_ID_EXTERNAL = "external_id";
    /** The name of the request attribute that will hold the originally requested URI. */
    public static final String ATTR_NAME_REQUESTED_URI = "requested_uri";
    /** The name of the request attribute that will hold the originally requested URI. */
    public static final String ATTR_NAME_CANONICAL_URI = "canonical_uri";
    /** The name of the request attribute that will hold the parent folder URI. */
    public static final String ATTR_NAME_REQUESTED_FOLDER_URI = "requested_folder_uri";
    
    /** The name of the property that holds the URI to the renderer template. */
    public static final String PROPERTY_TEMPLATE_DEFAULT = "template.default";
    
    @Override
    public CmsResource initResource(CmsResource resource, 
            CmsObject cmso, 
            HttpServletRequest request, 
            HttpServletResponse response) throws CmsResourceInitException {

        // Make sure the request is real
        if (request != null) {
            String uri = cmso.getRequestContext().getUri();
            
            // We're only interested in URIs that don't map to any VFS resource
            if (!cmso.existsResource(uri)) {
                try {
                    String vfsFolderUri = getClosestRealVfsPath(cmso, uri);
                    
                    // Check to see if the required property is set (the path
                    // to the default template)
                    String defaultTemplateUri = cmso.readPropertyObject(vfsFolderUri, PROPERTY_TEMPLATE_DEFAULT, true).getValue(null);

                    // If it no default template was set, we abort
                    if (defaultTemplateUri != null) {
                        if (!cmso.existsResource(defaultTemplateUri) ) {
                            throw new NullPointerException("Found '" + defaultTemplateUri + "'"
                                    + " as '" + PROPERTY_TEMPLATE_DEFAULT + "' value"
                                    + " on folder '" + vfsFolderUri + "'"
                                    + ", but this file does not exists.");
                        }
                        
                        // Extract the (assumed) ID from the URI. Typically, it
                        // would be a Data Centre entry ID, e.g.: 
                        // "jane.doe" for a person, or
                        // "e9b178f8-3414-447b-8806-d4ac378cdd3a" for a publication
                        String externalId = uri.substring(vfsFolderUri.length()).split("/")[0];
                        
                        if (externalId != null && !externalId.trim().isEmpty()) {
                            
                            // Store details as request attributes: 
                            // (These can be read by the rendering template and 
                            // other system files.)
                            
                            // The (assumed) ID
                            request.setAttribute(
                                    ATTR_NAME_ID_EXTERNAL,
                                    externalId
                            );
                            // The URI that was actually requested
                            request.setAttribute(
                                    ATTR_NAME_REQUESTED_URI,
                                    uri
                            );
                            // The URI to the closest "real" VFS folder
                            request.setAttribute(
                                    ATTR_NAME_REQUESTED_FOLDER_URI,
                                    vfsFolderUri
                            );
                            // The canonical URI
                            request.setAttribute(
                                    ATTR_NAME_CANONICAL_URI,
                                    vfsFolderUri.concat(externalId).concat("/")
                            );
                            
                            // Set the locale, determined from the parent folder
                            cmso.getRequestContext().setLocale(
                                    OpenCms.getLocaleManager().getDefaultLocale(cmso, vfsFolderUri)
                            );
                            
                            // Set the request URI to the VFS folder. That way,  
                            // cms.getRequestContext().getUri() will still 
                            // return the path an *actual* VFS resource, and we
                            // prevent CmsResourceNotFound exceptions.
                            // It is also the best we can do with respect to 
                            // site navigation etc.
                            cmso.getRequestContext().setUri(vfsFolderUri);
                            
                            // Finally, return the rendering template, as read 
                            // from the PROPERTY_TEMPLATE_DEFAULT property, e.g.
                            // /system/modules/no.npolar.common.person/elements/person.jsp
                            return cmso.readResource(defaultTemplateUri);
                        }
                    }
                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Handling URI to virtual page (non-VFS resource) '" + uri + "' failed.", e);
                    }
                }
            }
        }
        return resource;
    }
    
    /**
     * Gets the path to the VFS resource that most resembles the given path 
     * (possibly the given path itself).
     * <p>
     * If a VFS resource exists at the given path, it is returned directly.
     * <p>
     * If there is no VFS resource at the given path, we move up the path's 
     * tree, and return the path to the first VFS resource that exists. Or: the 
     * path to the closest parent folder of the given path.
     * 
     * @param cmso
     * @param path
     * @return 
     */
    private String getClosestRealVfsPath(CmsObject cmso, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        String tmp = path;
        while (!cmso.existsResource(tmp)) {
            tmp = CmsResource.getParentFolder(tmp);
            if (tmp.equals("/")) {
                break;
            }
        }
        
        return tmp;
    }
}
