package com.luminary.access.application;

import com.luminary.access.domain.TenantEntity;
import com.luminary.platform.mail.MailMessage;
import com.luminary.platform.mail.MailSender;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Composes and delivers the invitation email. The domain caller is
 * responsible for calling this at the right time (after the invitation
 * record is committed) and for deciding how a delivery failure is surfaced.
 */
@Component
public class InvitationMailer {

    private final MailSender mailSender;

    public InvitationMailer(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvitation(TenantEntity tenant, String email,
                               String acceptUrl, OffsetDateTime expiresAt) {
        String subject = "Join %s on Luminary".formatted(tenant.getName());
        mailSender.send(new MailMessage(email, subject,
                textBody(tenant, acceptUrl, expiresAt),
                htmlBody(tenant, acceptUrl, expiresAt)));
    }

    private String textBody(TenantEntity tenant, String acceptUrl,
                            OffsetDateTime expiresAt) {
        return """
                Hi,

                %s has invited you to join them on Luminary, an exam preparation
                and study platform.

                Open this link to activate your account and access your school:

                %s

                This link is single-use and expires on %s. If you did not
                expect this invitation, you can safely ignore this email.
                """.formatted(tenant.getName(), acceptUrl,
                formattedExpiry(expiresAt));
    }

    private String htmlBody(TenantEntity tenant, String acceptUrl,
                            OffsetDateTime expiresAt) {
        String name = htmlEscape(tenant.getName());
        String url = htmlEscape(acceptUrl);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:24px;font-family:Arial,\
                sans-serif;color:#1f2937;">
                  <div style="max-width:520px;margin:0 auto;">
                    <h2 style="margin-top:0;">Join %s on Luminary</h2>
                    <p>%s has invited you to join them on Luminary, an exam
                       preparation and study platform.</p>
                    <p>
                      <a href="%s"
                         style="display:inline-block;padding:12px 20px;\
                         background:#2563eb;color:#ffffff;text-decoration:none;\
                         border-radius:6px;">Activate your account</a>
                    </p>
                    <p style="font-size:13px;color:#6b7280;">
                      This link is single-use and expires on %s.<br>
                      If you did not expect this invitation, you can safely
                      ignore this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(name, name, url, formattedExpiry(expiresAt));
    }

    private static String formattedExpiry(OffsetDateTime expiresAt) {
        return expiresAt.toLocalDateTime()
                .format(java.time.format.DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static String htmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}