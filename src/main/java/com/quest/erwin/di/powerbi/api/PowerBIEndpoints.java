/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.api;

/**
 * Centralized constants for all Power BI REST API endpoint URLs.
 * Based on https://learn.microsoft.com/en-us/rest/api/power-bi/admin
 */
public final class PowerBIEndpoints {

    private PowerBIEndpoints() {}

    private static final String BASE = "https://api.powerbi.com/v1.0/myorg";
    private static final String ADMIN = BASE + "/admin";

    // Admin - Workspaces
    public static final String WORKSPACES_MODIFIED =
            ADMIN + "/workspaces/modified?excludePersonalWorkspaces=true&excludeInActiveWorkspaces=true";

    public static String workspacesModifiedSince(String modifiedSince) {
        return WORKSPACES_MODIFIED + "&modifiedSince=" + modifiedSince;
    }

    // Admin - Workspace Info (Tenant Scan)
    public static final String WORKSPACE_GET_INFO =
            ADMIN + "/workspaces/getInfo?lineage=true&datasourceDetails=true&datasetSchema=true&datasetExpressions=true&getArtifactUsers=true";

    public static String scanStatus(String scanId) {
        return ADMIN + "/workspaces/scanStatus/" + scanId;
    }

    public static String scanResult(String scanId) {
        return ADMIN + "/workspaces/scanResult/" + scanId;
    }

    // Admin - Datasets
    public static final String ADMIN_DATASETS = ADMIN + "/datasets";

    public static String adminDatasetDatasources(String datasetId) {
        return ADMIN + "/datasets/" + datasetId + "/datasources";
    }

    // Admin - Reports
    public static final String ADMIN_REPORTS = ADMIN + "/reports";

    // Admin - Dashboards
    public static final String ADMIN_DASHBOARDS = ADMIN + "/dashboards";

    public static String adminDashboardTiles(String dashboardId) {
        return ADMIN + "/dashboards/" + dashboardId + "/tiles";
    }

    // Admin - Dataflows
    public static final String ADMIN_DATAFLOWS = ADMIN + "/dataflows";

    public static String adminDataflowDatasources(String dataflowId) {
        return ADMIN + "/dataflows/" + dataflowId + "/datasources";
    }

    public static String adminDataflowUpstream(String dataflowId) {
        return ADMIN + "/dataflows/" + dataflowId + "/upstreamDataflows";
    }

    // Groups (workspace-level, non-admin)
    public static String groupDatasets(String groupId) {
        return BASE + "/groups/" + groupId + "/datasets";
    }

    public static String groupReports(String groupId) {
        return BASE + "/groups/" + groupId + "/reports";
    }

    public static String groupDashboards(String groupId) {
        return BASE + "/groups/" + groupId + "/dashboards";
    }

    public static String groupDataflows(String groupId) {
        return BASE + "/groups/" + groupId + "/dataflows";
    }

    public static String datasetRefreshSchedule(String groupId, String datasetId) {
        return BASE + "/groups/" + groupId + "/datasets/" + datasetId + "/refreshSchedule";
    }
}
