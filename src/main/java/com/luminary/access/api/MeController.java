package com.luminary.access.api;

import com.luminary.access.api.dto.ActiveWorkspaceRequest;
import com.luminary.access.api.dto.MeResponse;
import com.luminary.access.application.WorkspaceService;
import com.luminary.access.application.WorkspaceView;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import com.luminary.shared.query.PageResponse;
import com.luminary.shared.query.SearchRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Identity and workspace endpoints ({@code GET /me},
 * {@code POST /me/workspaces},
 * {@code POST /me/active-workspace}). The active workspace always comes from
 * the server session, never from the request.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final CurrentUser currentUser;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    public MeController(CurrentUser currentUser,
                        WorkspaceService workspaceService,
                        UserRepository userRepository) {
        this.currentUser = currentUser;
        this.workspaceService = workspaceService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public MeResponse me() {
        UserId userId = currentUser.require();
        UserEntity user = userRepository.findById(userId.value())
                .orElseThrow(() -> ApiException.unauthorized(
                        "user-not-found", "Account not found."));
        List<WorkspaceView> workspaces = workspaceService.workspaces(userId);
        TenantId active = workspaceService.currentOrEmpty(userId).orElse(null);
        return new MeResponse(
                new MeResponse.MeUser(user.getEmail(), user.getDisplayName()),
                active, workspaces);
    }

    @PostMapping("/workspaces")
    public PageResponse<WorkspaceView> searchWorkspaces(
            @Valid @RequestBody SearchRequest request) {
        return workspaceService.searchWorkspaces(
                currentUser.require(), request);
    }

    @PostMapping("/active-workspace")
    public WorkspaceView setActiveWorkspace(
            @RequestBody ActiveWorkspaceRequest body) {
        TenantId target = body.tenantId();
        return workspaceService.switchTo(currentUser.require(), target);
    }
}
