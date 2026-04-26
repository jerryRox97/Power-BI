/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response from scan initiation and status check APIs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanInfo {

    private String id;
    private String createdDateTime;
    private String status;

    public boolean isSucceeded() {
        return "Succeeded".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "Failed".equalsIgnoreCase(status);
    }

    public boolean isRunning() {
        return "Running".equalsIgnoreCase(status) || "NotStarted".equalsIgnoreCase(status);
    }
}
