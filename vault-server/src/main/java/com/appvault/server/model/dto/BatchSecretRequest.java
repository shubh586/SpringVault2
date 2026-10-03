package com.appvault.server.model.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BatchSecretRequest {

    private String environment;

    /**
     * List of "path/key" strings to fetch in bulk
     */
    private List<String> paths;
}
