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
import com.quest.erwin.di.powerbi.model.Workspace;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated metadata result from extraction.
 * Contains all workspaces and their artifacts, plus lookup maps.
 */
@Getter
@Setter
public class MetadataResult {

    private final List<Workspace> workspaces = new ArrayList<>();
    private final List<Dataset> datasets = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private final List<Dashboard> dashboards = new ArrayList<>();
    private final List<Dataflow> dataflows = new ArrayList<>();

    private Map<String, DataSource> datasourceMap = new HashMap<>();
    private final Map<String, Dataset> datasetMap = new HashMap<>();
    private final Map<String, Report> reportMap = new HashMap<>();
    private final Map<String, Dashboard> dashboardMap = new HashMap<>();
    private final Map<String, Dataflow> dataflowMap = new HashMap<>();
    private final Map<String, Workspace> workspaceMap = new HashMap<>();

    public void addWorkspace(Workspace workspace) {
        workspaces.add(workspace);
        workspaceMap.put(workspace.getId(), workspace);
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
        datasetMap.put(dataset.getId(), dataset);
    }

    public void addReport(Report report) {
        reports.add(report);
        reportMap.put(report.getId(), report);
    }

    public void addDashboard(Dashboard dashboard) {
        dashboards.add(dashboard);
        dashboardMap.put(dashboard.getId(), dashboard);
    }

    public void addDataflow(Dataflow dataflow) {
        dataflows.add(dataflow);
        dataflowMap.put(dataflow.getObjectId(), dataflow);
    }

    public int getDatasetCount() { return datasets.size(); }
    public int getReportCount() { return reports.size(); }
    public int getDashboardCount() { return dashboards.size(); }
    public int getDataflowCount() { return dataflows.size(); }
}
