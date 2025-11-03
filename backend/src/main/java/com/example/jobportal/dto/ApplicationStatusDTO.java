package com.example.jobportal.dto;

import com.example.jobportal.enums.ApplicationStatus;

public class ApplicationStatusDTO {
    private ApplicationStatus status;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
} 