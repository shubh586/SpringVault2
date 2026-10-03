package com.appvault.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps to the vault server's ApiResponse<T> format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;

    public VaultApiResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
