package org.shark.renovatio.api.controller;

import org.shark.renovatio.application.capability.SurfaceCapabilityRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CapabilitiesController {
    private final SurfaceCapabilityRegistry registry = new SurfaceCapabilityRegistry();

    @GetMapping("/api/v1/capabilities")
    public Map<String, Object> capabilities() {
        return registry.asMap();
    }
}
