package com.appvault.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TokenRequest {

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Environment is required")
    private String environment;

    private String region;

    /**
     * Token expiry in hours. If null, token never expires.
     */
    private Integer expiryHours;
}
