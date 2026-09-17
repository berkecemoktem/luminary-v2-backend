package com.luminary.platform.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * SMTP adapter backed by Spring's {@link JavaMailSender} (created by
 * auto-configuration only when {@code spring.mail.host} is configured). The
 * sender address comes from platform mail properties.
 */
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties properties;

    public SmtpMailSender(JavaMailSender javaMailSender,
                          MailProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    @Override
    public void send(MailMessage message) {
        MimeMessagePreparator preparator = mimeMessage ->
                prepare(mimeMessage, message);
        javaMailSender.send(preparator);
    }

    private void prepare(MimeMessage mimeMessage, MailMessage message)
            throws jakarta.mail.MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true,
                "UTF-8");
        helper.setFrom(properties.getFrom());
        helper.setTo(message.to());
        helper.setSubject(message.subject());
        boolean multipart = message.textBody() != null
                && message.htmlBody() != null;
        if (multipart) {
            helper.setText(message.textBody(), message.htmlBody());
        } else if (message.htmlBody() != null) {
            helper.setText(message.htmlBody(), true);
        } else {
            helper.setText(message.textBody(), false);
        }
    }
}