package edu.iu.uits.mail;

/*-
 * #%L
 * JavaMail S/MIME Transport
 * %%
 * Copyright (C) 2018 Indiana University - UITS
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University - UITS nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
