/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.quest.erwin.di.powerbi.model.Dashboard;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles dashboard metadata extraction.
 */
public class DashboardMetadataHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardMetadataHandler.class);

    public void handle(Dashboard dashboard, Workspace workspace, MetadataResult result) {
        log.debug("Processing dashboard: {} ({}) with {} tile(s)",
                dashboard.getDisplayName(), dashboard.getId(),
                dashboard.getTiles() != null ? dashboard.getTiles().size() : 0);
        result.addDashboard(dashboard);
    }
}
