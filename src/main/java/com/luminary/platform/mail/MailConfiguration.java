package com.luminary.platform.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Selects the active mail adapter. When {@code spring.mail.host} is configured
 * a real SMTP sender exists; otherwise a logging fallback keeps the
 * application runnable without a mail server. The platform mail port is the
 * only mail seam a domain module may depend on.
 */
@Configuration
public class MailConfiguration {

    @Bean
    MailSender mailSender(ObjectProvider<JavaMailSender> javaMailSender,
                          MailProperties properties) {
        JavaMailSender sender = javaMailSender.getIfAvailable();
        if (sender != null) {
            return new SmtpMailSender(sender, properties);
        }
        return new LoggingMailSender();
    }
}