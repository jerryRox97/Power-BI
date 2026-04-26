/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.glossary;

import com.quest.erwin.di.powerbi.metadata.MetadataResult;
import com.quest.erwin.di.powerbi.model.Column;
import com.quest.erwin.di.powerbi.model.Dashboard;
import com.quest.erwin.di.powerbi.model.DataSource;
import com.quest.erwin.di.powerbi.model.Dataflow;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.Expression;
import com.quest.erwin.di.powerbi.model.Measure;
import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.Table;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds business glossary entries from extracted metadata.
 * Creates entries for all Power BI artifact types with their business context.
 */
public class GlossaryBuilder {

    private static final Logger log = LoggerFactory.getLogger(GlossaryBuilder.class);

    /**
     * Builds glossary entries from the metadata result.
     */
    public List<GlossaryEntry> build(MetadataResult metadata) {
        log.info("Building business glossary");
        List<GlossaryEntry> entries = new ArrayList<>();

        for (Workspace ws : metadata.getWorkspaces()) {
            entries.add(buildWorkspaceEntry(ws));
            addDatasetEntries(ws, metadata, entries);
            addReportEntries(ws, entries);
            addDashboardEntries(ws, entries);
            addDataflowEntries(ws, entries);
        }

        addDatasourceEntries(metadata, entries);

        log.info("Built {} glossary entries", entries.size());
        return entries;
    }

    private GlossaryEntry buildWorkspaceEntry(Workspace ws) {
        return GlossaryEntry.builder()
                .name(ws.getName())
                .description("Power BI workspace: " + ws.getName())
                .category(GlossaryEntry.Category.WORKSPACE)
                .sourceType("Workspace")
                .workspaceName(ws.getName())
                .workspaceId(ws.getId())
                .build();
    }

    private void addDatasetEntries(Workspace ws, MetadataResult metadata, List<GlossaryEntry> entries) {
        for (Dataset ds : ws.getDatasets()) {
            entries.add(GlossaryEntry.builder()
                    .name(ds.getName())
                    .description("Dataset in workspace '" + ws.getName() + "'")
                    .category(GlossaryEntry.Category.DATASET)
                    .sourceType(ds.getContentProviderType())
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .owner(ds.getConfiguredBy())
                    .build());

            addTableEntries(ds, ws, entries);
            addExpressionEntries(ds, ws, entries);
        }
    }

    private void addTableEntries(Dataset ds, Workspace ws, List<GlossaryEntry> entries) {
        if (ds.getTables() == null) return;
        for (Table table : ds.getTables()) {
            entries.add(GlossaryEntry.builder()
                    .name(table.getName())
                    .description("Table in dataset '" + ds.getName() + "'")
                    .category(GlossaryEntry.Category.TABLE)
                    .sourceType(table.getStorageMode())
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .parentName(ds.getName())
                    .parentId(ds.getId())
                    .build());

            addColumnEntries(table, ds, ws, entries);
            addMeasureEntries(table, ds, ws, entries);
        }
    }

    private void addColumnEntries(Table table, Dataset ds, Workspace ws, List<GlossaryEntry> entries) {
        if (table.getColumns() == null) return;
        for (Column col : table.getColumns()) {
            entries.add(GlossaryEntry.builder()
                    .name(col.getName())
                    .description(col.getDescription() != null ? col.getDescription() :
                            "Column in table '" + table.getName() + "'")
                    .category(GlossaryEntry.Category.COLUMN)
                    .sourceType(col.getColumnType())
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .parentName(table.getName())
                    .parentId(ds.getId())
                    .dataType(col.getDataType())
                    .build());
        }
    }

