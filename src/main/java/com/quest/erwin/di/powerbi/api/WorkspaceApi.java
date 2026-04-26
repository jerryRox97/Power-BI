/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.quest.erwin.di.powerbi.client.PowerBIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * API for workspace-related operations.
 */
public class WorkspaceApi {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceApi.class);

    private final PowerBIClient client;

    public WorkspaceApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Gets all modified workspace IDs from the tenant.
     * Uses the admin/workspaces/modified endpoint.
     */
    public List<String> getModifiedWorkspaceIds() {
        log.info("Fetching modified workspace IDs");
        JsonNode response = client.getJson(PowerBIEndpoints.WORKSPACES_MODIFIED);
        return extractIds(response);
    }

    /**
     * Gets workspace IDs modified since a given timestamp (ISO 8601).
     */
    public List<String> getModifiedWorkspaceIdsSince(String modifiedSince) {
        log.info("Fetching workspace IDs modified since {}", modifiedSince);
        JsonNode response = client.getJson(PowerBIEndpoints.workspacesModifiedSince(modifiedSince));
        return extractIds(response);
    }

    private List<String> extractIds(JsonNode node) {
        List<String> ids = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.has("id")) {
                    ids.add(item.get("id").asText());
                }
            }
        }
        log.info("Found {} workspace(s)", ids.size());
        return ids;
    }
}
