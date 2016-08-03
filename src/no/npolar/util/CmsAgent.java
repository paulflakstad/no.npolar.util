package no.npolar.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import no.npolar.util.exception.*;
import org.opencms.jsp.CmsJspXmlContentBean;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.json.JSONArray;
import org.opencms.json.JSONException;
import org.opencms.json.JSONObject;
import org.opencms.jsp.I_CmsXmlContentContainer;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsUriSplitter;

/**
 * This class extends the typical class used by the Norwegian Polar Institute
 * to interact with OpenCms installations, providing some useful utility functions
 * that are frequently needed, especially when developing page templates for 
 * structured content.
 * 
 * @author Paul-Inge Flakstad
 */
public class CmsAgent extends CmsJspXmlContentBean {
    public static final String DEFAULT_TEMPLATE_RESOURCE_KEYWORD = "resourceUri";
    
    public static final String DEFAULT_DATETIME_FORMAT = "EEE MMM dd yyyy HH:mm:ss zzz";
    public static final String PROPERTY_JAVASCRIPT = "javascript";
    public static final String PROPERTY_CSS = "css";
    public static final String PROPERTY_BLACKLIST = "blacklist";
    public static final String PROPERTY_HEAD_SNIPPET = "head.snippet";
    public static final String PROPERTY_TEMPLATE_INCLUDE_ELEMENTS = "template-include-elements";
    public static final String PROPERTY_TEMPLATE_SEARCH_FOLDER = "template-search-folder";
    public static final String PROPERTY_VALUE_DELIMITER = ",";
    public static final String PROPERTY_URI_REFERENCED = "uri.referenced";
    
    public static final String EMAIL_REGEX_PATTERN = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";
    public static final String MAILTO_REGEX_PATTERN = "<a\\s[^>]+?mailto:" + EMAIL_REGEX_PATTERN + ".+?>.*?</a>";
    public static final String EMAIL_NOSCRIPT_MSG = "[javascript required to view e-mail address]";
    
    /** Parameter name for fingerprint parameters. */
    public static final String PARAM_NAME_FINGERPRINT = "fingerprint";
    
    /** The logger. */
    private static final Log LOG = LogFactory.getLog(CmsAgent.class);
    
    /**
     * Empty constructor, required for every JavaBean
     */
    public CmsAgent() {
        super();
    }
    
    /**
     * Standard JavaBean constructor.
     * @param context  The servlet page context
     * @param request  The request reference
     * @param response  The response reference
     */
    public CmsAgent(PageContext context, HttpServletRequest request, HttpServletResponse response) {
        super(context, request, response);
    }
    
    /**
     * Overrides the default link method by adding a fingerprint parameter to
     * stylesheets, javascript, images and PDFs (jpg|jpeg|png|gif|svg|css|js|pdf). 
     * <p>
     * The fingerprint parameter added is: 
     * "fingerprint=[date last modified numeric value]"
     * <p>
     * If a fingerprint parameter is already present in the given target, no 
     * additional fingerprint is added.
     * 
     * @see CmsJspXmlContentBean#link(java.lang.String) 
     */
    @Override
    public String link(String target) {
        try {
            if (target != null && !target.isEmpty()) {
                // Get the query string (incl. the "?" character), if it exists
                String queryString = target.contains("?") ? target.substring(target.indexOf("?")) : "";
                // Read the resource (remove query string if needed)
                CmsResource r = this.getCmsObject().readResource(queryString.isEmpty() ? target : target.substring(0, target.indexOf("?")));
                if (!r.isFolder()) {
                    // Get the file name (remove the query string if needed)
                    String fileName = CmsResource.getName(target);
                    if (!queryString.isEmpty())
                        fileName = fileName.substring(0, fileName.indexOf("?"));
                    // Get the parent folder
                    String folderUri = CmsResource.getParentFolder(target);

                    // Add a fingerprint, if necessary
                    if (!(queryString.contains("&"+PARAM_NAME_FINGERPRINT+"=") 
                            || queryString.contains("&amp;"+PARAM_NAME_FINGERPRINT+"=") 
                            || queryString.contains("?"+PARAM_NAME_FINGERPRINT+"="))) {
                        // Does the file extension indicate that a fingerprint should be added?
                        if (fileName.matches("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|svg|css|js|pdf))$)")) {
                            target = folderUri + fileName
                                        // keep any existing query string intact
                                        + (queryString.isEmpty() ? "?" : queryString.concat("&amp;")) 
                                        // add the fingerprint parameter
                                        + PARAM_NAME_FINGERPRINT + "=" + r.getDateLastModified();
                        }
                    }
                }
            }
        } catch (Exception e) {
            
        }
        return super.link(target);
    }
    
    /**
     * Determine whether an XML element exists.
     * <p>
     * An empty element is interpreted as "non-existing".
     * <p>
     * NOTE: This method is potentially unsafe because it relies solely on the string 
     * pattern; a string that starts with the string pattern "??? " and ends with the 
     * string pattern " ???" will be interpreted as a non-existent element. In OpenCMS, 
     * calling contentshow on a non-existent element will not produce an exception, but 
     * instead a string of the form "??? Non-existingElementName[0] ???".
     *
     * @param testStr  The element string as produced by OpenCms' method contentshow().
     * @return  True if the element exists, false if the element does not exist or has length()=0.
     */
    public static boolean elementExists(String testStr) {
        if (testStr == null) // Prevent nullpointer exception
            return false;
        if (testStr.length() == 0) // The element exists, but it is empty -> interpret this as non-existing
            return false;
        if (testStr.length() < 8) // A non-existing element is at least 8 long, because of the "??? " and " ???" at the start and end
            return true;
        String first = testStr.substring(0, 4); // Get the substring that is the first 4 characters
        String last = testStr.substring(testStr.length() - 4, testStr.length()); // Get the substring that is the 4 last characters
        // Check if a "???"-syntax is present at the start and end:
        boolean hasFirst = !(first.equalsIgnoreCase("??? "));
        boolean hasLast = !(last.equalsIgnoreCase(" ???"));       
        // Determine whether the element exists or not, return the result
        return (hasFirst && hasLast);
    }
    
    /**
     * Strips a string from paragraph tags, replaces paragraph tags inside
     * the string with: break tag + blank space + break tag.
     *
     * @param str  The string to remove paragraph tags from.
     * @return  The stripped string.
     */
    public static String stripParagraph(String str) {
        if (str == null)
                return str;
        str = str.trim();
        if (str.length() < 7) // The length of the String "<p></p>" is 7
                return str;
        // if the string ends with a </p>, strip this away clean so
        // there won't be line breaks at the end
        if (str.substring(str.length()-4, str.length()).equalsIgnoreCase("</p>")) {
                str = str.substring(0, str.length()-4);
        }
        // strip all <p> and <p class=... rel=... style=...> type tags away
        String regex = "<p\\s.+?>|<p>";
        str = str.replaceAll(regex, "");
        // and replace remaining </p> tags with extra line breaks
        str = str.replaceAll("</p>", "<br />&nbsp;<br />");
        return str;
    }
    
    /**
     * Converts a date from numeric to human readable format.
     * <p>
     * The dateFormat argument is a standard Java String that is used to define 
     * the date format.
     * <p>
     * For more information on time format syntax, see the documentation on 
     * java.text.SimpleDateFormat which should provide the necessary information
     * on how to construct time format strings.
     * 
     * @param timestamp  The timestamp (a String representation of a long).
     * @param dateFormat  The output time format. Can be null, in which case {@link #DEFAULT_DATETIME_FORMAT} is used.
     * @return  The date, formatted according to the given date format.
     * @throws javax.servlet.ServletException  If the formatting fails (probably due to syntax error in dateFormat).
     * @see java.text.SimpleDateFormat
     */
    public static String formatDate(String timestamp, String dateFormat) throws ServletException {
        Date date = new Date(); // Create a date (representing the moment in time it is created). This object is changed below.
        float millis = Float.parseFloat(timestamp); // Get the float value of the timestamp
        date.setTime((long)millis); // Change the date object so that it represents the time kept in "timestamp"
        if (dateFormat == null)
            dateFormat = DEFAULT_DATETIME_FORMAT;
        // Create the desired output format
        SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
        String dateString = null;
        try {
            dateString = outputFormat.format(date);
        }
        catch (Exception e) {
            //e.printStackTrace();
            throw new ServletException("An error was encountered while trying to process the date-time " + date.toString() + 
                    ". Please check the format of the string and correct the text, or use the calendar function to insert a date directly. " +
                    "If the problem persists, please contact your system administrator.");
        }
        return dateString;
    }
    
