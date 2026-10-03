package com.appvault.server.model.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TokenResponse {
    private String token;
    private String serviceName;
    private String environment;
    private String region;
    private LocalDateTime expiresAt;
}
