package edu.iu.uits.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class SignedEmailIT {

    private Properties properties = new Properties();
    private GreenMail greenMail;
    private Session session;


    private static final int SMTP_PORT = 4025;
    private static final String SMTP_PROTOCOL = "smtp+smime";
    private static final String EMAIL_ADDRESS_WITH_CERT = "workflow.noreply@iu.edu";
    private static final String EMAIL_ADDRESS_WITHOUT_CERT = "workflow.nocert.iu.edu";

    @Before
    public void setup() throws IOException {
        // properties
        final InputStream input = new FileInputStream("/opt/j2ee/security/kr/rice-keystore.properties");
        Properties keystoreProperties = new Properties();
        keystoreProperties.load(input);
        properties.put("mail.keystore.file",  keystoreProperties.getProperty("keystore.file"));
        properties.put("mail.keystore.password", keystoreProperties.getProperty("keystore.password"));
        properties.put("mail.keystore.workflow.noreply.password", keystoreProperties.getProperty("workflow.noreply@iu.edu"));
        // server setup
        final ServerSetup serverSetup = new ServerSetup(SMTP_PORT, null, SMTP_PROTOCOL);
        // server
        greenMail = new GreenMail(serverSetup);
        greenMail.start();
        // session
        session = GreenMailUtil.getSession(serverSetup, properties);

    }

    @After
    public void tearDown() {
        greenMail.stop();
    }


    @Test
    @DisplayName("Test that a basic email is signed")
    public void testTextEmailIsSigned() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setText("content");
        msg.setFrom(new InternetAddress(EMAIL_ADDRESS_WITH_CERT));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Testing signed text email");
        Transport.send(msg);

        Message sentMessage = getSentMessage();

        assertTrue(hasSignedContentType(sentMessage));
    }

    @Test
    @DisplayName("Test that an email sent from an address without a certificate is not signed")
    public void testEmailAddressWithoutCertIsNotSigned() throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setText("content");
        msg.setFrom(new InternetAddress(EMAIL_ADDRESS_WITHOUT_CERT));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Testing email address without cert");
        Transport.send(msg);

        Message sentMessage = getSentMessage();

        assertFalse(hasSignedContentType(sentMessage));
    }

    @Test
    @DisplayName("Test that text/html emails are signed")
    public void testTextHtmlEmailIsSigned() throws MessagingException, IOException {
        MimeMessage msg = new MimeMessage(session);
        msg.setContent("<p>content</p>", "text/html");
        msg.saveChanges();
        msg.setFrom(new InternetAddress(EMAIL_ADDRESS_WITH_CERT));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Testing signed text/html email");
        Transport.send(msg);

        Message sentMessage = getSentMessage();

        // message is signed
        assertTrue(hasSignedContentType(sentMessage));
        // message preserves original content type
        assertTrue(getInnerContentType(sentMessage).contains("text/html"));

    }


    @Test
    @DisplayName("Test that a multi-part email is properly signed")
    public void testMultipartEmailIsSigned() throws MessagingException, IOException {
        Message msg = new MimeMessage(session);
        Multipart multiPart = new MimeMultipart("alternative");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("text content", "utf-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<p>html content</p>", "text/html; charset=utf-8");

        multiPart.addBodyPart(htmlPart);
        multiPart.addBodyPart(textPart);
        msg.setContent(multiPart);

        msg.setFrom(new InternetAddress(EMAIL_ADDRESS_WITH_CERT));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Testing signed multipart email");
        Transport.send(msg);

        Message sentMessage = getSentMessage();
        // email is signed
        assertTrue(hasSignedContentType(sentMessage));
        // email has correct inner type
        assertTrue(getInnerContentType(sentMessage).contains("multipart/alternative"));

    }

    private Message getSentMessage() {
        Message[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        return  messages[0];
    }

    private boolean hasSignedContentType(Message sentMessage) throws MessagingException {
        Enumeration<Header> headers = sentMessage.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            System.out.println(String.format("%s : %s", header.getName(), header.getValue()));
            if (header.getName().equals("Content-Type")) {
                return header.getValue().contains("multipart/signed; protocol=\"application/pkcs7-signature\";");
            }
        }
        return false;
    }

    private String getInnerContentType(Message sentMessage) throws MessagingException, IOException {
        MimeMultipart content = (MimeMultipart) sentMessage.getContent();
        return content.getBodyPart(0).getHeader("Content-Type")[0];
    }
}

