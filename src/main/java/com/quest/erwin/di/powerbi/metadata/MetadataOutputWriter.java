/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quest.erwin.di.powerbi.model.Column;
import com.quest.erwin.di.powerbi.model.Dashboard;
import com.quest.erwin.di.powerbi.model.Dataflow;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.Expression;
import com.quest.erwin.di.powerbi.model.Measure;
import com.quest.erwin.di.powerbi.model.PowerBIUser;
import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.Table;
import com.quest.erwin.di.powerbi.model.TableSource;
import com.quest.erwin.di.powerbi.model.Tile;
import com.quest.erwin.di.powerbi.model.Workspace;
import com.quest.erwin.di.powerbi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Writes extracted metadata to JSON files for ACP consumption.
 * Produces workspace-level metadata files with datasets, reports, dashboards, and dataflows.
 */
public class MetadataOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(MetadataOutputWriter.class);

    private final File outputDir;

    public MetadataOutputWriter(String outputDirectory) {
        this.outputDir = new File(outputDirectory, "metadata");
        this.outputDir.mkdirs();
    }

    /**
     * Writes all metadata for each workspace into separate JSON files.
     */
    public void write(MetadataResult result) throws IOException {
        for (Workspace workspace : result.getWorkspaces()) {
            writeWorkspaceMetadata(workspace);
        }
        writeSummary(result);
        log.info("Metadata written to {}", outputDir.getAbsolutePath());
    }

    private void writeWorkspaceMetadata(Workspace workspace) throws IOException {
        ObjectNode root = JsonUtil.createObject();
        root.put("workspaceId", workspace.getId());
        root.put("workspaceName", workspace.getName());
        root.put("type", workspace.getType());
        root.put("state", workspace.getState());
        root.put("isOnDedicatedCapacity", workspace.isOnDedicatedCapacity());

        root.set("datasets", buildDatasetArray(workspace));
        root.set("reports", buildReportArray(workspace));
        root.set("dashboards", buildDashboardArray(workspace));
        root.set("dataflows", buildDataflowArray(workspace));
        root.set("users", buildUserArray(workspace));

        String filename = sanitize(workspace.getName()) + "_metadata.json";
        JsonUtil.writeToFile(root, new File(outputDir, filename));
        log.debug("Wrote metadata for workspace: {}", workspace.getName());
    }

    private ArrayNode buildDatasetArray(Workspace workspace) {
        ArrayNode arr = JsonUtil.createArray();
        for (Dataset ds : workspace.getDatasets()) {
            ObjectNode node = JsonUtil.createObject();
            node.put("id", ds.getId());
            node.put("name", ds.getName());
            node.put("configuredBy", ds.getConfiguredBy());
            node.put("targetStorageMode", ds.getTargetStorageMode());
            node.put("createdDate", ds.getCreatedDate());
            node.put("contentProviderType", ds.getContentProviderType());

            ArrayNode tables = JsonUtil.createArray();
            if (ds.getTables() != null) {
                for (Table t : ds.getTables()) {
                    ObjectNode tNode = JsonUtil.createObject();
                    tNode.put("name", t.getName());
                    tNode.put("isHidden", t.isHidden());
                    tNode.put("storageMode", t.getStorageMode());

                    tNode.set("columns", buildColumnArray(t));
                    tNode.set("measures", buildMeasureArray(t));
                    tNode.set("source", buildTableSourceArray(t));
                    tables.add(tNode);
                }
            }
            node.set("tables", tables);

            ArrayNode expressions = JsonUtil.createArray();
            if (ds.getExpressions() != null) {
                for (Expression e : ds.getExpressions()) {
                    ObjectNode eNode = JsonUtil.createObject();
                    eNode.put("name", e.getName());
                    eNode.put("description", e.getDescription());
                    eNode.put("expression", e.getExpression());
                    expressions.add(eNode);
                }
            }
            node.set("expressions", expressions);
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode buildReportArray(Workspace workspace) {
        ArrayNode arr = JsonUtil.createArray();
        for (Report r : workspace.getReports()) {
            ObjectNode node = JsonUtil.createObject();
            node.put("id", r.getId());
            node.put("name", r.getName());
            node.put("reportType", r.getReportType());
            node.put("datasetId", r.getDatasetId());
            node.put("createdDateTime", r.getCreatedDateTime());
            node.put("modifiedDateTime", r.getModifiedDateTime());
            node.put("modifiedBy", r.getModifiedBy());
            node.put("createdBy", r.getCreatedBy());
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode buildDashboardArray(Workspace workspace) {
        ArrayNode arr = JsonUtil.createArray();
        for (Dashboard d : workspace.getDashboards()) {
            ObjectNode node = JsonUtil.createObject();
            node.put("id", d.getId());
            node.put("displayName", d.getDisplayName());
            node.put("isReadOnly", d.isReadOnly());

            ArrayNode tiles = JsonUtil.createArray();
            if (d.getTiles() != null) {
                for (Tile t : d.getTiles()) {
                    ObjectNode tNode = JsonUtil.createObject();
                    tNode.put("id", t.getId());
                    tNode.put("title", t.getTitle());
                    tNode.put("reportId", t.getReportId());
                    tNode.put("datasetId", t.getDatasetId());
                    tiles.add(tNode);
                }
            }
            node.set("tiles", tiles);
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode buildDataflowArray(Workspace workspace) {
        ArrayNode arr = JsonUtil.createArray();
        for (Dataflow df : workspace.getDataflows()) {
            ObjectNode node = JsonUtil.createObject();
            node.put("objectId", df.getObjectId());
            node.put("name", df.getName());
            node.put("description", df.getDescription());
            node.put("configuredBy", df.getConfiguredBy());
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode buildUserArray(Workspace workspace) {
        ArrayNode arr = JsonUtil.createArray();
        if (workspace.getUsers() != null) {
            for (PowerBIUser u : workspace.getUsers()) {
                ObjectNode node = JsonUtil.createObject();
                node.put("displayName", u.getDisplayName());
                node.put("emailAddress", u.getEmailAddress());
                node.put("accessRight", u.getAccessRight());
                node.put("principalType", u.getPrincipalType());
                arr.add(node);
            }
        }
        return arr;
    }

    private ArrayNode buildColumnArray(Table table) {
        ArrayNode cols = JsonUtil.createArray();
        if (table.getColumns() == null) return cols;
        for (Column c : table.getColumns()) {
            ObjectNode cNode = JsonUtil.createObject();
            cNode.put("name", c.getName());
            cNode.put("dataType", c.getDataType());
            cNode.put("isHidden", c.isHidden());
            cNode.put("columnType", c.getColumnType());
            cNode.put("sourceColumn", c.getSourceColumn());
            if (c.getExpression() != null) {
                cNode.put("expression", c.getExpression());
            }
            if (c.getFormatString() != null) {
                cNode.put("formatString", c.getFormatString());
            }
            if (c.getLineageTag() != null) {
                cNode.put("lineageTag", c.getLineageTag());
            }
            if (c.getSourceLineageTag() != null) {
                cNode.put("sourceLineageTag", c.getSourceLineageTag());
            }
            if (c.getSummarizeBy() != null) {
                cNode.put("summarizeBy", c.getSummarizeBy());
            }
            if (c.getDescription() != null) {
                cNode.put("description", c.getDescription());
            }
            cols.add(cNode);
        }
        return cols;
    }

    private ArrayNode buildMeasureArray(Table table) {
        ArrayNode measures = JsonUtil.createArray();
        if (table.getMeasures() == null) return measures;
        for (Measure m : table.getMeasures()) {
            ObjectNode mNode = JsonUtil.createObject();
            mNode.put("name", m.getName());
            mNode.put("expression", m.getExpression());
            mNode.put("isHidden", m.isHidden());
            if (m.getFormatString() != null) {
                mNode.put("formatString", m.getFormatString());
            }
            if (m.getDescription() != null) {
                mNode.put("description", m.getDescription());
            }
            if (m.getLineageTag() != null) {
                mNode.put("lineageTag", m.getLineageTag());
            }
            measures.add(mNode);
        }
        return measures;
    }

    private ArrayNode buildTableSourceArray(Table table) {
        ArrayNode sources = JsonUtil.createArray();
        if (table.getSource() == null) return sources;
        for (TableSource src : table.getSource()) {
            ObjectNode sNode = JsonUtil.createObject();
            sNode.put("expression", src.getExpression());
            sources.add(sNode);
        }
        return sources;
    }

    private void writeSummary(MetadataResult result) throws IOException {
        ObjectNode summary = JsonUtil.createObject();
        summary.put("workspaceCount", result.getWorkspaces().size());
        summary.put("datasetCount", result.getDatasetCount());
        summary.put("reportCount", result.getReportCount());
        summary.put("dashboardCount", result.getDashboardCount());
        summary.put("dataflowCount", result.getDataflowCount());
        summary.put("datasourceCount", result.getDatasourceMap().size());
        JsonUtil.writeToFile(summary, new File(outputDir, "metadata_summary.json"));
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
