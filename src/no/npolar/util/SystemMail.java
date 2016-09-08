package no.npolar.util;

import org.apache.commons.mail.EmailException;
//import org.opencms.file.CmsObject;
import org.opencms.mail.CmsMailSettings;
import org.opencms.mail.CmsSimpleMail;
//import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows for sending - typically system-generated - emails, using the global
 * email configuration.
 * <p>
 * Created due to massive issues sending emails as an authenticated user in 
 * OpenCms 8.5.
 * 
 * @author Paul-Inge Flakstad, Norwegian Polar Institute
 * @see no.npolar.common.forms.AutoEmail
 */
public class SystemMail {
    private class AutoMailAuthenticator extends javax.mail.Authenticator {
        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                OpenCms.getSystemInfo().getMailSettings().getDefaultMailHost().getUsername(), 
                OpenCms.getSystemInfo().getMailSettings().getDefaultMailHost().getPassword()
            );
        }
    }
    /** The recipient address line. */
    private String toAddr = null;
    /** The sender address. */
    private String fromAddr = null;
    /** The sender name. */
    private String fromName = null;
    /** The subject line. */
    private String subject = null;
    /** The message body. */
    private String message = null;
    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(SystemMail.class);;
    /** 
     * The global OpenCms email settings.
     * @see file opencms-system.xml 
     */
    private final CmsMailSettings SETTINGS = OpenCms.getSystemInfo().getMailSettings();
    
    /**
     * Creates a new, blank instance.
     */
    public SystemMail() {
        fromAddr = SETTINGS.getMailFromDefault();
    }

    /**
     * Gets the recipient address line.
     * <p>
     * Multiple recipients are delimited by semicolon(s).
     * 
     * @return the recipient address line.
     */
    public String getToAddr() {
        return toAddr;
    }

    /**
     * Sets the recipient address(es).
     * <p>
     * To add multiple recipients, use semicolon as delimiter.
     * 
     * @param toAddr the recipient address(es).
     */
    public void setToAddr(String toAddr) {
        this.toAddr = toAddr;
    }

    /**
     * Gets the sender address.
     * 
     * @return the sender address.
     */
    public String getFromAddr() {
        return fromAddr;
    }

    /**
     * Sets the sender address. 
     * 
     * @param fromAddr the sender address.
     */
    public void setFromAddr(String fromAddr) {
        this.fromAddr = fromAddr;
    }

    /**
     * Gets the sender name.
     * 
     * @return the sender name.
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * Sets the sender name.
     * 
     * @param fromName the sender name.
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * Gets the subject line.
     * 
     * @return the subject line.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject line.
     * 
     * @param subject the subject line.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets the message body.
     * 
     * @return the message body.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message body.
     * 
     * @param message the message body.
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Constructs an email object, ready to send.
     * <p>
     * This is typically the last method one would call right before sending, 
     * after all necessary fields have been set.
     * 
     * @return An email object, ready to send.
     * @throws EmailException If anything goes wrong.
     */    
    public CmsSimpleMail getSimpleEmail() throws EmailException {
        CmsSimpleMail mail = new CmsSimpleMail();
        mail.setCharset("utf-8");
        
        String[] recipients = toAddr.split(";");
        for (int i = 0; i < recipients.length; i++) {
            mail.addTo(recipients[i]);
        }
        mail.setFrom(fromAddr, fromName == null ? fromAddr : fromName, "utf-8");
        mail.setSubject(subject);
        mail.setMsg(message);
        
        return mail;
    }
    
    /**
     * Sends the email.
     */
    public synchronized void send() {
        //CmsMailSettings mailSettings = OpenCms.getSystemInfo().getMailSettings();

        if (SETTINGS.getDefaultMailHost().isAuthenticating() && SETTINGS.getDefaultMailHost().getProtocol().equalsIgnoreCase("smtp")) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SETTINGS.getDefaultMailHost().getHostname());
            props.put("mail.smtp.port", String.valueOf(SETTINGS.getDefaultMailHost().getPort()));

            Session mailSession = Session.getInstance( props, new AutoMailAuthenticator() );

            try {
                Message msg = new MimeMessage(mailSession);

                msg.setFrom(new InternetAddress(fromAddr));
                msg.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(toAddr.replace(";", ","))
                );
                msg.setSubject(subject);
                msg.setText(message);

                mailSession.getTransport("smtp").send(msg);

            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unable to send automatic email.", e);
                }
            }
        } else {
            try {
                getSimpleEmail().send();
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unable to send automatic email.", e);
                }
            }
        }
    }
}
