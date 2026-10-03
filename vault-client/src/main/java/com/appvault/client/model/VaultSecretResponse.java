package com.appvault.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps to the vault server's SecretResponse DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultSecretResponse {

    private String path;
    private String key;
    private String value;
    private String environment;
    private String region;
    private String description;
    private Integer version;

    public VaultSecretResponse() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
