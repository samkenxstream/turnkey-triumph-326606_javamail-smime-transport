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
