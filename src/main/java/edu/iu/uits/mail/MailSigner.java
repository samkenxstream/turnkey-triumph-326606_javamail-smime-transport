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

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
public class MailSigner {

    public static final String CERT_PASSWORD_PROPERTY_TEMPLATE = "mail.keystore.%s.password";

    private static final String LOCAL_ADDRESS_REGEX = "^(.*)@.*$";

    private KeyStore keyStore;
    private Properties properties;

    public MailSigner(Properties properties) {
        this.properties = properties;
        String keyStoreFile = properties.getProperty("mail.keystore.file");
        String keyStorePassword = properties.getProperty("mail.keystore.password");

        try {
            if(keyStoreFile != null && !keyStoreFile.trim().isEmpty() && keyStorePassword != null && !keyStorePassword.trim().isEmpty()) {
                keyStore = KeyStore.getInstance("JKS");
                keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
            } else {
                log.warn("No mail keystore file or password set.  No emails will be signed.");
            }
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            log.error("Caught exception attempting to load mail keystore.  No emails will be signed.", e);
        }
    }

    public MailSigner(Properties properties, KeyStore keyStore) {
        this.properties = properties;
        this.keyStore = keyStore;
    }



    public Optional<MimeMessage> signMessage(MimeMessage mimeMessage) {
        if(keyStore == null) {
            log.warn("No keystore provided so the message will not be signed");
            return Optional.empty();
        }
        try {
            Address[] from = mimeMessage.getFrom();


            Optional<String> signingAddress = Arrays.stream(from).map(Address::toString).filter(address -> {
                try {
                    return keyStore.containsAlias(address);
                } catch (KeyStoreException e) {
                    return false;
                }
            }).findFirst();

            if(signingAddress.isPresent()) {
                String alias = signingAddress.get();
                String emailKeyPassword = getEmailPassword(alias);
                return Optional.of(MailSigner.signMessage(mimeMessage, (PrivateKey)keyStore.getKey(alias, emailKeyPassword.toCharArray()), ((X509Certificate) keyStore.getCertificateChain(alias)[0])));
            } else {
                log.info("Could not find an email certificate for any of the from addresses: " + from);
                return Optional.empty();
            }
        } catch (MessagingException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            log.error("Caught exception when attempting to sign a message.  The message will be sent unsigned.", e);
            return Optional.empty();
        }
    }

    protected String getEmailPassword(String alias) {
        String emailKeyPassword = properties.getProperty(String.format(CERT_PASSWORD_PROPERTY_TEMPLATE, alias));
        if (emailKeyPassword == null || emailKeyPassword.trim().isEmpty()) {
            // try local part of address
            final String localPart = alias.replaceAll(LOCAL_ADDRESS_REGEX, "$1");
            log.debug(String.format("No key password found for %s.  Trying local portion, %s.", alias, localPart));
            emailKeyPassword = properties.getProperty(String.format("mail.keystore.%s.password", localPart));
            if (emailKeyPassword == null || emailKeyPassword.trim().isEmpty()) {
                // default to keystore password
                log.debug(String.format("No key password found for %s or %s.  Defaulting to using the keystore password.", alias, localPart));
                emailKeyPassword = properties.getProperty("mail.keystore.password");
            }
        }
        return emailKeyPassword;
    }

    public static MimeMessage signMessage(final MimeMessage message, PrivateKey privateKey, X509Certificate certificate)  {
        try {
            Store certs = new JcaCertStore(Collections.singletonList(certificate));

            ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
            SMIMECapabilityVector caps = new SMIMECapabilityVector();
            caps.addCapability(SMIMECapability.dES_EDE3_CBC);
            caps.addCapability(SMIMECapability.rC2_CBC, 128);
            caps.addCapability(SMIMECapability.dES_CBC);
            caps.addCapability(SMIMECapability.aES256_CBC);
            signedAttributes.add(new SMIMECapabilitiesAttribute(caps));

            IssuerAndSerialNumber issuerAndSerialNumber = new IssuerAndSerialNumber(
                    new X500Name(certificate.getIssuerDN().getName()), certificate.getSerialNumber());
            signedAttributes.add(new SMIMEEncryptionKeyPreferenceAttribute(issuerAndSerialNumber));

            SMIMESignedGenerator gen = new SMIMESignedGenerator();

            gen.addSignerInfoGenerator(
                    new JcaSimpleSignerInfoGeneratorBuilder().setProvider(new BouncyCastleProvider())
                            .setSignedAttributeGenerator(new AttributeTable(signedAttributes))
                            .build("SHA1withRSA", privateKey, certificate));

            gen.addCertificates(certs);

            // the message could be just a plain text message, or it could be a multipart message, let's handle both!
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            Object messageContent = message.getContent();
            if (messageContent instanceof String) {
                mimeBodyPart.setContent(messageContent, message.getContentType());
            } else if (messageContent instanceof MimeMultipart) {
                mimeBodyPart.setContent((MimeMultipart)messageContent);
            }

            MimeMultipart signedMultipart = gen.generate(mimeBodyPart);

            MimeMessage signedMessage = new MimeMessage(message.getSession());
            signedMessage.setContent(signedMultipart, signedMultipart.getContentType());

            // Set all original headers in the signed message EXCEPT for any pre-existing Content-Type header,
            // our new Content-Type will be `multipart/signed`
            Enumeration headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                String headerLine = (String) headers.nextElement();
                if (!headerLine.startsWith("Content-Type:")) {
                    signedMessage.addHeaderLine(headerLine);
                }
            }
            signedMessage.saveChanges();

            return signedMessage;
        } catch (CertificateEncodingException |
                OperatorCreationException |
                IOException |
                MessagingException |
                SMIMEException e) {
            log.error("Caught exception when attempting to sign a message. Message will be sent unsigned", e);
            return message;
        }
    }
}
