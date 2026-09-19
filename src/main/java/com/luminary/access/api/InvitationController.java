package com.luminary.access.api;

import com.luminary.access.api.dto.AcceptInvitationRequest;
import com.luminary.access.api.dto.CreateInvitationRequest;
import com.luminary.access.api.dto.ResendInvitationRequest;
import com.luminary.access.application.InvitationCreated;
import com.luminary.access.application.InvitationService;
import com.luminary.access.application.InvitedWorkspace;
import com.luminary.access.application.PendingInvitation;
import com.luminary.shared.identity.InvitationId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Invitation endpoints. Tenant resolution on creation and resend uses the
 * active workspace from the server session. The creation response never
 * contains the raw token; it is delivered to the invited student by email.
 */
@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final CurrentUser currentUser;
    private final InvitationService invitationService;

    public InvitationController(CurrentUser currentUser,
                                InvitationService invitationService) {
        this.currentUser = currentUser;
        this.invitationService = invitationService;
    }

    @PostMapping
    public InvitationCreated create(@RequestBody CreateInvitationRequest body) {
        return invitationService.create(
                currentUser.require().value(),
                currentUser.requireTenant(),
                body.email(), body.role());
    }

    @PostMapping("/resend")
    public InvitationCreated resend(
            @RequestBody ResendInvitationRequest body) {
        return invitationService.resend(
                currentUser.require().value(),
                currentUser.requireTenant(),
                body.email());
    }

    @PostMapping("/accept")
    public InvitedWorkspace accept(@RequestBody AcceptInvitationRequest body) {
        return invitationService.accept(currentUser.require().value(),
                body.token());
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID invitationId) {
        invitationService.reject(currentUser.require().value(),
                new InvitationId(invitationId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public List<PendingInvitation> pending() {
        return invitationService.pendingFor(currentUser.require().value());
    }
}