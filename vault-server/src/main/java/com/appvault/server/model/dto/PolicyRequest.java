package com.appvault.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String name;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String environment;

    private String region;

    @NotBlank(message = "Allowed paths are required")
    private String allowedPaths;

    private String effect; // ALLOW or DENY
}
