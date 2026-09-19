package com.luminary.student.api;

import com.luminary.access.api.CurrentUser;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import com.luminary.shared.query.ListQuery;
import com.luminary.shared.query.Page;
import com.luminary.student.application.StudentDetailsView;
import com.luminary.student.application.StudentDirectoryService;
import com.luminary.student.application.StudentListItemView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Institution-facing student directory endpoints: a paged list of the
 * tenant's students and a single-student details view. Both require an
 * institution admin role in the active workspace.
 */
@RestController
@RequestMapping("/api/v1/institution/students")
public class InstitutionStudentsController {

    private final CurrentUser currentUser;
    private final StudentDirectoryService directoryService;

    public InstitutionStudentsController(CurrentUser currentUser,
                                         StudentDirectoryService directoryService) {
        this.currentUser = currentUser;
        this.directoryService = directoryService;
    }

    @PostMapping
    public Page<StudentListItemView> list(
            @RequestBody ListQuery query) {
        UserId actor = currentUser.require();
        TenantId tenant = currentUser.requireTenant();
        return directoryService.list(actor.value(), tenant, query);
    }

    @GetMapping("/{userId}")
    public StudentDetailsView details(@PathVariable String userId) {
        UserId actor = currentUser.require();
        UserId target = UserId.fromString(userId);
        return directoryService.details(
                actor.value(), currentUser.requireTenant(), target);
    }
}