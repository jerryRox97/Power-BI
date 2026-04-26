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
 * Represents a Power BI dataset from the scan result.
 * Contains tables (with columns/measures), expressions, and datasource usages for lineage.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "name", "contentProviderType"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Dataset {

    private String id;
    private String name;
    private String configuredBy;
    private String configuredById;
    private boolean isEffectiveIdentityRequired;
    private boolean isEffectiveIdentityRolesRequired;
    private String targetStorageMode;
    private String createdDate;
    private String contentProviderType;
    private String schemaRetrievalError;
    private String workspaceId;

    @Builder.Default private List<Table> tables = new ArrayList<>();
    @Builder.Default private List<Expression> expressions = new ArrayList<>();
    @Builder.Default private List<DatasourceUsage> datasourceUsages = new ArrayList<>();
    @Builder.Default private List<UpstreamDataset> upstreamDatasets = new ArrayList<>();
    @Builder.Default private List<UpstreamDataflow> upstreamDataflows = new ArrayList<>();
    @Builder.Default private List<PowerBIUser> users = new ArrayList<>();
}