    /**
     * Converts a date from numeric to human readable format.
     * <p>
     * The dateFormat argument is a standard Java String that is used to define 
     * the date format.
     * <p>
     * For more information on time format syntax, see the documentation on 
     * java.text.SimpleDateFormat which should provide the necessary information
     * on how to construct time format strings.
     * 
     * @param timestamp  The timestamp (a String representaion of a long).
     * @param dateFormat  The output time format. Can be null, in which case {@link #DEFAULT_DATETIME_FORMAT} is used.
     * @param locale The Locale (language) to use.
     * @return  The date, formatted according to the specified date format and locale.
     * @throws javax.servlet.ServletException  If the formatting fails (probably due to syntax error in dateFormat).
     * @see java.text.SimpleDateFormat
     * @see java.util.Locale
     */
    public static String formatDate(String timestamp, String dateFormat, Locale locale) throws ServletException {
        Date date = new Date(); // Create a date (representing the moment in time it is created). This object is changed below.
        float millis = Float.parseFloat(timestamp); // Get the float value of the timestamp
        date.setTime((long)millis); // Change the date object so that it represents the time kept in "timestamp"
        if (dateFormat == null)
            dateFormat = DEFAULT_DATETIME_FORMAT;
        // Create the desired output format
        SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat, locale);
        String dateString = null;
        try {
            dateString = outputFormat.format(date);
        }
        catch (Exception e) {
            //e.printStackTrace();
            throw new ServletException("An error was encountered while trying to process the date-time " + date.toString() + 
                    ". Please check the format of the string and correct the text, or use the calendar function to insert a date directly. " +
                    "If the problem persists, please contact your system administrator.");
        }
        return dateString;
    }
    
    /**
     * Gets the file size of the given file formatted for screen output.
     * 
     * @param fileUri The VFS path to the file.
     * @return The file size, nicely formatted; e.g. "2.4 MB".
     * @throws CmsException If the file cannot be read.
     */
    public String formatFileSize(String fileUri) throws CmsException {
        return formatFileSize(this.getCmsObject().readResource(fileUri).getLength());
    }

    /**
     * Converts the given number of bytes to a value formatted for screen output.
     * 
     * @param bytes The number of bytes.
     * @return The converted value, nicely formatted; e.g. "2.4 MB".
     */
    public static String formatFileSize(int bytes) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        final int KB = 1024;
        final int MB = KB*KB;
        final int GB = MB*KB;
        if (bytes < KB) {
            return bytes + "&nbsp;bytes";
        } else if (bytes >= KB && bytes < MB) {
            return df.format((double)bytes / KB) + "&nbsp;kB";
        } else if (bytes >= MB && bytes < GB) {
            return df.format((double)bytes / MB) + "&nbsp;MB";
        } else {
            return df.format((double)bytes / GB) + "&nbsp;GB";
        }
    }
    
    /**
     * Capitalizes the first letter of the given string.
     * 
     * @param s The string to capitalize.
     * @return The given string, with the first letter capitalized.
     */
    public static String capitalizeFirstLetter(String s) {
        if (s == null)
            return null;
        if (s.length() < 1)
            return s;
        String firstChar = s.substring(0, 1);
        s = s.replaceFirst(firstChar, firstChar.toUpperCase());
        return s;
    }
    
    /**
     * Gets the resource name by examining a String that is the resource path.
     * <ul>
     * <li>If "/foo/bar/my.file" is supplied, the return String will be "my.file". </li>
     * <li>If "/foo/bar/" is supplied, the return String will be "bar". </li>
     * </ul>
     * <p>
     * The method is strictly a String manipulator - no OpenCms methods are 
     * invoked. (= The method can be applied to any standard "URI-type" String.)
     * 
     * @param resourcePath  The resource path.
     * @return  The resource name.
     */
    public static String getResourceName(String resourcePath) {
        String name = resourcePath;
        if (name == null)
            return null;
        String[] nameElements = name.split("/");
        if (nameElements.length == 1 || nameElements.length == 0)
            return name;
        else
            name = nameElements[nameElements.length - 1];
        return name;
    }
    
    /**
     * Resolves the path to the default image for a Vimeo video, based on the
     * video's URL.
     * 
     * @param videoUrl  The Vimeo video's URL. (Must start with "http://".)
     * @return  The video's default image URL.
     * @throws IOException
     * @throws JSONException 
     */
    public String getVimeoThumb(String videoUrl) throws IOException, JSONException {
        String videoId = null;
        try {
            videoId = videoUrl.replace("http://", "");
            videoId = videoId.substring(videoId.indexOf("/") + 1);
            if (videoId.contains("?")) {
                videoId = videoId.substring(0, videoId.indexOf("?"));
            }
        } catch (Exception e) {
            throw new NullPointerException("Error reading video ID for '" + videoUrl + "': " + e.getMessage());
        }
        // 
        // Read the video metadata from Vimeo
        //
        StringBuffer contentBuffer = new StringBuffer(1024);
        BufferedReader in = null;
        String inputLine = null;
        String jsonMetadata = null;

        try {
            in = new BufferedReader(new InputStreamReader(
                                        new URL("http://vimeo.com/api/v2/video/" + videoId + ".json").openConnection().getInputStream()
                                        )
                                    );
            while ((inputLine = in.readLine()) != null) {
                contentBuffer.append(inputLine);
            }
            jsonMetadata = contentBuffer.toString();
            in.close();
        } catch (Exception e) {
            throw new IOException("Error reading video metadata: " + e.getMessage());
        }
        String thumbnailUrl = "";
        // Get thumbnail image
        try {
            JSONArray ja = new JSONArray(jsonMetadata);
            JSONObject videoMeta = ja.getJSONObject(0);// new JSONObject(ja.toString());
            thumbnailUrl = (String)videoMeta.get("thumbnail_medium");
        } catch (Exception e) {
            throw new JSONException("Error resolving thumbnail image: " + e.getMessage());
        }
        return thumbnailUrl;
    }

    /**
     * Resolves the path to the default image for a YouTube video, based on the
     * video's URL.
     * 
     * @param videoUrl  The YouTube video's URL. (Must start with "http://".)
     * @return  The video's default image URL.
     */
    public String getYouTubeThumb(String videoUrl) {
        String videoId = null;
        try {
            String videoUrlQueryPart = new CmsUriSplitter(videoUrl).getQuery();
            Map ytUrlParts = CmsRequestUtil.createParameterMap(videoUrlQueryPart);
            videoId = ((String[])(ytUrlParts.get("v")))[0];
        } catch (Exception e) {
            throw new NullPointerException("Error resolving video ID for '" + videoUrl + "': " + e.getMessage());
        }
        return "http://img.youtube.com/vi/%(id)/0.jpg".replace("%(id)", videoId);
    }
    
    /**
     * Resolves the image for a given resource, which has its image set to "auto".
     * <p>
     * Resource types that "support" automatic images are:
     * <ul>
     * <li>videoresource (only YouTube / Vimeo, resolves image from these services)</li>
     * <li>resourceinfo (images must be placed in an /img/ subfolder inside the same folder as the target file)</li>
     * <ul>
     * <p>
     * The method relies on the property uri.referenced to contain the URI to the target resource.
     * 
     * @param resource  The resource to resolve the image for.
     * @return  The URI to the image, or null if no image could be resolved.
     * @throws CmsException  If anything goes wrong.
     */
    public String getAutoImage(CmsResource resource) throws CmsException {
        String imageUri = null;
        if (OpenCms.getResourceManager().getResourceType("videoresource").getTypeId() == resource.getTypeId()) { // Then this item is a video
            String videoUrl = this.getCmsObject().readPropertyObject(resource, "uri.referenced", false).getValue(""); // A referenced URI (e.g. the YouTube/Vimeo URL OR a path to a local video)
            // YouTube video
            if (videoUrl.indexOf("youtube.com/") > -1) {
                try {
                    imageUri = getYouTubeThumb(videoUrl);
                    //out.println("<!-- YouTube video, resolved image is '" + imageUri + "' -->");
                } catch (Exception e) {
                    //out.println("<!-- Exception: " + e.getMessage() + " -->");
                }
            }
            // Vimeo video
            else if (videoUrl.indexOf("vimeo.com/") > -1) {
                try {
                    imageUri = getVimeoThumb(videoUrl);
                    //out.println("<!-- Vimeo video, resolved image is '" + imageUri + "' -->");
                } catch (Exception e) {
                    //out.println("<!-- Exception: " + e.getMessage() + " -->");
                }
            }
            else {
                //out.println("<!-- Unknown video type for 'auto' image -->");
            }
        }
        else if (OpenCms.getResourceManager().getResourceType("resourceinfo").getTypeId() == resource.getTypeId()) { // Then this item is a resource info page
            String resourceUrl = this.getCmsObject().readPropertyObject(resource, PROPERTY_URI_REFERENCED, false).getValue(""); // A referenced URI (e.g. a path to a local PDF)
            if (resourceUrl.startsWith("/")) { // Local image
                resourceUrl = this.getCmsObject().getRequestContext().removeSiteRoot(resourceUrl);
            }

            if (this.getCmsObject().existsResource(resourceUrl)) {
                // External resource: Resolve image from "img" subfolder of the resource's parent folder (name must be the resource name + .jpg).
                imageUri = CmsResource.getParentFolder(resourceUrl).concat("img/").concat(CmsResource.getName(resourceUrl));
                imageUri = imageUri.substring(0, imageUri.lastIndexOf(".")).concat(".jpg");
                //out.println("<!-- imageUri = '" + imageUri + "' -->");

                if (!this.getCmsObject().existsResource(imageUri)) {
                    //out.println("<!-- Image '" + imageUri + "' did not exist, looking for .png ...");
                    imageUri = imageUri.substring(0, imageUri.lastIndexOf(".")).concat(".png");

                    if (!this.getCmsObject().existsResource(imageUri)) {
                        //out.println("<!-- Image '" + imageUri + "' did not exist, looking for .gif ...");
                        imageUri = imageUri.substring(0, imageUri.lastIndexOf(".")).concat(".gif");

                        if (!this.getCmsObject().existsResource(imageUri)) {
                            //out.println("<!-- Image '" + imageUri + "' did not exist, unable to resolve image ...");
                            imageUri = null;
                        }
                    }
                }
            } else {
                //out.println("<!-- Resource '" + resourceUrl + "' did not exist locally, unable to resolve image ...");
                // External resource, unable to resolve image automatically
                imageUri = null;
            }
        }
        return imageUri;
    }
    
    /**
     * Looks for width="xxx" and height="yyy" in a code string and alters the values based on the
     * new width, keeping the aspect ratio intact. If no width attribute is found in the given 
     * code, a with attribute (width="[newWidth]") is added to the first element encountered in the 
     * given code.
     * @param code  The code for which the width/height attributes should be altered.
     * @param newWidth  The new width.
     * @return  The altered code, or the same code as given if no alterations were applied.
     */
    public static String alterDimensions(String code, int newWidth) {
        String alteredCode = code;
        String regex = "width=\\\"([0-9]+)\\\"";
        String replacement = "width=\"" + newWidth + "\"";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(alteredCode); // Get a Matcher object
        String width = null;
        double widthValue = -1;
        String height = null;
        double heightValue = -1;
        Set widthMatches = new HashSet();
        Set heightMatches = new HashSet();

        //System.out.println("searching...\n");
        while(m.find()) {
            widthMatches.add(alteredCode.substring(m.start(), m.end()));
            //System.out.println("match: [" + match + "]");
        }

        if (!widthMatches.isEmpty()) {
            if (widthMatches.size() > 1) {
                throw new IllegalArgumentException("Unable to alter code: More than one width was specified.");
            }
            Iterator i = widthMatches.iterator();
            while (i.hasNext()) {
                width = (String)i.next();
                // Get the value of the heigth attribute as a double
                widthValue = Integer.valueOf(width.substring(width.indexOf("\"") + 1, width.lastIndexOf("\"") - 1)).doubleValue();
                alteredCode = alteredCode.replaceAll(width, replacement);
            }
        }
        else {
            alteredCode = code.replaceFirst(">", " width=\"" + newWidth + "\">");
            return alteredCode;
            //throw new IllegalArgumentException("Unable to alter code: No width was specified.");
        }

        // Done altering the width attributes, continue with heights
        regex = "height=\\\"([0-9]+)\\\"";
        p = Pattern.compile(regex);
        m = p.matcher(alteredCode);
        while (m.find()) {
            heightMatches.add(alteredCode.substring(m.start(), m.end()));
        }

        if (!heightMatches.isEmpty()) {
            if (heightMatches.size() > 1) {
                throw new IllegalArgumentException("Unable to alter code: More than one height was specified.");
            }
            Iterator i = heightMatches.iterator();
            while (i.hasNext()) {
                height = (String)i.next(); // height="some number"
                // Get the value of the heigth attribute as a double
                heightValue = Integer.valueOf(height.substring(height.indexOf("\"") + 1, height.lastIndexOf("\"") - 1)).doubleValue();
                // Find the height to width ratio
                double ratio = heightValue / widthValue;
                // Calculate the new height
                double newHeight = (newWidth * ratio);
                replacement = "height=\"" + new Double(newHeight).intValue() + "\"";
                alteredCode = alteredCode.replaceAll(height, replacement);
            }
        }

        return alteredCode;
    }
    
    /**
     * Returns the value of the "image.size" property, as an integer array. 
     * The size of the array can be:<br />
     *      1 - the width, if the property value is a single number, e.g.: "300"<br />
     *      2 - the property value is of standard form, e.g.: "w:400,h:300"<br />
     * @param resource  The resource to examine "image.size" on
     * @return  An integer array holding the width, or the width and height
     * @throws org.opencms.main.CmsException  If the resource cannot be read
     * @throws no.npolar.util.exception.MalformedPropertyValueException  If there is something wrong with the format of the property value
     */
    public int[] getImageSize(CmsResource resource) throws CmsException, MalformedPropertyValueException {
        // Get the property "image.size" as a String
        String sizeValue = this.getCmsObject().readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, false).getValue();
        // Prevent NullPointerException
        if (sizeValue == null)
            return null;
        // Initialize the main variables
        int width = 0, height = 0;
        
        // If a comma separator exists, assume that the format is "w:123,h:123"
        if (sizeValue.indexOf(",") != -1) {
            // Split the String into two separate substrings using "," as the separator
            String[] aspectRatio = sizeValue.split(",");
            // This length of aspectRatio should always be 2. (If not, the "image.size" property of the original image is missing or malformatted.)
            if (aspectRatio.length == 2) {
                // We need to make sure the property is correctly formatted. Correct-format-example: aspectRatio[0]=w:400 and aspectRatio[1]=h:300
                if (aspectRatio[0].lastIndexOf("w:") != -1 && aspectRatio[0].length() >= 3 && 
                        aspectRatio[1].lastIndexOf("h:") != -1 && aspectRatio[1].length() >= 3) { 
                    try {
                        width = Integer.parseInt( aspectRatio[0].substring(aspectRatio[0].lastIndexOf(":") + 1, aspectRatio[0].length()));
                        height = Integer.parseInt( aspectRatio[1].substring(aspectRatio[1].lastIndexOf(":")+ 1, aspectRatio[1].length()));
                        int[] twoValues = { width, height };
                        return twoValues;
                    } catch (Exception e) {
                        throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                            "': 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                            " The exception was: " +  e.getMessage()); 
                    }
                } 
                else { 
                    throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                        "': 'image.size' was missing either 'w:' or 'h:'. Correct format example: 'w:123,h:123'"); 
                }
            }
            else { 
                throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                    "': 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'"); 
            }
        }
        // No comma separator exists, assume that the value is a single number that should be the width
        else {
            try {
                // The value should be a single number describing the width
                width = Integer.parseInt(sizeValue);
                int oneValue[] = {width};
                return oneValue;
            } catch (Exception e) {
                throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                    "': 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                    " The exception was: " +  e.getMessage()); 
            }
        }
    }
    
    /**
     * Returns the value of the "image.size" property, as an integer array. 
     * The size of the array can be:<br />
     *      1 - the width, if the property value is a single number, e.g.: "300"<br />
     *      2 - the property value is of standard form, e.g.: "w:400,h:300"<br />
     * @param cmso  The CmsObject reference to use when reading the resource
     * @param resource  The resource to examine "image.size" on
     * @return  An integer array holding the width, or the width and height
     * @throws org.opencms.main.CmsException  If the resource cannot be read
     * @throws no.npolar.util.exception.MalformedPropertyValueException  If there is something wrong with the format of the property value
     */
    public static int[] getImageSize(CmsObject cmso, CmsResource resource) throws CmsException, MalformedPropertyValueException {
        // Get the property "image.size" as a String
        String sizeValue = cmso.readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_IMAGE_SIZE, false).getValue();
        // Prevent NullPointerException
        if (sizeValue == null)
            return null;
        // Initialize the main variables
        int width = 0, height = 0;
        
        // If a comma separator exists, assume that the format is "w:123,h:123"
        if (sizeValue.indexOf(",") != -1) {
            // Split the String into two separate substrings using "," as the separator
            String[] aspectRatio = sizeValue.split(",");
            // This length of aspectRatio should always be 2. (If not, the "image.size" property of the original image is missing or malformatted.)
            if (aspectRatio.length == 2) {
                // We need to make sure the property is correctly formatted. Correct-format-example: aspectRatio[0]=w:400 and aspectRatio[1]=h:300
                if (aspectRatio[0].lastIndexOf("w:") != -1 && aspectRatio[0].length() >= 3 && 
                        aspectRatio[1].lastIndexOf("h:") != -1 && aspectRatio[1].length() >= 3) { 
                    try {
                        width = Integer.parseInt( aspectRatio[0].substring(aspectRatio[0].lastIndexOf(":") + 1, aspectRatio[0].length()));
                        height = Integer.parseInt( aspectRatio[1].substring(aspectRatio[1].lastIndexOf(":")+ 1, aspectRatio[1].length()));
                        int[] twoValues = { width, height };
                        return twoValues;
                    } catch (Exception e) {
                        throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                            "': 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                            " The exception was: " +  e.getMessage()); 
                    }
                } 
                else { 
                    throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                        "': 'image.size' was missing either 'w:' or 'h:'. Correct format example: 'w:123,h:123'"); 
                }
            }
            else { 
                throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                    "': 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'"); 
            }
        }
        // No comma separator exists, assume that the value is a single number that should be the width
        else {
            try {
                // The value should be a single number describing the width
                width = Integer.parseInt(sizeValue);
                int oneValue[] = {width};
                return oneValue;
            } catch (Exception e) {
                throw new MalformedPropertyValueException("Property value was not of correct format for '" + resource.getName() + 
                    "': 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                    " The exception was: " +  e.getMessage()); 
            }
        }
    }
    
    /**
     * Returns the value of the "image.size" property, as an integer array. 
     * <p>
     * <ul>The size of the array can be:<br />
     *     <li>1 - the width, if the property value is a single number, e.g.: "300"</li>
     *     <li>2 - the property value is of standard form, e.g.: "w:400,h:300"</li>
     * </ul>
     * 
     * @param propertyImageSizeValue  The string value of the property "image.size".
     * @return  An integer array holding the width, or the width and height.
     * @throws no.npolar.util.exception.MalformedPropertyValueException  If there is something wrong with the format of the property value.
     */
    public static int[] getImageSize(String propertyImageSizeValue) throws MalformedPropertyValueException {
        // Get the property "image.size" as a String
        String sizeValue = propertyImageSizeValue;
        // Prevent NullPointerException
        if (sizeValue == null)
            return null;
        // Initialize the main variables
        int width = 0, height = 0;
        
        // If a comma separator exists, assume that the format is "w:123,h:123"
        if (sizeValue.indexOf(",") != -1) {
            // Split the String into two separate substrings using "," as the separator
            String[] aspectRatio = sizeValue.split(",");
            // This length of aspectRatio should always be 2. (If not, the "image.size" property of the original image is missing or malformatted.)
            if (aspectRatio.length == 2) {
                // We need to make sure the property is correctly formatted. Correct-format-example: aspectRatio[0]=w:400 and aspectRatio[1]=h:300
                if (aspectRatio[0].lastIndexOf("w:") != -1 && aspectRatio[0].length() >= 3 && 
                        aspectRatio[1].lastIndexOf("h:") != -1 && aspectRatio[1].length() >= 3) { 
                    try {
                        width = Integer.parseInt( aspectRatio[0].substring(aspectRatio[0].lastIndexOf(":") + 1, aspectRatio[0].length()));
                        height = Integer.parseInt( aspectRatio[1].substring(aspectRatio[1].lastIndexOf(":")+ 1, aspectRatio[1].length()));
                        int[] twoValues = { width, height };
                        return twoValues;
                    } catch (Exception e) {
                        throw new MalformedPropertyValueException("Property value was not of correct format" +
                            ": 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                            " The exception was: " +  e.getMessage()); 
                    }
                } 
                else { 
                    throw new MalformedPropertyValueException("Property value was not of correct format" + 
                        ": 'image.size' was missing either 'w:' or 'h:'. Correct format example: 'w:123,h:123'"); 
                }
            }
            else { 
                throw new MalformedPropertyValueException("Property value was not of correct format" + 
                    ": 'image.size' did not contain 2 elements, or did not use the correct separator sign. Correct format example: 'w:123,h:123'"); 
            }
        }
        // No comma separator exists, assume that the value is a single number that should be the width
        else {
            try {
                // The value should be a single number describing the width
                width = Integer.parseInt(sizeValue);
                int oneValue[] = {width};
                return oneValue;
            } catch (Exception e) {
                throw new MalformedPropertyValueException("Property value was not of correct format" +
                    ": 'image.size' contained a non-numeric value where numeric value was expected. Correct format example: 'w:123,h:123'." + 
                    " The exception was: " +  e.getMessage()); 
            }
        }
    }
    
    /**
     * Calculates a new height based on the new width, preserving the aspect ratio.
     * 
     * @param newWidth The new width.
     * @param width The original width.
     * @param height The original height.
     * @return The new height.
     */
    public static int calculateNewImageHeight(int newWidth, int width, int height ) {
        double ratio = (double)width / newWidth; // IMPORTANT! Cast one to double, or ratio will get an integer value..!!!
        double newHeight = (double)height / ratio;
        return (int)newHeight;
    }
    
    public int calculateNewImageHeight(int newWidth, String imageUri) throws CmsException, MalformedPropertyValueException {
        CmsResource imageResource = this.getCmsObject().readResource(imageUri);
        int[] imageDimensions = this.getImageSize(imageResource);
        
        double ratio = (double)imageDimensions[0] / newWidth; // IMPORTANT! Cast one to double, or ratio will get an integer value..!!!
        double newHeight = (double)imageDimensions[1] / ratio;
        return (int)newHeight;
    }
    
    
    /**
     * Gets the HTML for a complex image container, consisting of an outer
     * wrapper, the image, and containers for image text and credit.
     * <p>
     * Compliant with the Highslide Javascript for image magnification.
     * 
     * @param useSpans set to true to force use of span (no block-elements). Set to false to force use of div.
     * @param imageTag the complete IMG tag (standard html)
     * @param imageWidth the image's width
     * @param imagePadding the padding of the image (used to calculate the outer wrapper div's width). Set to 0 if no padding is used.
     * @param imageText the image text. Can be null.
     * @param imageCreditLabel the label for the credit text, e.g. "Photo: ". Can be null.
     * @param imageCredit the image credit text. Can be null.
     * @return the HTML code for the image container.
     */
    public String getImageContainer(boolean useSpans, 
                                    String imageTag, 
                                    int imageWidth, 
                                    int imagePadding, 
                                    String imageText, 
                                    String imageCreditLabel,
                                    String imageCredit) {
        String imageContainer = useSpans ? "span" : "div";
        String textContainer = useSpans ? "span" : "p";
        String imageFrameHTML = 
                "<" + imageContainer + " class=\"illustration" + (useSpans ? "" : " poster") + "\" " +
                "style=\"width:" + (imageWidth + (imagePadding*2)) + "px;\">" + 
                    imageTag +
                    "<" + textContainer + " class=\"imagetext highslide-caption\">" +
                        (elementExists(imageText) ? stripParagraph(imageText) : "") + 
                        (elementExists(imageCredit) ? ("<span class=\"imagecredit\"> (" + (imageCreditLabel == null ? "" : imageCreditLabel) + imageCredit + ")</span>") : "") +
                    "</" + textContainer + ">" +
                "</" + imageContainer + ">";
        return imageFrameHTML;
    }
    
    /**
     * Gets the HTML &lt;img ... /&gt; code for an image.
     * 
     * @param src The image's URI.
     * @param clazz The image's class.
     * @param alt The image's alternative text.
     * @param title The image's title.
     * @param width The image's width.
     * @param height The image's height.
     * @param xhtmlSyntax True if XHTML syntax should be used, false if not..
     * @return The HTML code for the image.
     */
    public static String getImageTag(String src, String clazz, String alt, String title, String width, String height, boolean xhtmlSyntax) {
        String tag = "<img src=\"" + src + "\"";
        tag += " alt=\"" + (alt != null ? alt : "") + "\"";
        if (clazz != null)
            tag += " class=\"" + clazz + "\"";
        if (title != null)
            tag += " title=\"" + title + "\"";
        if (width != null)
            tag += " width=\"" + width + "\"";
        if (height != null)
            tag += " height=\"" + height + "\"";
        if (xhtmlSyntax)
            tag += " /";
        tag += ">";
        return tag;
    }
    
    
    /**
     * Removes the username from the "fullname" String returned by the OpenCms
     * macro <code>%(currentuser.fullname)</code>.
     * <p>
     * E.g.:<br />
     * <code>
     * String fullname = "My Name (myname)"; // = %(currentuser.fullname)<br />
     * String name = CmsAgent.removeUsername(fullname); // name = "My Name"<br />
     * </code>
     * 
     * @param fullname  The full name + login name in parenthesis
     * @return  The full name without the login name
     */
    public static String removeUsername(String fullname) {
        if (fullname.indexOf("(") == -1)
            return fullname;
        String login = fullname.substring(fullname.indexOf("(") - 1, fullname.length());
        return fullname.replace(login, "");
    }
    
    /**
     * Includes a plain text file or a jsp file directly, OR: 
     * Includes a template-driven file by calling it's designated 
     * "template-elements"-jsp.
     * <p>
     * The jsp is informed of the URI to the XML content file by attaching its 
     * VFS URI as a parameter when calling the jsp.
     * 
     * @param includeUri  The URI of the file to include.
     * @param parameterNameForTemplate  The name to use for the XML content URI parameter, if the content to include is produced by a template. Provide null to use the default ({@link CmsAgent#DEFAULT_TEMPLATE_RESOURCE_KEYWORD}).
     * @throws org.opencms.main.CmsException  If an error occurs during property reading
     * @throws javax.servlet.jsp.JspException  If an error occurs during the include
     * @see org.opencms.jsp.CmsJspActionElement#include(java.lang.String) 
     * @see org.opencms.jsp.CmsJspActionElement#include(java.lang.String, java.lang.String, java.util.Map)  
     */
    public void includeAny(String includeUri, String parameterNameForTemplate) throws CmsException, JspException {
        this.includeAny(includeUri, parameterNameForTemplate, null, null);
        /*if (parameterNameForTemplate == null)
            parameterNameForTemplate = DEFAULT_TEMPLATE_RESOURCE_KEYWORD;
        
        String correctedUri = this.getRequestContext().removeSiteRoot(includeUri);
        // If the included file is a JSP, include it directly
        if (this.getCmsObject().readResource(correctedUri).getTypeId() == OpenCms.getResourceManager().getResourceType("jsp").getTypeId() ||
                this.getCmsObject().readResource(correctedUri).getTypeId() == OpenCms.getResourceManager().getResourceType("plain").getTypeId()) {
            this.include(correctedUri);
        }
        // If the included file is NOT a JSP, check the included file's "template-elements" property. Then, include that
        // JSP with a parameter "resourceUri" that is the file itself (typically a HTML file). The parameter "resourceUri"
        // will then act in place of the usual CmsJspActionElement.getRequestContext().getUri() - but this solution requires
        // that the "template-elements" JSP actually checks for the parameter. There is no auto-check on the parameter from
        // on behalf of OpenCms.
        else {// (cmso.readResource(includeFile).getTypeId() == OpenCms.getResourceManager().getResourceType("np_form").getTypeId()) {
            String tempElem = this.getCmsObject().readPropertyObject(correctedUri, CmsPropertyDefinition.PROPERTY_TEMPLATE_ELEMENTS, true).getValue();
            HashMap parameters = new HashMap(); // request.getParameterMap();
            parameters.put(parameterNameForTemplate, correctedUri);
            //parameters.put("resourceUri", includeUri);
            //cms.include(tempElem, "main", parameters);
            this.include(tempElem, null, parameters);
        }*/
    }
    
    public void includeAny(String includeUri) throws CmsException, JspException {
        this.includeAny(includeUri, null, null, null);
    }
    
    public void includeAny(String includeUri, String parameterNameForTemplate, Map<String, ?> parameters) throws CmsException, JspException {
        this.includeAny(includeUri, parameterNameForTemplate, null, parameters);
    }
    
    
    public void includeAny(String includeUri, Map<String, ?> parameters) throws CmsException, JspException {
        this.includeAny(includeUri, null, null, parameters);
    }
    
    /**
     * Includes a plain text file or a jsp file directly, OR: 
     * Includes a template-driven file by calling it's designated 
     * "template-elements"-jsp.
     * <p>The jsp is informed of the URI to the XML content file by attaching 
     * its URI as a parameter when calling the jsp.
     * 
     * @param includeUri  The URI of the file to include.
     * @param parameterNameForTemplate  The name to use for the XML content URI parameter, if the content to include is produced by a template. Provide null to use the default ({@link CmsAgent#DEFAULT_TEMPLATE_RESOURCE_KEYWORD}).
     * @param templateElementName The name of the template element, if any. Can be null.
     * @param parameters Any parameters to use in the include. Values in the map should be of type String. For convenience, String[] is also accepted, but only the String at index 0 will be used.
     * @throws org.opencms.main.CmsException  If an error occurs during property reading
     * @throws javax.servlet.jsp.JspException  If an error occurs during the include
     * @see org.opencms.jsp.CmsJspActionElement#include(java.lang.String) 
     * @see org.opencms.jsp.CmsJspActionElement#include(java.lang.String, java.lang.String, java.util.Map)  
     */
    public void includeAny(String includeUri, String parameterNameForTemplate, String templateElementName, Map<String, ?> parameters) throws CmsException, JspException {
        if (parameterNameForTemplate == null || parameterNameForTemplate.isEmpty()) {
            parameterNameForTemplate = DEFAULT_TEMPLATE_RESOURCE_KEYWORD;
        }
        
        HashMap<String, String> modParameters = new HashMap<String, String>();
        
        if (parameters != null && !parameters.isEmpty()) {
            Iterator i = parameters.keySet().iterator();
            while (i.hasNext()) {
                String parameterKey = (String)i.next();
                Object parameterValue = parameters.get(parameterKey);
                if (parameterValue instanceof String[]) {
                    modParameters.put(parameterKey, ((String[])parameterValue)[0]);
                } else if (parameterValue instanceof String) {
                    modParameters.put(parameterKey, (String)parameterValue);
                }
            }
        }
        
        String correctedUri = this.getRequestContext().removeSiteRoot(includeUri);
        
        // If the included file is a JSP or plain text file, include it directly
        if (this.getCmsObject().readResource(correctedUri).getTypeId() == OpenCms.getResourceManager().getResourceType("jsp").getTypeId() ||
                this.getCmsObject().readResource(correctedUri).getTypeId() == OpenCms.getResourceManager().getResourceType("plain").getTypeId()) {
            if (modParameters.isEmpty()) {
                this.include(correctedUri);
            } else {
                try {
                    this.include(correctedUri, templateElementName, modParameters);
                } catch (Exception e) {
                    this.include(correctedUri);
                }
            }
        }
        
        // If the included file is NOT a JSP, check the included file's "template-elements" property. Then, include that
        // JSP with a parameter "resourceUri" that is the file itself (typically a HTML file). The parameter "resourceUri"
        // will then act in place of the usual CmsJspActionElement.getRequestContext().getUri() - but this solution requires
        // that the "template-elements" JSP actually checks for the parameter. There is no auto-check on the parameter from
        // on behalf of OpenCms.
        else {// (cmso.readResource(includeFile).getTypeId() == OpenCms.getResourceManager().getResourceType("np_form").getTypeId()) {
            String tempElem = this.getCmsObject().readPropertyObject(correctedUri, CmsPropertyDefinition.PROPERTY_TEMPLATE_ELEMENTS, true).getValue();
            //HashMap parameters = new HashMap(); // request.getParameterMap();
            modParameters.put(parameterNameForTemplate, correctedUri);
            //parameters.put("resourceUri", includeUri);
            //cms.include(tempElem, "main", parameters);
            this.include(tempElem, templateElementName, modParameters);
        }
    }
    
    /**
     * Gets the "master" template, by reading the <code>template</code> property, 
     * searching parent folders if it cannot be found on the request resource 
     * directly.
     * <p>
     * Equivalent to calling <code>CsmJspActionElement.property("template", "search")</code>.
     * 
     * @return  The value of the <code>template</code> property, or null if none.
     */
    public String getTemplate() {
        return this.property(CmsPropertyDefinition.PROPERTY_TEMPLATE, "search");
    }
    
    /**
     * Gets the OpenCms element names of the master template.
     * <p>
     * These are read from the property "template-include-elements". 
     * There has to be two values, separated with a semicolon (";"). Also, the 
     * top element should be the first one, and the bottom element should be the 
     * last.
     * 
     * @return  The top and bottom element names, as Strings in an array of size 2.
     * @throws javax.servlet.ServletException  If the property is undefined, or the value is missing or malformed.
     */
    public String[] getTemplateIncludeElements() throws ServletException {
        String templateIncludeElements = this.property(PROPERTY_TEMPLATE_INCLUDE_ELEMENTS, "search");
        if (templateIncludeElements == null) {
            /*throw new ServletException("Error on property template-include-elements: Missing property value or undefined property " +
                "(have you created the property 'template-include-elements'?).");*/
            templateIncludeElements = "head;foot";
        }
        String[] elements = templateIncludeElements.split(";");
        if (elements.length != 2) {
            throw new ServletException("Error on property " + PROPERTY_TEMPLATE_INCLUDE_ELEMENTS + ": There must be 2 values, separated by ';' i.e.: 'header;footer'.");
        }
        return elements;
    }
    
    /**
     * Convenience method for including the top part of a "master" template in 
     * a single call.
     * <p>
     * Basically a shortcut for the {@link CmsJspXmlContentBean#include(java.lang.String, java.lang.String)} 
     * call, where it's invoked with values from the <code>template</code> and 
     * <code>template-include-elements</code> properties.
     * <p>
     * If the latter is undefined, this method will fallback to the OpenCms 
     * default; "head".
     * 
     * @throws javax.servlet.ServletException
     * @throws javax.servlet.jsp.JspException
     */
    public void includeTemplateTop() throws ServletException, JspException {
        String elementName = "head"; // Default in OpenCms
        try { elementName = this.getTemplateIncludeElements()[0]; } catch (Exception e) {} // Override?
        
        this.include(this.getTemplate(), elementName);
    }
    
    /**
     * Convenience method for including the bottom part of a "master" template in 
     * a single call.
     * <p>
     * Basically a shortcut for the {@link CmsJspXmlContentBean#include(java.lang.String, java.lang.String)} 
     * call, where it's invoked with values from the <code>template</code> and 
     * <code>template-include-elements</code> properties.
     * <p>
     * If the latter is undefined, this method will fallback to the OpenCms 
     * default; "foot".
     * 
     * @throws javax.servlet.ServletException
     * @throws javax.servlet.jsp.JspException
     */
    public void includeTemplateBottom() throws ServletException, JspException {
        String elementName = "foot"; // Default in OpenCms
        try { elementName = this.getTemplateIncludeElements()[1]; } catch (Exception e) {} // Override?
        
        this.include(this.getTemplate(), elementName);
    }
    
    /**
     * Gets the property "template-search-folder" from the request resource, searches
     * parent folder(s) if the property has no direct value.
     * 
     * @return  The value of the property "template-search-folder", possibly inherited from parent folder(s).
     */
    public String getTemplateSearchFolder() throws ServletException {
        String templateSearchFolder = this.property(PROPERTY_TEMPLATE_SEARCH_FOLDER, "search");
        templateSearchFolder = this.getRequestContext().removeSiteRoot(templateSearchFolder);
        
        if (templateSearchFolder == null) {
            throw new ServletException("Error on property '" + PROPERTY_TEMPLATE_SEARCH_FOLDER + "': Missing property value or undefined property (have you created the property 'template-search-folder'?).");
        }
        else if (!this.getCmsObject().existsResource(templateSearchFolder)) {
            throw new ServletException("Error on property '" + PROPERTY_TEMPLATE_SEARCH_FOLDER + "': Resource '" + templateSearchFolder + "' does not exist.");
        }
        return templateSearchFolder;
    }
    
    /**
     * Gets XML content by looping over an I_CmsXmlContentContainer and extracting 
     * the fields specified in the String array "fields".
     * 
     * @param fields  The element names ("fields") that should be extracted
     * @param container  The XML container, should be instanciated using the OpenCms method contentloop()
     * @return  A list of StringMaps, each map containing key/value-pairs of the extracted content
     * @throws javax.servlet.jsp.JspException  If I_CmsXmlContentContainer.hasMoreContent() fails
     */
    public ArrayList getCmsXmlContentMap(String[] fields, I_CmsXmlContentContainer container) throws JspException {
        ArrayList contentMaps = new ArrayList();
        StringMap contentMap = new StringMap();
        while (container.hasMoreContent()) {
            for (int i = 0; i < fields.length; i++) {
                contentMap.put(fields[i], contentshow(container, fields[i]));
            }
            contentMaps.add(new StringMap(contentMap));
            contentMap.clear();
        }
        return contentMaps;
    }
    
    /**
     * Gets an element String value from an I_CmsXmlContentContainer.
     * <p>
     * Equal to CmsJspXmlContentBean.contentshow(container, field).
     * 
     * @param container  The I_CmsXmlContentContainer to read from
     * @param field  The field (element) to read from the container
     * @return  The field (element) value
     */
    public String getElement(I_CmsXmlContentContainer container, String field) {
        String elementValue = contentshow(container, field);
        return elementValue;
    }
    
    /**
     * Gets the different locale siblings of a resource. 
     * 
     * @param resource  The path to the resource to find siblings of
     * @return  List of paths to siblings that have a different locale. (The list contains Strings.)
     * @throws org.opencms.main.CmsException  If something goes wrong during properties / file reading
     */
    public ArrayList getLocaleSiblings(String resource) throws CmsException {
        String locale = this.getCmsObject().readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_LOCALE, true).getValue();
        ArrayList others = new ArrayList();
        List siblings = this.getCmsObject().readSiblings(resource, CmsResourceFilter.ALL);
        CmsResource sibling = null;
        Iterator i = siblings.iterator();
        while (i.hasNext()) {
            sibling = (CmsResource)i.next();
            if (!this.getCmsObject().readPropertyObject(sibling, CmsPropertyDefinition.PROPERTY_LOCALE, true).getValue().equalsIgnoreCase(locale)) {
                others.add(this.getCmsObject().getSitePath(sibling));
            }
        }
        return others;
    }
    
    /**
     * Quick-and-dirty method used to get Russian labels from workplace.properties-files
     * since there are encoding issues with OpenCms' native methods.
     * <p>
     * NOTE: This method is ugly and uses hard-coded paths for the workplace files!!!
     * 
     * @param labelname  The label to get
     * @param locale  The locale (if anything but russian, OpenCms' native label() method is used)
     * @return  The label
     * @deprecated NEVER USE THIS! Use labelUnicode(String) instead.
     */
    public String label(String labelname, String locale) {
        if (!locale.equalsIgnoreCase("ru")) {
            return this.label(labelname);
        }
        // Give special treatment to russian stuff
        else {
            String filename = "workplace_"+locale.toLowerCase()+".properties";
            String[] folders = new String[3];
            folders[0] = "/system/modules/no.npolar.language/classes/no/npolar/language/"; //local machine
            folders[1] = "/system/modules/no.npolar.site.ivorygull/classes/no/npolar/site/ivorygull/";
            folders[2] = "/system/modules/no.npolar.common.newsbulletin/classes/no/npolar/common/newsbulletin/";
            
            java.util.HashMap mapping = new java.util.HashMap(); // Will hold key/value pairs of labels
            
            String workplaceFile = null;
                String fileContent = null;
                String[] lines;
                String[] keysAndValues;

                for (int j = 0; j < folders.length; j++) {
                    workplaceFile = folders[j].concat(filename);
                    
                    if (this.getCmsObject().existsResource(workplaceFile)) {
                        fileContent   = this.getContent(workplaceFile); // Get the file contents of the workplace file
                        
                        lines = fileContent.split("\\n");
                        for (int i = 0; i < lines.length; i++) {
                            if (lines[i].trim().length() > 0) {
                                if (!lines[i].trim().substring(0,1).equals("#")) {
                                    keysAndValues = lines[i].split("=");
                                    if (keysAndValues.length == 2) {
                                        mapping.put(keysAndValues[0], keysAndValues[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            String value = (String)mapping.get(labelname);
            if (value == null)
                return this.label(labelname);
            return value;
        }
    }
    
    /**
     * Help method used to retrieve correct label strings from the .properties
     * file.
     * <p>
     * The native method CmsJspActionElement#label(String) does not 
     * return the correct label strings for characters not in ISO-8859-1 (for
     * example Russian).
     * <p>
     * If ISO-8859-1 or UTF-8 is not supported, the returned label will contain
     * a message, effectively forcing the user to use the standard label(String) 
     * method instead.
     * 
     * @param key the label name
     * @return the label
     */
    public String labelUnicode(String key) {
        try {
            return new String(this.label(key).getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "[Default label: ".concat(this.label(key)).concat("]");
        }
    }
    
    /**
     * Retrieves a parameterized string from the workplace.properties file, with
     * the given parameters injected.
     * 
     * @param key The label name.
     * @param params The parameter strings to inject. Make sure to provide the appropriate number of parameter strings for the specific label to ensure a good result.
     * @return The parameterized string, with the given parameters injected.
     * @see #labelUnicode(java.lang.String) 
     */
    public String labelUnicode(String key, String ... params) {
        try {
            return resolveLabelParameters(labelUnicode(key), params);
        } catch (Exception e) {
            return label(key);
        }
    }
    
    /**
     * Resolves the parameters in the given parameterized string.
     * 
     * @param str The parameterized string.
     * @param params The parameter strings to inject.
     * @return The given parameterized string, with parameters resolved (i.e. with the given parameter strings injected).
     */
    protected String resolveLabelParameters(String str, String ... params) {
        String s = str;
        for (int i = 0; i < params.length; i++){
            s = s.replaceAll("\\{".concat(String.valueOf(i)).concat("\\}"), params[i]);
        }
        return s;
    }
    
    /**
     * Gets all inherited and individual head elements for the given resource.
     * <p>
     * The head elements are stylesheets, javascripts or text snippets to include
     * in the HTML head section.
     * 
     * @param property one of the designated properties javascript, css or head.snippet
     * @param resourceUri the URI of the resource to resolve elements for
     * @return a list of paths to resources
     * @throws org.opencms.main.CmsException if reading property values fails
     */
    public List resolveHeaderElements(String property, String resourceUri) throws CmsException {
        CmsObject cmso              = this.getCmsObject();
        ArrayList<String> elements  = new ArrayList<String>();
        String examineUri           = resourceUri;

        while (examineUri != null) {
            String propertyValue = cmso.readPropertyObject(examineUri, property, false).getValue();

            if (propertyValue != null) {
                String[] propertyValues = propertyValue.split(PROPERTY_VALUE_DELIMITER);
                for (String value : propertyValues) {
                    if (value != null) {
                        elements.add(value);
                    }
                }
            }
            examineUri = CmsResource.getParentFolder(examineUri);
        }
        return elements;
    }
    
    /**
     * Convenience method for getting CSS resources for the requested resource.
     * 
     * @return Any CSS resources for the requested resource.
     * @throws javax.servlet.jsp.JspException
     * @throws org.opencms.main.CmsException
     * @see #getHeaderElement(java.lang.String, java.lang.String)
     */
    public String getCss() throws JspException, CmsException {
        return getHeaderElement(PROPERTY_CSS, this.getRequestContext().getUri());
    }
    
    /**
     * Convenience method for getting javascripts for the requested resource.
     * 
     * @return Any javascripts for the requested resource.
     * @throws javax.servlet.jsp.JspException
     * @throws org.opencms.main.CmsException
     * @see #getHeaderElement(java.lang.String, java.lang.String)
     */
    public String getJavascript() throws JspException, CmsException {
        return getHeaderElement(PROPERTY_JAVASCRIPT, this.getRequestContext().getUri());
    }
    
    /**
     * Convenience method for getting head snippets for the requested resource.
     * 
     * @return Any head snippets for the requested resource.
     * @throws javax.servlet.jsp.JspException
     * @throws org.opencms.main.CmsException
     * @see #getHeaderElement(java.lang.String, java.lang.String)
     */
    public String getHeadSnippet() throws JspException, CmsException {
        return getHeaderElement(PROPERTY_HEAD_SNIPPET, this.getRequestContext().getUri());
    }
    
    /**
     * Convenience method for getting a list of any excluded ("blacklisted") 
     * resources for the requested resource.
     * 
     * @return Any blacklist resources for the requested resource.
     * @throws javax.servlet.jsp.JspException
     * @throws org.opencms.main.CmsException
     * @see #getBlacklist(java.lang.String)
     */
    public List getBlacklist() throws JspException, CmsException {
        return getBlacklist(this.getRequestContext().getUri());
    }
    
    /**
     * Gets a list of any excluded ("blacklisted") resources for a resource.
     * <p>
     * This is a help method for filtering included resources (in particular, 
     * javascripts - e.g. highslide.js requires to be replaced by 
     * highslide-with-gallery.js for image galleries). Blacklisted header 
     * elements will be removed from the lists of included resources.
     * 
     * @param resourceUri the resource to get the blacklisted resources for
     * @return a list of blacklisted resources
     * @throws javax.servlet.jsp.JspException
     * @throws org.opencms.main.CmsException
     * @see #resolveHeaderElements(java.lang.String, java.lang.String) 
     */
    public List getBlacklist(String resourceUri) throws JspException, CmsException {
        return resolveHeaderElements(PROPERTY_BLACKLIST, resourceUri);
    }

    /**
     * Gets the HTML &lt;head&gt; code section for CSS, javascript or an
     * arbitrary code section (a code snippet).
     * <p>
     * This method also takes the "blacklist" into consideration. The returned 
     * string can be inserted directly into the HTML &lt;head&gt; node.
     * 
     * @param property one of the designated properties javascript, css or head.snippet
     * @param resourceUri the URI of the resource to get header elements for
     * @return the header section
     * @throws JspException if inclusion of a snippet goes wrong
     * @throws CmsException if a property cannot be accessed
     * @see #resolveHeaderElements(java.lang.String, java.lang.String) 
     */
    public String getHeaderElement(String property, String resourceUri) throws JspException, CmsException {
        List headerElements = this.resolveHeaderElements(property, resourceUri);
        List blacklist = this.getBlacklist(resourceUri);
        // Remove blacklisted resources
        headerElements.removeAll(blacklist);
        
        String headerSection = "";

        if (property.equals(PROPERTY_HEAD_SNIPPET)) {
            if (!headerElements.isEmpty()) {
                Iterator<String> itr = headerElements.iterator();
                while (itr.hasNext()) {
                    this.include(itr.next());
                }
                headerSection += "<!-- " + headerElements.size() + " head snippet(s) total -->\n";
            } else {
                headerSection += "<!-- no head snippet(s) -->\n";
            }
        }
        else if (property.equals(PROPERTY_CSS) || property.equals(PROPERTY_JAVASCRIPT)) {
            if (!headerElements.isEmpty()) {
                Iterator<String> itr = headerElements.iterator();
                while (itr.hasNext()) {
                    String resourcePath = itr.next();
                    if (property.equals(PROPERTY_CSS))
                        headerSection += "<link href=\"" + this.link(resourcePath) + "\" rel=\"stylesheet\" type=\"text/css\" />\n";
                    else if (property.equals(PROPERTY_JAVASCRIPT))
                        headerSection += "<script src=\"" + this.link(resourcePath) + "\" type=\"text/javascript\"></script>\n";
                }
            }
        }

        return headerSection;
    }
    
    /**
     * Obfuscates all e-mail addresses in a text using javascript, making the e-mail
     * addresses more difficult for crawlers to detect. 
     * <p>
     * Both plaintext addresses and <code>mailto</code> links will be obfuscated. 
     * <p>
     * A noscript tag is also attached, so that users with javascript disabled 
     * can understand why they cannot see the e-mail address.
     * 
     * @param txt The text to process, which may contain one or more e-mail addresses.
     * @param createAsMailtoLink If true, addresses in plaintext will be converted to mailto links.
     * @return The obfuscated text, where e-mail related sections are replaced by javascript.
     */
    public static String obfuscateEmailAddr(String txt, boolean createAsMailtoLink) {
        if (txt == null)
            return "";
        
        String text = txt;

        Pattern p = Pattern.compile(MAILTO_REGEX_PATTERN);
        Matcher m = p.matcher(text); // Get a Matcher object
        String match = null;
        int offset = 0; // The offset is important, since we'll be altering the text string during inside while (m.find())
        String jsCode = null; // The javascript code that will replace the original text
        Map swapMap = new HashMap<String, String>();
        
        // Find each mailto:-occurence and map it to its javascript equivalent
        while(m.find()) {
            match = text.substring(m.start() + offset, m.end() + offset);
            jsCode = getJavascriptMailto(match);
            swapMap.put(match, jsCode);
            //text = text.replace(match, jsCode);
            //offset += (jsCode.length() - match.length());
        }
        
        Set keys = swapMap.keySet();
        Iterator iKeys = keys.iterator();
        while (iKeys.hasNext()) {
            String key = (String)iKeys.next();
            text = text.replace(key, (String)swapMap.get(key));
        }
        
        // Now replace all remaining plain text addresses
        p = Pattern.compile(EMAIL_REGEX_PATTERN);
        m = p.matcher(text); // Get a Matcher object
        match = null;
        offset = 0;
        jsCode = null;
        swapMap.clear();
        // Find each email address and map it to its javascript equivalent
        while(m.find()) {
            match = text.substring(m.start() + offset, m.end() + offset);
            jsCode = getJavascriptEmail(match, createAsMailtoLink, null);
            swapMap.put(match, jsCode);
            //text = text.replaceFirst(match, jsCode);
            //offset += (jsCode.length() - match.length());
        }
        
        keys = swapMap.keySet();
        iKeys = keys.iterator();
        while (iKeys.hasNext()) {
            String key = (String)iKeys.next();
            text = text.replace(key, (String)swapMap.get(key));
        }
        
        return text;
    }
    
    /**
     * Creates javascript code that will render an e-mail address so that it is
     * harder for crawlers to pick up. 
     * <p>
     * A noscript tag is also attached, so that users with javascript disabled 
     * can understand why they cannot see the e-mail address.
     * 
     * @param email The e-mail address to print as javascript code.
     * @param createAsMailtoLink If true, the e-mail address will be a mailto-link.
     * @param subject The subject line of the mailto-link, can be null.
     * @return The e-mail address as javascript code.
     */
    public static String getJavascriptEmail(String email, boolean createAsMailtoLink, String subject) {
        String js = "<script type=\"text/javascript\">";
        String [] parts = email.split("@");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Non-valid e-mail address: " + email);
        }
        js += "\r\n/*<![CDATA[*/";
        js += "\r\nvar domain = '" + parts[1] + "';";
        js += "\r\nvar address = '" + parts[0] + "';";
        if (createAsMailtoLink) {
            js += "\r\ndocument.write('<a href=\"mailto:' + address + '@' + domain + " + 
                    (subject != null ? ("'?subject=" + subject + "' + ") : "") + "'\">' + address + '@' + domain + '</a>');";
        }
        else {
            js += "\r\ndocument.write(address + '@' + domain);";
        }
        js += "\r\n/*]]>*/";
        js += "\r\n</script>";
        //js += "<object type=\"text\"><noscript>" + EMAIL_NOSCRIPT_MSG + "</noscript></object>";
        js += "<noscript>" + EMAIL_NOSCRIPT_MSG + "</noscript>";
        return js;
    }
    
    /**
     * Obfuscates a mailto link using javascript, making it more difficult for
     * crawlers to detect.
     * 
     * @param mailtoLink The mailto link, as a complete anchor tag (&lt;a href="mailto:..." ...&gt;...&lt;/a&gt;).
     * @return The javascript obfuscated mailto link.
     */
    public static String getJavascriptMailto(String mailtoLink) {
        final String MAILTO_HREF_PREFIX = "mailto:";
        // Get all attributes
        Map attributes = getTagAttributesAsMap(mailtoLink);
        
        // Remove the href attribute from the attributes map, but keep the value
        String href = (String)attributes.remove("href");
        if (href == null) {
            throw new NullPointerException("Unexpected format on mailto-link: [" + mailtoLink + "]");
        }
        // Remove any "mailto:" prefix, so we're left with the address and parameter string (if any)
        href = href.replace(MAILTO_HREF_PREFIX, "");
        
        // Get an iterator for the attributes map 
        Set keys = attributes.keySet();
        Iterator iKey = keys.iterator();
        // Build the miscellaneous attributes string
        String miscAttribs = "";
        while (iKey.hasNext()) {
            String key = (String)iKey.next();
            miscAttribs += " " + key + "=\"" + (String)attributes.get(key) + "\"";
        }
        
        // Get the link text
        String linkText = getTagStringValue(mailtoLink);
        
        // Split the address and parameter string
        String emailAddr = href;
        String emailParamStr = null;
        if (href.contains("?")) {
            emailAddr = href.substring(0, href.indexOf("?"));
            emailParamStr = href.substring(href.indexOf("?"));
        }
        
        String[] emailAddrParts = emailAddr.split("@");
        if (emailAddrParts.length < 2) {
            throw new IllegalArgumentException("Non-valid e-mail address: " + emailAddr);
        }
        String addr = emailAddrParts[0];
        String domain = emailAddrParts[1];
        
        // Build the javascript code
        String js = "<script type=\"text/javascript\">";    
        js += "\r\n/*<![CDATA[*/";    
        js += "\r\nvar address = '" + addr + "';";
        js += "\r\nvar domain = '" + domain + "';";
        js += "\r\ndocument.write('<a href=\"" + MAILTO_HREF_PREFIX + "' + address + '@' + domain + '" + 
                (emailParamStr == null ? "" : emailParamStr.replaceAll("'", "\\\\'")) + "\"" + miscAttribs + ">');";
        if (linkText.equalsIgnoreCase(addr + "@" + domain)) // The mailto link text is the e-mail address -> obfuscate the address in the link as well
            js += "\r\ndocument.write(address + '@' + domain + '</a>');";
        else // The mailto link text is something other than the address
            js += "\r\ndocument.write('" + linkText + "</a>');";
        js += "\r\n/*]]>*/";
        js += "\r\n</script>";
        //js += "<object type=\"text\"><noscript>" + EMAIL_NOSCRIPT_MSG + "</noscript></object>";
        js += "<noscript>" + EMAIL_NOSCRIPT_MSG + "</noscript>";
        return js;
    }
    
    
    /**
     * Gets the value of a given attribute within the given tag code.
     * 
     * @param tagCode The tag (must start with &lt; and end with &gt;).
     * @param attribName The name of the attribute to get the value for.
     * @return The value of the given attribute, or null if no such attribute exists.
     * @throws java.lang.IllegalArgumentException if the tag is not formatted correctly.
     */
    protected static String getAttribValue(String tagCode, String attribName) throws IllegalArgumentException {
        final String VALUE_START_TOKEN = "=\"";
        String[] tagSplit = tagCode.split(">");
        
        if (tagSplit.length < 2) {
            throw new IllegalArgumentException("Argument string was not a well-formed tag, required character '>' was missing.");
        }
        
        String attrib = null;
        
        try {
            if (tagSplit[0].contains(attribName.concat("="))) {
                int startIndex = tagSplit[0].indexOf(attribName.concat(VALUE_START_TOKEN)) + attribName.length() + VALUE_START_TOKEN.length();            
                attrib = tagSplit[0].substring(startIndex);
                attrib = attrib.substring(0, attrib.indexOf("\""));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Argument string was not correctly formatted, is the character sequence '" + attribName + "=\"...\"' missing?");
        }
        return attrib;
    }
    
    /**
     * Gets the value enclosed in an HTML/XML tag as a String value.
     * <p>
     * E.g. gets the link text in a hyperlink: for &lt;a href="/link/path.html"&gt;link text&lt;/a&gt;, 
     * the returned value will be "link text".
     * 
     * @param tag The complete enclosing HTML/XML tag.
     * @return The value enclosed by the HTML/XML tag.
     */
    public static String getTagStringValue(String tag) throws IllegalArgumentException {
        try {
            // tag = "<tag ....>Value</tag>"
            String[] tagSplit = tag.split(">");
            // tagSplit[0] = "<tag"
            // tagSplit[1] = "Value</tag"
            String val = tagSplit[1].substring(0, tagSplit[1].indexOf("<"));
            // val = "Value"
            return val;
        } catch (Exception e) {
            throw new IllegalArgumentException("Argument string '" + tag + "' was not a well-formed tag.");
        }
    }
    
    /**
     * Returns a map containing all attributes within a tag, where the map keys 
     * are the attribute names and the map values are the attribute values.
     * 
     * @param tag The complete HTML/XML tag.
     * @return A map of all attributes, or an empty map if none.
     */
    public static Map getTagAttributesAsMap(String tag) {
        final String VALUE_START_TOKEN = "=\"";
        final String VALUE_END_TOKEN = "\"";
        
        HashMap attribs = new HashMap();
        
        try {
            /*String[] tagSplit = tag.split(">");
            allAttribs = tagSplit[0].trim().substring(tagSplit[0].indexOf(" ")).trim();*/
            
            // Separate the opening tag in a string
            String openingTag = (tag.endsWith(" />")) ? tag.replace(" />", "") : tag.split(">")[0];
            
            // Separate the attributes contained in the opening tag in a string
            String allAttribs = openingTag.trim().substring(openingTag.indexOf(" ")).trim();
            
            while (allAttribs.length() > 0) {
                int firstQuoteIndex = allAttribs.indexOf(VALUE_START_TOKEN) + 1;
                int secondQuoteIndex = allAttribs.indexOf(VALUE_END_TOKEN, firstQuoteIndex + 1);
                String attrib = allAttribs.substring(0, secondQuoteIndex + 1);
                String key = attrib.substring(0, attrib.indexOf(VALUE_START_TOKEN));
                String val = attrib.substring(attrib.indexOf(VALUE_START_TOKEN)+2);
                val = val.substring(0, val.length() - 1);
                attribs.put(key, val);
                allAttribs = allAttribs.replace(attrib, "").trim();
            }
            return attribs;
        } catch (Exception e) {
            throw new IllegalArgumentException("Argument string '" + tag + "' was not a well-formed tag. (" + e.getMessage() + ")");
        }
    }
    
    /**
     * Creates a parameter string from a given map of parameters.
     * <p>
     * The parameter string can then be appended to a location (URI) to pass the 
     * parameters.
     * <p>
     * The given map must have keys (parameter names) of type String, and the 
     * values (parameter values) must be either of type String or String[].
     * 
     * @param params The parameter map, for instance request.getParameterMap().
     * @return The parameters.
     */
    public static String getParameterString(Map params) {
        if (params == null)
            return null;
        
        Iterator<String> iKeys = params.keySet().iterator();
        String paramStr = "";
        while (iKeys.hasNext()) {
            String key = iKeys.next();
            if (params.get(key) instanceof String[]) { // Multiple values (standard form)
                String[] values = (String[])params.get(key);
                //for (int i = 0; i < values.length; i++) {
                for (String val : values) {
                    //String val = values[i];
                    if (val != null && !val.trim().isEmpty()) {
                        if (paramStr.length() > 0)
                            paramStr += "&amp;";
                        paramStr += key + "=" + val;
                    }
                }
            }
            else if (params.get(key) instanceof String) { // Single value
                if (paramStr.length() == 0)
                    paramStr += key + "=" + (String)params.get(key);
                else
                    paramStr += "&amp;" + key + "=" + (String)params.get(key);
            }
        }
        return paramStr;
    }
    
    /**
     * Redirects to the given location using the given type.
     * <p>
     * The type must be either 301 (permanent) or 302 (temporary).
     * <p>
     * <em>This method is a substitute for the core method 
     * {@link org.opencms.util.CmsRequestUtil#redirectPermanently(org.opencms.jsp.CmsJspActionElement, java.lang.String), 
     * which is erroneously implemented, causing it to issue temporary (302) 
     * redirects instead of the expected permanent (301) redirects.</em>
     * 
     * @param location The redirect target location. Local paths should be site-relative (starting with a slash).
     * @param type The type of redirect. Pass 301 for permanent, 302 for temporary.
     */
    public void sendRedirect(String location, int type) {
        String target = location;
        boolean targetIsLocal = false;
        String redirLocation = null;
        
        if (target.startsWith("//")) {
            // Absolute and scheme-agnostic target location
            target = this.getRequest().getScheme() + ":" + target;
        } else if (!target.substring(0,7).contains(":")) {
            // Relative target location
            // (No "http:", "https:", "ftp:", etc. in the first part)
            targetIsLocal = true;
            
            // Ensure the target location is site-relative 
            // (Ideally, this should never happen but who knows...)
            if (!target.startsWith("/")) {
                // Assume the target location is folder-relative
                if (target.startsWith("./")) {
                    target = target.substring(2);
                }
                target = this.getRequestContext().getFolderUri() + target;
            }
        } else {
            // Assume absolute target location, e.g. starting with "http://xxxx"
        }
        
        if (targetIsLocal) { // Relative path
            try {
                redirLocation = this.link(target);
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Cannot link to " + target + " in preparation of redirect.", e);
                }
            }
        } else { // Absolute path
            redirLocation = target;
        }

        //
        // Execute the redirect
        //
        switch (type) {
            case 301:
                this.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // does work
                //cms.getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // does NOT work
                this.getResponse().setHeader("Location", redirLocation);
                this.getResponse().setHeader("Connection", "close"); // Maybe not necessary?
                //CmsRequestUtil.redirectPermanently(cms, redirLocation); // does NOT work - uses HttpServletResponse#sendRedirect
                break;
            case 302:
                try {
                    this.getResponse().sendRedirect(redirLocation);
                } catch (Exception e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Unable to issue temporary redirect to " + redirLocation + ".", e);
                    }
                }
                break;
            default:
                if (LOG.isErrorEnabled()) {
                    LOG.error("Error issuing redirect to " + redirLocation + ": Expected a type of 301 or 302, but got " + type + ".");
                }
                break;
        }
    }
}