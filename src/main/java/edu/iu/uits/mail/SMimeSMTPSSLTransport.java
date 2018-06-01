package edu.iu.uits.mail;

import com.sun.mail.smtp.SMTPSSLTransport;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Optional;

public class SMimeSMTPSSLTransport extends SMTPSSLTransport {

    private MailSigner mailSigner;

    public SMimeSMTPSSLTransport(Session session, URLName urlname) {
        super(session, urlname);
        mailSigner = new MailSigner(session.getProperties());
    }

    @Override
    public synchronized void sendMessage(Message message, Address[] addresses) throws MessagingException {
        Optional<MimeMessage> signedMessage = mailSigner.signMessage((MimeMessage)message);
        super.sendMessage(signedMessage.isPresent() ? signedMessage.get() : message, addresses);
    }

}
