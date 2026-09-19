package com.luminary.access.api;

import com.luminary.access.application.SchoolNotificationService;
import com.luminary.access.application.SchoolNotificationView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * School notification endpoints for institution admins. Tenant resolution
 * uses the active workspace from the server session.
 */
@RestController
@RequestMapping("/api/v1/institution/notifications")
public class SchoolNotificationController {

    private final CurrentUser currentUser;
    private final SchoolNotificationService notificationService;

    public SchoolNotificationController(CurrentUser currentUser,
                                        SchoolNotificationService notificationService) {
        this.currentUser = currentUser;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<SchoolNotificationView> list() {
        return notificationService.listForTenant(
                currentUser.require().value(), currentUser.requireTenant());
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID notificationId) {
        notificationService.markRead(currentUser.require().value(),
                currentUser.requireTenant(), notificationId);
        return ResponseEntity.noContent().build();
    }
}