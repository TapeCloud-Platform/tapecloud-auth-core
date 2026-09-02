package com.tapecloud.sso.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Backend TapeCloud SSO funcionando";
    }

    @GetMapping("/version")
    public String version() {
        return "v0.0.1-SNAPSHOT";
    }
}
