package com.luminary.access.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Access module policy knobs.
 */
@ConfigurationProperties(prefix = "app.access")
public class AccessAppProperties {

    private Duration invitationTtl = Duration.ofDays(7);

    private String invitationAcceptUrlTemplate =
            "http://localhost:4200/invitations/accept?token={token}";

    private final DevFirstAdmin devFirstAdmin = new DevFirstAdmin();

    public Duration getInvitationTtl() {
        return invitationTtl;
    }

    public void setInvitationTtl(Duration invitationTtl) {
        this.invitationTtl = invitationTtl;
    }

    public String getInvitationAcceptUrlTemplate() {
        return invitationAcceptUrlTemplate;
    }

    public void setInvitationAcceptUrlTemplate(String invitationAcceptUrlTemplate) {
        this.invitationAcceptUrlTemplate = invitationAcceptUrlTemplate;
    }

    public DevFirstAdmin getDevFirstAdmin() {
        return devFirstAdmin;
    }

    /**
     * Controlled first-admin provisioning (E03-09), usable only in local
     * development. No global superadmin exists.
     */
    public static class DevFirstAdmin {

        private boolean enabled = false;

        private Set<String> emailAllowlist = new HashSet<>();

        private String institutionName = "Luminary Demo";

        private String timeZone = "UTC";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Set<String> getEmailAllowlist() {
            return emailAllowlist;
        }

        public void setEmailAllowlist(Set<String> emailAllowlist) {
            this.emailAllowlist = emailAllowlist;
        }

        public String getInstitutionName() {
            return institutionName;
        }

        public void setInstitutionName(String institutionName) {
            this.institutionName = institutionName;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }
    }
}