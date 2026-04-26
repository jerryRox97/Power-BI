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
 * API for report-related operations.
 */
public class ReportApi {

    private static final Logger log = LoggerFactory.getLogger(ReportApi.class);

    private final PowerBIClient client;

    public ReportApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Gets all reports from the admin API.
     */
    public JsonNode getAdminReports() {
        log.info("Fetching all reports via admin API");
        return client.getJson(PowerBIEndpoints.ADMIN_REPORTS);
    }

    /**
     * Gets reports for a specific workspace.
     */
    public JsonNode getGroupReports(String groupId) {
        log.debug("Fetching reports for workspace={}", groupId);
        return client.getJson(PowerBIEndpoints.groupReports(groupId));
    }
}
