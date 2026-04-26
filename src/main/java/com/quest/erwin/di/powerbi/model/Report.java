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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Power BI report from the scan result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "name", "reportType", "datasetId"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Report {

    private String id;
    private String name;
    private String reportType;
    private String format;
    private String datasetId;
    private String createdDateTime;
    private String modifiedDateTime;
    private String modifiedBy;
    private String createdBy;
    private String modifiedById;
    private String createdById;
    private String webUrl;
    private String embedUrl;
    private String description;
    private String workspaceId;

    @Builder.Default private List<PowerBIUser> users = new ArrayList<>();
}
