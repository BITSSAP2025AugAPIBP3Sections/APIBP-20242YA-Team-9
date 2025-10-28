package com.example.Job_Portal.dto;

import com.example.Job_Portal.enums.ApplicationStatus;

public class ApplicationStatusDTO {
    private ApplicationStatus status;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
} 