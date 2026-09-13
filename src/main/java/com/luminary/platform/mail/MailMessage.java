package com.luminary.platform.mail;

/**
 * A message queued for delivery by a {@link MailSender}. Both a plain-text
 * and an HTML alternative may be supplied; the sender picks whichever the
 * receiving client supports.
 */
public record MailMessage(String to, String subject, String textBody,
                          String htmlBody) {
}