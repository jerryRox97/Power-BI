/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.quest.erwin.di.powerbi.client.PowerBIClient;
import com.quest.erwin.di.powerbi.model.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * API for dataset-related operations.
 */
public class DatasetApi {

    private static final Logger log = LoggerFactory.getLogger(DatasetApi.class);

    private final PowerBIClient client;

    public DatasetApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Gets datasources for a specific dataset via admin API.
     */
    public List<DataSource> getDatasetDatasources(String datasetId) {
        log.debug("Fetching datasources for dataset={}", datasetId);
        JsonNode response = client.getJson(PowerBIEndpoints.adminDatasetDatasources(datasetId));
        return parseDataSources(response);
    }

    /**
     * Gets the refresh schedule for a dataset within a workspace.
     */
    public JsonNode getRefreshSchedule(String groupId, String datasetId) {
        log.debug("Fetching refresh schedule for dataset={} in workspace={}", datasetId, groupId);
        return client.getJson(PowerBIEndpoints.datasetRefreshSchedule(groupId, datasetId));
    }

    private List<DataSource> parseDataSources(JsonNode node) {
        List<DataSource> sources = new ArrayList<>();
        JsonNode valueArray = node != null && node.has("value") ? node.get("value") : node;
        if (valueArray != null && valueArray.isArray()) {
            for (JsonNode item : valueArray) {
                try {
                    DataSource ds = client.getObjectMapper().treeToValue(item, DataSource.class);
                    sources.add(ds);
                } catch (Exception e) {
                    log.warn("Failed to parse datasource: {}", e.getMessage());
                }
            }
        }
        return sources;
    }
}
