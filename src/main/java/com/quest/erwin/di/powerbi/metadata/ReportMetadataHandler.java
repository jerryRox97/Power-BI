/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles report metadata extraction.
 */
public class ReportMetadataHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportMetadataHandler.class);

    public void handle(Report report, Workspace workspace, MetadataResult result) {
        log.debug("Processing report: {} ({}) -> dataset: {}",
                report.getName(), report.getId(), report.getDatasetId());
        result.addReport(report);
    }
}
