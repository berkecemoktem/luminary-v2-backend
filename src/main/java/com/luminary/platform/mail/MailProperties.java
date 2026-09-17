package com.luminary.platform.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Platform mail policy knobs.
 */
@ConfigurationProperties(prefix = "app.platform.mail")
public class MailProperties {

    private String from = "Luminary <noreply@luminary.dev>";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = Objects.requireNonNull(from, "from");
    }
}