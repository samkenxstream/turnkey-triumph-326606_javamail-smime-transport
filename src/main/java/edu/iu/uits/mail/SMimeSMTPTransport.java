package edu.iu.uits.mail;

import com.sun.mail.smtp.SMTPTransport;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Optional;

public class SMimeSMTPTransport extends SMTPTransport {

    private MailSigner mailSigner;

    public SMimeSMTPTransport(Session session, URLName urlname) {
        super(session, urlname);
        mailSigner = new MailSigner(session.getProperties());
    }

    protected SMimeSMTPTransport(Session session, URLName urlname, String name, boolean isSSL) {
        super(session, urlname, name, isSSL);
        mailSigner = new MailSigner(session.getProperties());
    }

    @Override
    public synchronized void sendMessage(Message message, Address[] addresses) throws MessagingException {
        Optional<MimeMessage> signedMessage = mailSigner.signMessage((MimeMessage)message);
        super.sendMessage(signedMessage.isPresent() ? signedMessage.get() : message, addresses);
    }

}
