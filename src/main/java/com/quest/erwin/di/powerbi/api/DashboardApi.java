/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.quest.erwin.di.powerbi.client.PowerBIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API for dashboard-related operations.
 */
public class DashboardApi {

    private static final Logger log = LoggerFactory.getLogger(DashboardApi.class);

    private final PowerBIClient client;

    public DashboardApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Gets all dashboards from the admin API.
     */
    public JsonNode getAdminDashboards() {
        log.info("Fetching all dashboards via admin API");
        return client.getJson(PowerBIEndpoints.ADMIN_DASHBOARDS);
    }

    /**
     * Gets tiles for a specific dashboard.
     */
    public JsonNode getDashboardTiles(String dashboardId) {
        log.debug("Fetching tiles for dashboard={}", dashboardId);
        return client.getJson(PowerBIEndpoints.adminDashboardTiles(dashboardId));
    }
}
