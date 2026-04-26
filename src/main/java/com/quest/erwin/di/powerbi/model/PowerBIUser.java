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
 * Represents a Power BI user with access rights.
 * Used for workspace, dataset, report, and dashboard users.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"displayName", "emailAddress", "principalType"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerBIUser {

    private String groupUserAccessRight;
    private String datasetUserAccessRight;
    private String reportUserAccessRight;
    private String dashboardUserAccessRight;

    private String emailAddress;
    private String displayName;
    private String identifier;
    private String graphId;
    private String principalType;
    private String userType;

    /**
     * Returns the effective access right regardless of which field is populated.
     */
    public String getAccessRight() {
        if (groupUserAccessRight != null) return groupUserAccessRight;
        if (datasetUserAccessRight != null) return datasetUserAccessRight;
        if (reportUserAccessRight != null) return reportUserAccessRight;
        if (dashboardUserAccessRight != null) return dashboardUserAccessRight;
        return "";
    }
}