    private void addMeasureEntries(Table table, Dataset ds, Workspace ws, List<GlossaryEntry> entries) {
        if (table.getMeasures() == null) return;
        for (Measure m : table.getMeasures()) {
            entries.add(GlossaryEntry.builder()
                    .name(m.getName())
                    .description(m.getDescription() != null ? m.getDescription() :
                            "Measure in table '" + table.getName() + "'")
                    .category(GlossaryEntry.Category.MEASURE)
                    .sourceType("DAX")
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .parentName(table.getName())
                    .parentId(ds.getId())
                    .expression(m.getExpression())
                    .build());
        }
    }

    private void addExpressionEntries(Dataset ds, Workspace ws, List<GlossaryEntry> entries) {
        if (ds.getExpressions() == null) return;
        for (Expression expr : ds.getExpressions()) {
            entries.add(GlossaryEntry.builder()
                    .name(expr.getName())
                    .description(expr.getDescription() != null && !expr.getDescription().isEmpty()
                            ? expr.getDescription() : "Expression in dataset '" + ds.getName() + "'")
                    .category(GlossaryEntry.Category.EXPRESSION)
                    .sourceType("M/PowerQuery")
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .parentName(ds.getName())
                    .parentId(ds.getId())
                    .expression(expr.getExpression())
                    .build());
        }
    }

    private void addReportEntries(Workspace ws, List<GlossaryEntry> entries) {
        for (Report r : ws.getReports()) {
            entries.add(GlossaryEntry.builder()
                    .name(r.getName())
                    .description("Report in workspace '" + ws.getName() + "'")
                    .category(GlossaryEntry.Category.REPORT)
                    .sourceType(r.getReportType())
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .parentName(r.getDatasetId())
                    .owner(r.getCreatedBy())
                    .build());
        }
    }

    private void addDashboardEntries(Workspace ws, List<GlossaryEntry> entries) {
        for (Dashboard d : ws.getDashboards()) {
            entries.add(GlossaryEntry.builder()
                    .name(d.getDisplayName())
                    .description("Dashboard in workspace '" + ws.getName() + "'")
                    .category(GlossaryEntry.Category.DASHBOARD)
                    .sourceType("Dashboard")
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .additionalProperties("tiles=" +
                            (d.getTiles() != null ? d.getTiles().size() : 0))
                    .build());
        }
    }

    private void addDataflowEntries(Workspace ws, List<GlossaryEntry> entries) {
        for (Dataflow df : ws.getDataflows()) {
            entries.add(GlossaryEntry.builder()
                    .name(df.getName())
                    .description(df.getDescription() != null && !df.getDescription().isEmpty()
                            ? df.getDescription() : "Dataflow in workspace '" + ws.getName() + "'")
                    .category(GlossaryEntry.Category.DATAFLOW)
                    .sourceType("Dataflow")
                    .workspaceName(ws.getName())
                    .workspaceId(ws.getId())
                    .owner(df.getConfiguredBy())
                    .build());
        }
    }

    private void addDatasourceEntries(MetadataResult metadata, List<GlossaryEntry> entries) {
        for (DataSource ds : metadata.getDatasourceMap().values()) {
            String connInfo = buildConnectionInfo(ds);
            entries.add(GlossaryEntry.builder()
                    .name(ds.getDatasourceType() + "_" + ds.getDatasourceId().substring(0, 8))
                    .description("Data source of type '" + ds.getDatasourceType() + "'")
                    .category(GlossaryEntry.Category.DATASOURCE)
                    .sourceType(ds.getDatasourceType())
                    .additionalProperties(connInfo)
                    .build());
        }
    }

    private String buildConnectionInfo(DataSource ds) {
        StringBuilder sb = new StringBuilder();
        String server = ds.getServer();
        if (!server.isEmpty()) sb.append("server=").append(server);
        String database = ds.getDatabase();
        if (!database.isEmpty()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("database=").append(database);
        }
        String url = ds.getUrl();
        if (!url.isEmpty()) sb.append("url=").append(url);
        String path = ds.getPath();
        if (!path.isEmpty()) sb.append("path=").append(path);
        return sb.toString();
    }
}
