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
 * API for dataflow-related operations.
 */
public class DataflowApi {

    private static final Logger log = LoggerFactory.getLogger(DataflowApi.class);

    private final PowerBIClient client;

    public DataflowApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Gets all dataflows from the admin API.
     */
    public JsonNode getAdminDataflows() {
        log.info("Fetching all dataflows via admin API");
        return client.getJson(PowerBIEndpoints.ADMIN_DATAFLOWS);
    }

    /**
     * Gets datasources for a specific dataflow.
     */
    public JsonNode getDataflowDatasources(String dataflowId) {
        log.debug("Fetching datasources for dataflow={}", dataflowId);
        return client.getJson(PowerBIEndpoints.adminDataflowDatasources(dataflowId));
    }

    /**
     * Gets upstream dataflows for a specific dataflow.
     */
    public JsonNode getUpstreamDataflows(String dataflowId) {
        log.debug("Fetching upstream dataflows for dataflow={}", dataflowId);
        return client.getJson(PowerBIEndpoints.adminDataflowUpstream(dataflowId));
    }
}
