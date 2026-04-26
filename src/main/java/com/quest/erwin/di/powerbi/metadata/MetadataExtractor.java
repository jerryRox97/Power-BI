/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.quest.erwin.di.powerbi.model.Dashboard;
import com.quest.erwin.di.powerbi.model.DataSource;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.Dataflow;
import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.ScanResultResponse;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Facade for extracting metadata from scan results.
 * Delegates to type-specific handlers for datasets, reports, dashboards, and dataflows.
 */
public class MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(MetadataExtractor.class);

    private final DatasetMetadataHandler datasetHandler;
    private final ReportMetadataHandler reportHandler;
    private final DashboardMetadataHandler dashboardHandler;
    private final DataflowMetadataHandler dataflowHandler;

    public MetadataExtractor() {
        this.datasetHandler = new DatasetMetadataHandler();
        this.reportHandler = new ReportMetadataHandler();
        this.dashboardHandler = new DashboardMetadataHandler();
        this.dataflowHandler = new DataflowMetadataHandler();
    }

    /**
     * Extracts all metadata from the scan result into a structured MetadataResult.
     */
    public MetadataResult extract(ScanResultResponse scanResult) {
        log.info("Extracting metadata from {} workspace(s)", scanResult.getWorkspaces().size());

        MetadataResult result = new MetadataResult();

        Map<String, DataSource> datasourceMap = buildDatasourceMap(scanResult);
        result.setDatasourceMap(datasourceMap);

        for (Workspace workspace : scanResult.getWorkspaces()) {
            log.info("Processing workspace: {} ({})", workspace.getName(), workspace.getId());
            result.addWorkspace(workspace);

            for (Dataset dataset : workspace.getDatasets()) {
                dataset.setWorkspaceId(workspace.getId());
                datasetHandler.handle(dataset, workspace, result);
            }

            for (Report report : workspace.getReports()) {
                report.setWorkspaceId(workspace.getId());
                reportHandler.handle(report, workspace, result);
            }

            for (Dashboard dashboard : workspace.getDashboards()) {
                dashboardHandler.handle(dashboard, workspace, result);
            }

            for (Dataflow dataflow : workspace.getDataflows()) {
                dataflow.setWorkspaceId(workspace.getId());
                dataflowHandler.handle(dataflow, workspace, result);
            }
        }

        log.info("Metadata extraction complete: {} datasets, {} reports, {} dashboards, {} dataflows",
                result.getDatasetCount(), result.getReportCount(),
                result.getDashboardCount(), result.getDataflowCount());

        return result;
    }

    private Map<String, DataSource> buildDatasourceMap(ScanResultResponse scanResult) {
        Map<String, DataSource> map = new HashMap<>();
        if (scanResult.getDatasourceInstances() != null) {
            for (DataSource ds : scanResult.getDatasourceInstances()) {
                map.put(ds.getDatasourceId(), ds);
            }
        }
        log.info("Built datasource map with {} entries", map.size());
        return map;
    }
}
