# Java Mail S/MIME Transport

When this library is added to a project you may configure a Java Mail session to use the `smtp+smime` or `smtps+smime` protocol to have the transport sign outgoing mail with a mail certificate stored in a Java keystore (JKS) file.

## Configuration
The following properties need to be set to configure the Java mail session

| Property                                | Description |
|-----------------------------------------|-------------|
| `mail.transport.protocol`                 | Special protocol to use: `smtp+smime` or `smtps+smime` |
| `mail.keystore.file`                      | Keystore file containing certificate |
| `mail.keystore.password`                  | Password for keystore file |
| `mail.keystore.<email address>.password`  | Password for `email address` alias in keystore |
 
If you omit the `mail.keystore.<email address>.password` configuration, the process will look for an alias 
in the keystore that matches the email "from" address and use the keystore password 

## Examples

### Java Mail
```java
// example class
class Mailer {
    
    Session mailSession;
    
    public Mailer(String smtpHost, String smtpPort, String username, String password) {
        properties.put("mail.transport.protocol", "smtp+smime");
        properties.put("mail.smtp+smime.host",  smtpHost);
        properties.put("mail.smtp+smime.port", smtpPort);
        properties.put("mail.smtp+smime.auth", "true");
        properties.put("mail.smtp+smime.starttls.enable", "true");
        
        Authenticator auth = new SMTPAuthenticator(username, password);
        mailSession = Session.getInstance(properties, new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
             return new PasswordAuthentication(username, password);
          }
       });        
    }
    
    public void mail(String fromAddress, String toAddress, String subject, String text) {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromAddress));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
        message.setSubject(subject);
        message.setText(text);
        Transport.send(message);        
    }
}
```

### Spring Mail
```yml
# application.yml
spring.mail:
  protocol: smtp+smime
  host: <smtp-host>
  port: <smtp-port>
  properties.mail:
    keystore:
      file: <path-to-keystore>
    smtp+smime.starttls.enable: true
```

```java
class Mailer {
    
    @Autowired
    private MailSender mailSender;
    
    public void mail(String fromAddress, String toAddress, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toAddress);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);        
    }    
}
```

