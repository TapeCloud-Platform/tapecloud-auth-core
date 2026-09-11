package com.tapecloud.auth.api;

import com.tapecloud.auth.service.AdminService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return adminService.listAllUsers();
    }

    @PostMapping("/users/{userId}/grant-admin")
    public Map<String, Object> grantAdmin(@PathVariable UUID userId) {
        return adminService.grantAdminRole(userId);
    }

    @PostMapping("/users/{userId}/revoke-admin")
    public Map<String, Object> revokeAdmin(@PathVariable UUID userId) {
        return adminService.revokeAdminRole(userId);
    }
}
