/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.scan;

import com.quest.erwin.di.powerbi.api.ScanApi;
import com.quest.erwin.di.powerbi.api.WorkspaceApi;
import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import com.quest.erwin.di.powerbi.model.ScanResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Orchestrates the full tenant scan workflow:
 * 1. Fetch modified workspace IDs (full or incremental)
 * 2. Batch-scan workspaces via getInfo API
 * 3. Return aggregated scan results
 */
public class TenantScanner {

    private static final Logger log = LoggerFactory.getLogger(TenantScanner.class);

    private final WorkspaceApi workspaceApi;
    private final ScanBatcher scanBatcher;
    private final ConnectorConfig config;

    public TenantScanner(WorkspaceApi workspaceApi, ScanApi scanApi, ConnectorConfig config) {
        this.workspaceApi = workspaceApi;
        this.scanBatcher = new ScanBatcher(scanApi, config);
        this.config = config;
    }

    /**
     * Scans the entire tenant or only workspaces modified since last run.
     */
    public ScanResultResponse scan() {
        List<String> workspaceIds = fetchWorkspaceIds();

        if (workspaceIds.isEmpty()) {
            log.info("No workspaces to scan");
            return new ScanResultResponse();
        }

        return scanBatcher.scanAll(workspaceIds);
    }

    private List<String> fetchWorkspaceIds() {
        if (config.isIncrementalLoad() && !config.getLastRunTime().isEmpty()) {
            log.info("Incremental load: fetching workspaces modified since {}", config.getLastRunTime());
            return workspaceApi.getModifiedWorkspaceIdsSince(config.getLastRunTime());
        }

        log.info("Full load: fetching all modified workspaces");
        return workspaceApi.getModifiedWorkspaceIds();
    }
}
