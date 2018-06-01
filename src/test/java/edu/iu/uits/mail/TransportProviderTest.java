package edu.iu.uits.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TransportProviderTest {

    @Test
    @DisplayName("Test smtp+smime protocol returns proper transport")
    public void testSmtpSmimeProtocolTransport() throws NoSuchProviderException {
        try {
            Session mailSession = Session.getInstance(new Properties());

            Transport mailTransport = mailSession.getTransport("smtp+smime");

            assertTrue(mailTransport instanceof SMimeSMTPTransport, "The mail transport for the smtp+smime protocol is not correct");
        } catch (NoSuchProviderException e) {
            fail("No provider found for the smtp+smime protocol");
        }
    }

    @Test
    @DisplayName("Test smtps+smime protocol returns proper transport")
    public void testSmtpSSLSmimeProtocolTransport() throws NoSuchProviderException {
        try {
            Session mailSession = Session.getInstance(new Properties());

            Transport mailTransport = mailSession.getTransport("smtps+smime");

            assertTrue(mailTransport instanceof SMimeSMTPSSLTransport, "The mail transport for the smtps+smime protocol is not correct");
        } catch (NoSuchProviderException e) {
            fail("No provider found for the smtps+smime protocol");
        }
    }
}
