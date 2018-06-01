package edu.iu.uits.mail;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

@Slf4j
public class MailSigner {

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
                String emailKeyPassword = properties.getProperty(String.format("mail.keystore.%s.password", signingAddress.get()));
                if(emailKeyPassword == null || emailKeyPassword.trim().isEmpty()) {
                    log.debug(String.format("No key password found for %s.  Defaulting to using the keystore password.", signingAddress.get()));
                    emailKeyPassword = properties.getProperty("mail.keystore.password");
                }
                return Optional.of(MailSigner.signMessage(mimeMessage, (PrivateKey)keyStore.getKey(signingAddress.get(), emailKeyPassword.toCharArray()), keyStore.getCertificateChain(signingAddress.get())[0]));
            } else {
                log.info("Could not find an email certificate for any of the from addresses: " + from);
                return Optional.empty();
            }
        } catch (MessagingException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            log.error("Caught exception when attempting to sign a message.  The message will be sent unsigned.", e);
            return Optional.empty();
        }
    }

    public static MimeMessage signMessage(final MimeMessage message, PrivateKey privateKey, Certificate certificate) {
        try {
            MailcapCommandMap mailcap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();

            mailcap.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
            mailcap.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
            mailcap.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
            mailcap.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
            mailcap.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");

            // Create the SMIMESignedGenerator
            SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
            capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
            capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
            capabilities.addCapability(SMIMECapability.dES_CBC);
            capabilities.addCapability(SMIMECapability.aES256_CBC);
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            ASN1EncodableVector attributes = new ASN1EncodableVector();
            X500Name x500Name =  new X500Name(((X509Certificate) certificate)
                    .getIssuerDN().getName());
            IssuerAndSerialNumber issuerAndSerialNumber = new IssuerAndSerialNumber(x500Name, ((X509Certificate) certificate).getSerialNumber());
            attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(issuerAndSerialNumber));
            attributes.add(new SMIMECapabilitiesAttribute(capabilities));

            SMIMESignedGenerator signer = new SMIMESignedGenerator();
            signer.addSigner(
                    privateKey,
                    (X509Certificate) certificate,
                    "DSA".equals(privateKey.getAlgorithm()) ? SMIMESignedGenerator.DIGEST_SHA1
                            : SMIMESignedGenerator.DIGEST_MD5,
                    new AttributeTable(attributes), null);

            // Add the list of certs to the generator
            List<Certificate> certList = new ArrayList<Certificate>();
            certList.add(certificate);
            CertStore certs = CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(certList), "BC");
            signer.addCertificatesAndCRLs(certs);

            // Sign the message
            MimeMultipart mm = signer.generate(message, "BC");
            MimeMessage signedMessage = new MimeMessage(message.getSession());

            // Set all original MIME headers in the signed message
            Enumeration headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                signedMessage.addHeaderLine((String) headers.nextElement());
            }

            // Set the content of the signed message
            signedMessage.setContent(mm);
            return signedMessage;
        } catch (NoSuchAlgorithmException |
                SMIMEException |
                CertStoreException |
                InvalidAlgorithmParameterException |
                MessagingException |
                NoSuchProviderException e) {
            log.error("Caught exception when attempting to sign a message. Message will be sent unsigned", e);
        }

        return message;
    }
}
