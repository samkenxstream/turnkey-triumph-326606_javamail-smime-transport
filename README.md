# Java Mail S/MIME Transport

When this library is added to a project you may configure a Java Mail session to use the `smtp+smime` or `smtps+smime` protocol to have the transport sign outgoing mail with a mail certificate stored in a Java keystore (JKS) file.

## Installation
### From Maven
Add the following as a dependency in your pom.xml
```xml
<dependency>
    <groupId>edu.iu.uits</groupId>
    <artifactId>javamail-smime-transport</artifactId>
    <version><!-- latest version --></version>
</dependency>
```

You can find the latest version in [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22edu.iu.uits%22%20AND%20a%3A%22javamail-smime-transport%22).

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

## Testing Configuration

### Unit Tests

The unit tests (under `src/test` can run without configuration). 

### Integration Tests

The integration tests (under `src/it`) require the following configuration:

A properties file whose default location is `/opt/j2ee/security/kr//opt/j2ee/security/kr/rice-keystore.properties`. This location can be overridden with a `KEYSTORE_PROPERTY_FILE_PATH` environment variable. 

The properties file should have `keystore.file` and `keystore.password` entries as well as at least one email address mapped to its certificate password. For example:

```properties
keystore.file=/path/to/keystore.jks
keystore.password=73cur3P@ssw0rd
foo@example.com=p@ssw0rd4al1as
```

## Adding license headers

Each source file in this project needs to include the standard license header at the top of the file.  This header can be added automatically by running the following:

```sh
mvn license:update-file-header
```

When a pull request is submitted the license headers will be verified and we will not be able to merge pull requests with files which lack this header.
