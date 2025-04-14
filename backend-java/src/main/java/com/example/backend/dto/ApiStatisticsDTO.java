package com.example.backend.dto;

import java.time.LocalDateTime;

public class ApiStatisticsDTO {
    private String apiEndpoint;
    private String method;
    private String userId;
    private LocalDateTime timestamp;
    private String status;
    private long processingTimeMs;
    private String requestData;

    public ApiStatisticsDTO() {
    }

    public ApiStatisticsDTO(String apiEndpoint, String method, String userId, String status, long processingTimeMs, String requestData) {
        this.apiEndpoint = apiEndpoint;
        this.method = method;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.processingTimeMs = processingTimeMs;
        this.requestData = requestData;
    }

    // Getters and Setters
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    @Override
    public String toString() {
        return "ApiStatisticsDTO{" +
                "apiEndpoint='" + apiEndpoint + '\'' +
                ", method='" + method + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                ", requestData='" + requestData + '\'' +
                '}';
    }
} 