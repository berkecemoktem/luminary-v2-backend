package com.luminary.platform.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback adapter used when no SMTP transport is configured (for example in
 * local development or tests). It never transmits anything; it only logs the
 * message so a delivery path can be observed without a mail server.
 */
public class LoggingMailSender implements MailSender {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(MailMessage message) {
        log.info("MAIL(to={}, subject={}) body={}",
                message.to(), message.subject(), message.textBody());
    }

    @Override
    public String toString() {
        return "LoggingMailSender[no-smtp-configured]";
    }
}