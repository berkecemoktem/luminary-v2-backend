package com.luminary.platform.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpMailSenderTest {

    private JavaMailSender javaMailSender;
    private SmtpMailSender sender;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        MailProperties properties = new MailProperties();
        properties.setFrom("Luminary <noreply@luminary.dev>");
        sender = new SmtpMailSender(javaMailSender, properties);
    }

    @Test
    void sendsMessageWithFromSubjectAndRecipient() throws Exception {
        MimeMessage prepared = new MimeMessage(
                Session.getInstance(new Properties()));
        doAnswer(invocation -> {
            MimeMessagePreparator preparator = invocation.getArgument(0);
            preparator.prepare(prepared);
            return null;
        }).when(javaMailSender).send(any(MimeMessagePreparator.class));

        sender.send(new MailMessage("student@luminary.dev",
                "Join a school on Luminary", "plain body",
                "<p>html body</p>"));

        assertThat(prepared.getAllRecipients()[0].toString())
                .isEqualTo("student@luminary.dev");
        assertThat(prepared.getSubject()).isEqualTo(
                "Join a school on Luminary");
        assertThat(prepared.getFrom()[0].toString())
                .contains("noreply@luminary.dev");
        verify(javaMailSender).send(any(MimeMessagePreparator.class));
    }
}