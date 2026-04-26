/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.quest.erwin.di.powerbi.client.ApiException;
import com.quest.erwin.di.powerbi.client.PowerBIClient;
import com.quest.erwin.di.powerbi.model.ScanInfo;
import com.quest.erwin.di.powerbi.model.ScanResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API for tenant workspace scanning (getInfo, scanStatus, scanResult).
 * This is the primary mechanism for extracting metadata with lineage and datasource details.
 */
public class ScanApi {

    private static final Logger log = LoggerFactory.getLogger(ScanApi.class);
    private static final long POLL_INTERVAL_MS = 5_000L;
    private static final int MAX_POLL_ATTEMPTS = 120; // 10 minutes max

    private final PowerBIClient client;

    public ScanApi(PowerBIClient client) {
        this.client = client;
    }

    /**
     * Initiates a workspace scan for the given workspace IDs.
     * Calls POST admin/workspaces/getInfo with lineage=true, datasourceDetails=true, etc.
     */
    public ScanInfo initiateScan(List<String> workspaceIds) {
        String body = buildScanRequestBody(workspaceIds);
        log.info("Initiating scan for {} workspace(s)", workspaceIds.size());

        return client.post(PowerBIEndpoints.WORKSPACE_GET_INFO, body, responseBody -> {
            try {
                return client.getObjectMapper().readValue(responseBody, ScanInfo.class);
            } catch (Exception e) {
                throw new ApiException(0, "ParseError", "Failed to parse scan initiation response", e);
            }
        });
    }

    /**
     * Checks the scan status for the given scan ID.
     */
    public ScanInfo checkScanStatus(String scanId) {
        return client.getAs(PowerBIEndpoints.scanStatus(scanId), ScanInfo.class);
    }

    /**
     * Retrieves the full scan result with all workspace metadata, lineage, and datasources.
     */
    public ScanResultResponse getScanResult(String scanId) {
        log.info("Retrieving scan result for scanId={}", scanId);
        return client.getAs(PowerBIEndpoints.scanResult(scanId), ScanResultResponse.class);
    }

    /**
     * Initiates a scan and waits for it to complete, then returns the full result.
     */
    public ScanResultResponse scanAndWait(List<String> workspaceIds) {
        ScanInfo scanInfo = initiateScan(workspaceIds);
        String scanId = scanInfo.getId();
        log.info("Scan initiated: scanId={}, waiting for completion", scanId);

        ScanInfo status = waitForScanCompletion(scanId);

        if (status.isFailed()) {
            throw new ApiException(0, "ScanFailed", "Workspace scan failed: scanId=" + scanId);
        }

        return getScanResult(scanId);
    }

    private ScanInfo waitForScanCompletion(String scanId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            ScanInfo status = checkScanStatus(scanId);
            if (status.isSucceeded() || status.isFailed()) {
                log.info("Scan completed: scanId={}, status={}", scanId, status.getStatus());
                return status;
            }
            log.debug("Scan still running: scanId={}, attempt={}", scanId, attempt + 1);
            sleep(POLL_INTERVAL_MS);
        }
        throw new ApiException(0, "ScanTimeout",
                "Scan did not complete within timeout: scanId=" + scanId);
    }

    private String buildScanRequestBody(List<String> workspaceIds) {
        String idsJson = workspaceIds.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        return "{\"workspaces\":[" + idsJson + "]}";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Interrupted", "Scan polling interrupted");
        }
    }
}
