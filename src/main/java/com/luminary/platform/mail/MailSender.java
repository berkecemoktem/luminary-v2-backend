package com.luminary.platform.mail;

/**
 * Technical mail port owned by the platform module. Adapters are responsible
 * for transport only; they never contain domain policy.
 */
public interface MailSender {

    /**
     * Delivers a message. Implementations must not swallow errors silently;
     * the caller decides how a delivery failure is handled.
     */
    void send(MailMessage message);
}