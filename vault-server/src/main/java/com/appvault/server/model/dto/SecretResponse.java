package com.appvault.server.model.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SecretResponse {

    private String path;
    private String key;
    private String value;
    private String environment;
    private String region;
    private String description;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
