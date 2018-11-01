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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyStore;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MailSignerTest {

    private static final String TEST_ADDRESS_LOCAL_PART = "foo";
    private static final String TEST_ADDRESS = TEST_ADDRESS_LOCAL_PART +  "@bar.baz";
    private static final String KEYSTORE_PASSWORD = "k3yst0r3p@ssw0rd";
    private static final String ALIAS_PASSWORD = "@l1@sp@ssw0rd";

    @Mock
    private KeyStore keyStore;

    private Properties properties;
    private MailSigner mailSigner;

    @BeforeEach
    public void setup() {
        properties = new Properties();
        properties.setProperty("mail.keystore.password", KEYSTORE_PASSWORD);
    }

    @Test
    @DisplayName("Test basic case of finding password property based on full email")
    public void testGetEmailPasswordFullEmail() {
        properties.setProperty(String.format(MailSigner.CERT_PASSWORD_PROPERTY_TEMPLATE, TEST_ADDRESS), ALIAS_PASSWORD);
        mailSigner = new MailSigner(properties, keyStore);
        String emailPassword = mailSigner.getEmailPassword(TEST_ADDRESS);
        assertEquals(ALIAS_PASSWORD, emailPassword);
    }


    @Test
    @DisplayName("Test basic case of finding password property based on local part of email")
    public void testGetEmailPasswordLocalAddressPart() {
        properties.setProperty(String.format(MailSigner.CERT_PASSWORD_PROPERTY_TEMPLATE, TEST_ADDRESS_LOCAL_PART), ALIAS_PASSWORD);
        mailSigner = new MailSigner(properties, keyStore);
        String emailPassword = mailSigner.getEmailPassword(TEST_ADDRESS);
        assertEquals(ALIAS_PASSWORD, emailPassword);
    }

    @Test
    @DisplayName("Test finding password property based on local host and full address property being non-null, but empty")
    public void testGetEmailPasswordLocalAddressEmptyFullAddress() {
        properties.setProperty(String.format(MailSigner.CERT_PASSWORD_PROPERTY_TEMPLATE, TEST_ADDRESS), "");
        properties.setProperty(String.format(MailSigner.CERT_PASSWORD_PROPERTY_TEMPLATE, TEST_ADDRESS_LOCAL_PART), ALIAS_PASSWORD);
        mailSigner = new MailSigner(properties, keyStore);
        String emailPassword = mailSigner.getEmailPassword(TEST_ADDRESS);
        assertEquals(ALIAS_PASSWORD, emailPassword);
    }

    @Test
    @DisplayName("Test defaulting to keystore password when email-specific property is not defined")
    public void testGetEmailPasswordKeystore() {
        mailSigner = new MailSigner(properties, keyStore);
        String emailPassword = mailSigner.getEmailPassword(TEST_ADDRESS);
        assertEquals(KEYSTORE_PASSWORD, emailPassword);
    }

    @Test
    @DisplayName("Test defaulting to keystore password when email-specific property is defined, but empty")
    public void testGetEmailPasswordKeystoreEmptyLocalPart() {
        properties.setProperty(String.format(MailSigner.CERT_PASSWORD_PROPERTY_TEMPLATE, TEST_ADDRESS_LOCAL_PART), "");
        mailSigner = new MailSigner(properties, keyStore);
        String emailPassword = mailSigner.getEmailPassword(TEST_ADDRESS);
        assertEquals(KEYSTORE_PASSWORD, emailPassword);
    }

}
