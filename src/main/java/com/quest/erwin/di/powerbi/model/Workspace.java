/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Power BI workspace (group) from the scan result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "name", "type", "state"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace {

    private String id;
    private String name;
    private String type;
    private String state;
    private boolean isOnDedicatedCapacity;
    private String capacityId;
    private String defaultDatasetStorageFormat;

    @Builder.Default private List<Report> reports = new ArrayList<>();
    @Builder.Default private List<Dashboard> dashboards = new ArrayList<>();
    @Builder.Default private List<Dataset> datasets = new ArrayList<>();
    @Builder.Default private List<Dataflow> dataflows = new ArrayList<>();
    @Builder.Default private List<PowerBIUser> users = new ArrayList<>();
}
