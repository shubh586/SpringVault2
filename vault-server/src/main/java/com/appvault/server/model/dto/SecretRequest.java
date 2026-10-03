package com.appvault.server.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SecretRequest {

    @NotBlank(message = "Path is required")
    private String path;

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Value is required")
    private String value;

    @NotBlank(message = "Environment is required")
    private String environment;

    private String region;

    private String description;
}
