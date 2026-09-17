package com.luminary.platform.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingMailSenderTest {

    @Test
    void send_neverThrows_andLogs() {
        LoggingMailSender sender = new LoggingMailSender();

        assertThatCode(() -> sender.send(new MailMessage(
                "student@luminary.dev", "Join a school on Luminary",
                "plain body", "<p>html body</p>")))
                .doesNotThrowAnyException();
    }
}