/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.lineage;

import com.quest.erwin.di.powerbi.metadata.MetadataResult;
import com.quest.erwin.di.powerbi.model.Column;
import com.quest.erwin.di.powerbi.model.Dashboard;
import com.quest.erwin.di.powerbi.model.DataSource;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.Dataflow;
import com.quest.erwin.di.powerbi.model.DatasourceUsage;
import com.quest.erwin.di.powerbi.model.Measure;
import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.Table;
import com.quest.erwin.di.powerbi.model.TableSource;
import com.quest.erwin.di.powerbi.model.Tile;
import com.quest.erwin.di.powerbi.model.UpstreamDataflow;
import com.quest.erwin.di.powerbi.model.UpstreamDataset;
import com.quest.erwin.di.powerbi.model.Workspace;
import com.quest.erwin.di.powerbi.parser.dax.DaxExpressionParserAntlr;
import com.quest.erwin.di.powerbi.parser.dax.DaxLineageVisitor;
import com.quest.erwin.di.powerbi.parser.powerquery.PowerQueryExpressionParserAntlr;
import com.quest.erwin.di.powerbi.parser.powerquery.PowerQueryLineageVisitor;
import com.quest.erwin.di.powerbi.util.DaxExpressionParser;
import com.quest.erwin.di.powerbi.util.MExpressionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a directed lineage graph from the extracted metadata.
 * Resolves:
 * - Datasource → Dataset (via datasourceUsages + datasourceInstances)
 * - Dataset → Report (via report.datasetId)
 * - Dataset → Dashboard (via dashboard tiles)
 * - Dataset → Dataset (via upstreamDatasets)
 * - Dataflow → Dataset (via upstreamDataflows)
 * - Report → Dashboard (via dashboard tiles)
 */
public class LineageBuilder {

    private static final Logger log = LoggerFactory.getLogger(LineageBuilder.class);

    /**
     * Builds the complete lineage graph from metadata.
     */
    public LineageGraph build(MetadataResult metadata) {
        log.info("Building lineage graph");
        LineageGraph graph = new LineageGraph();

        addWorkspaceNodes(graph, metadata);
        addDatasourceNodes(graph, metadata);
        addDatasetNodes(graph, metadata);
        addReportNodes(graph, metadata);
        addDashboardNodes(graph, metadata);
        addDataflowNodes(graph, metadata);

        resolveDatasourceToDatasetEdges(graph, metadata);
        resolveDatasetToReportEdges(graph, metadata);
        resolveDashboardTileEdges(graph, metadata);
        resolveUpstreamDatasetEdges(graph, metadata);
        resolveUpstreamDataflowEdges(graph, metadata);
        resolveTableSourceLineage(graph, metadata);
        resolveCalculatedColumnLineage(graph, metadata);
        resolveMeasureLineage(graph, metadata);

        log.info("Lineage graph built: {} nodes, {} edges", graph.nodeCount(), graph.edgeCount());
        return graph;
    }

    private void addWorkspaceNodes(LineageGraph graph, MetadataResult metadata) {
        for (Workspace ws : metadata.getWorkspaces()) {
            graph.addNode(LineageNode.builder()
                    .id(ws.getId())
                    .name(ws.getName())
                    .nodeType(LineageNode.NodeType.WORKSPACE)
                    .workspaceId(ws.getId())
                    .workspaceName(ws.getName())
                    .build());
        }
    }

    private void addDatasourceNodes(LineageGraph graph, MetadataResult metadata) {
        for (DataSource ds : metadata.getDatasourceMap().values()) {
            String dsName = buildDatasourceName(ds);
            graph.addNode(LineageNode.builder()
                    .id(ds.getDatasourceId())
                    .name(dsName)
                    .nodeType(LineageNode.NodeType.DATASOURCE)
                    .additionalInfo(ds.getDatasourceType())
                    .build());
        }
    }

    private void addDatasetNodes(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            Workspace ws = metadata.getWorkspaceMap().get(ds.getWorkspaceId());
            graph.addNode(LineageNode.builder()
                    .id(ds.getId())
                    .name(ds.getName())
                    .nodeType(LineageNode.NodeType.DATASET)
                    .workspaceId(ds.getWorkspaceId())
                    .workspaceName(ws != null ? ws.getName() : "")
                    .build());
        }
    }

    private void addReportNodes(LineageGraph graph, MetadataResult metadata) {
        for (Report r : metadata.getReports()) {
            Workspace ws = metadata.getWorkspaceMap().get(r.getWorkspaceId());
            graph.addNode(LineageNode.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .nodeType(LineageNode.NodeType.REPORT)
                    .workspaceId(r.getWorkspaceId())
                    .workspaceName(ws != null ? ws.getName() : "")
                    .build());
        }
    }

    private void addDashboardNodes(LineageGraph graph, MetadataResult metadata) {
        for (Dashboard d : metadata.getDashboards()) {
            graph.addNode(LineageNode.builder()
                    .id(d.getId())
                    .name(d.getDisplayName())
                    .nodeType(LineageNode.NodeType.DASHBOARD)
                    .build());
        }
    }

    private void addDataflowNodes(LineageGraph graph, MetadataResult metadata) {
        for (Dataflow df : metadata.getDataflows()) {
            Workspace ws = metadata.getWorkspaceMap().get(df.getWorkspaceId());
            graph.addNode(LineageNode.builder()
                    .id(df.getObjectId())
                    .name(df.getName())
                    .nodeType(LineageNode.NodeType.DATAFLOW)
                    .workspaceId(df.getWorkspaceId())
                    .workspaceName(ws != null ? ws.getName() : "")
                    .build());
        }
    }

    private void resolveDatasourceToDatasetEdges(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getDatasourceUsages() == null) continue;
            for (DatasourceUsage usage : ds.getDatasourceUsages()) {
                String dsInstanceId = usage.getDatasourceInstanceId();
                if (graph.hasNode(dsInstanceId)) {
                    graph.addEdge(LineageEdge.builder()
                            .sourceId(dsInstanceId)
                            .targetId(ds.getId())
                            .edgeType(LineageEdge.EdgeType.DATASOURCE_TO_DATASET)
                            .description("Datasource feeds dataset '" + ds.getName() + "'")
                            .build());
                }
            }
        }
    }

    private void resolveDatasetToReportEdges(LineageGraph graph, MetadataResult metadata) {
        for (Report report : metadata.getReports()) {
            if (report.getDatasetId() != null && graph.hasNode(report.getDatasetId())) {
                graph.addEdge(LineageEdge.builder()
                        .sourceId(report.getDatasetId())
                        .targetId(report.getId())
                        .edgeType(LineageEdge.EdgeType.DATASET_TO_REPORT)
                        .description("Dataset feeds report '" + report.getName() + "'")
                        .build());
            }
        }
    }

    private void resolveDashboardTileEdges(LineageGraph graph, MetadataResult metadata) {
        for (Dashboard dashboard : metadata.getDashboards()) {
            if (dashboard.getTiles() == null) continue;
            for (Tile tile : dashboard.getTiles()) {
                if (tile.getDatasetId() != null && graph.hasNode(tile.getDatasetId())) {
                    graph.addEdge(LineageEdge.builder()
                            .sourceId(tile.getDatasetId())
                            .targetId(dashboard.getId())
                            .edgeType(LineageEdge.EdgeType.DATASET_TO_DASHBOARD)
                            .description("Dataset feeds dashboard tile '" + tile.getTitle() + "'")
                            .build());
                }
                if (tile.getReportId() != null && graph.hasNode(tile.getReportId())) {
                    graph.addEdge(LineageEdge.builder()
                            .sourceId(tile.getReportId())
                            .targetId(dashboard.getId())
                            .edgeType(LineageEdge.EdgeType.REPORT_TO_DASHBOARD)
                            .description("Report feeds dashboard tile '" + tile.getTitle() + "'")
                            .build());
                }
            }
        }
    }

    private void resolveUpstreamDatasetEdges(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getUpstreamDatasets() == null) continue;
            for (UpstreamDataset upstream : ds.getUpstreamDatasets()) {
                String upstreamId = upstream.getTargetDatasetId();
                if (upstreamId != null) {
                    if (!graph.hasNode(upstreamId)) {
                        graph.addNode(LineageNode.builder()
                                .id(upstreamId)
                                .name("External Dataset " + upstreamId)
                                .nodeType(LineageNode.NodeType.DATASET)
                                .workspaceId(upstream.getGroupId())
                                .additionalInfo("external")
                                .build());
                    }
                    graph.addEdge(LineageEdge.builder()
                            .sourceId(upstreamId)
                            .targetId(ds.getId())
                            .edgeType(LineageEdge.EdgeType.DATASET_TO_DATASET)
                            .description("Upstream dataset feeds '" + ds.getName() + "'")
                            .build());
                }
            }
        }
    }

    private void resolveUpstreamDataflowEdges(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getUpstreamDataflows() == null) continue;
            for (UpstreamDataflow upstream : ds.getUpstreamDataflows()) {
                String upstreamId = upstream.getTargetDataflowId();
                if (upstreamId != null) {
                    if (!graph.hasNode(upstreamId)) {
                        graph.addNode(LineageNode.builder()
                                .id(upstreamId)
                                .name("External Dataflow " + upstreamId)
                                .nodeType(LineageNode.NodeType.DATAFLOW)
                                .workspaceId(upstream.getGroupId())
                                .additionalInfo("external")
                                .build());
                    }
                    graph.addEdge(LineageEdge.builder()
                            .sourceId(upstreamId)
                            .targetId(ds.getId())
                            .edgeType(LineageEdge.EdgeType.DATAFLOW_TO_DATASET)
                            .description("Dataflow feeds dataset '" + ds.getName() + "'")
                            .build());
                }
            }
        }
    }

    /**
     * Adds TABLE nodes for each table in a dataset and resolves M expression sources.
     * Parses table source expressions to extract SQL table references.
     */
    private void resolveTableSourceLineage(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getTables() == null) continue;
            for (Table table : ds.getTables()) {
                String tableNodeId = ds.getId() + "::" + table.getName();
                graph.addNode(LineageNode.builder()
                        .id(tableNodeId)
                        .name(table.getName())
                        .nodeType(LineageNode.NodeType.TABLE)
                        .workspaceId(ds.getWorkspaceId())
                        .additionalInfo("dataset=" + ds.getName())
                        .build());

                graph.addEdge(LineageEdge.builder()
                        .sourceId(tableNodeId)
                        .targetId(ds.getId())
                        .edgeType(LineageEdge.EdgeType.TABLE_TO_DATASET)
                        .description("Table '" + table.getName() + "' belongs to dataset '" + ds.getName() + "'")
                        .build());

                if (table.getSource() == null) continue;
                for (TableSource src : table.getSource()) {
                    if (src.getExpression() == null) continue;

                    // ANTLR-based M parsing for data source extraction
                    PowerQueryExpressionParserAntlr.ParseResult antlrResult =
                            PowerQueryExpressionParserAntlr.parse(src.getExpression());
                    if (antlrResult.isParsed()) {
                        addAntlrDataSourceNodes(graph, antlrResult, tableNodeId, table.getName());
                    }

                    // Regex-based fallback for SQL table extraction
                    MExpressionParser.ParseResult parsed = MExpressionParser.parse(src.getExpression());
                    addSqlTableNodes(graph, parsed, tableNodeId, table.getName());
                }
            }
        }
    }

    private void addSqlTableNodes(LineageGraph graph, MExpressionParser.ParseResult parsed,
                                  String tableNodeId, String tableName) {
        List<String> sqlTables = parsed.getReferencedTables();
        if (sqlTables.isEmpty()) return;

        for (String sqlTable : sqlTables) {
            String sqlNodeId = "sql::" + sqlTable;
            if (!graph.hasNode(sqlNodeId)) {
                String sourceDesc = parsed.getSourceType();
                if (parsed.getServer() != null) sourceDesc += " @ " + parsed.getServer();
                if (parsed.getDatabase() != null) sourceDesc += "/" + parsed.getDatabase();
                if (parsed.getCatalog() != null) sourceDesc += "/" + parsed.getCatalog();

                graph.addNode(LineageNode.builder()
                        .id(sqlNodeId)
                        .name(sqlTable)
                        .nodeType(LineageNode.NodeType.DATASOURCE)
                        .additionalInfo(sourceDesc)
                        .build());
            }
            graph.addEdge(LineageEdge.builder()
                    .sourceId(sqlNodeId)
                    .targetId(tableNodeId)
                    .edgeType(LineageEdge.EdgeType.SQL_TABLE_TO_PBI_TABLE)
                    .description("SQL table '" + sqlTable + "' feeds PBI table '" + tableName + "'")
                    .build());
        }
    }

    /**
     * Resolves calculated column DAX expression references to other tables.
     * Uses ANTLR-based parser with regex fallback.
     */
    private void resolveCalculatedColumnLineage(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getTables() == null) continue;
            for (Table table : ds.getTables()) {
                if (table.getColumns() == null) continue;
                String tableNodeId = ds.getId() + "::" + table.getName();
                for (Column col : table.getColumns()) {
                    if (col.getExpression() == null) continue;
                    List<String> refTables = extractDaxTableReferences(col.getExpression());
                    for (String refTable : refTables) {
                        String refNodeId = ds.getId() + "::" + refTable;
                        if (graph.hasNode(refNodeId) && !refNodeId.equals(tableNodeId)) {
                            graph.addEdge(LineageEdge.builder()
                                    .sourceId(refNodeId)
                                    .targetId(tableNodeId)
                                    .edgeType(LineageEdge.EdgeType.TABLE_TO_TABLE)
                                    .description("Calc column '" + col.getName() + "' references '" + refTable + "'")
                                    .build());
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves measure DAX expression references to tables within the same dataset.
     * Uses ANTLR-based parser with regex fallback.
     */
    private void resolveMeasureLineage(LineageGraph graph, MetadataResult metadata) {
        for (Dataset ds : metadata.getDatasets()) {
            if (ds.getTables() == null) continue;
            for (Table table : ds.getTables()) {
                if (table.getMeasures() == null) continue;
                String tableNodeId = ds.getId() + "::" + table.getName();
                for (Measure measure : table.getMeasures()) {
                    if (measure.getExpression() == null) continue;
                    List<String> refTables = extractDaxTableReferences(measure.getExpression());
                    for (String refTable : refTables) {
                        String refNodeId = ds.getId() + "::" + refTable;
                        if (graph.hasNode(refNodeId) && !refNodeId.equals(tableNodeId)) {
                            graph.addEdge(LineageEdge.builder()
                                    .sourceId(refNodeId)
                                    .targetId(tableNodeId)
                                    .edgeType(LineageEdge.EdgeType.TABLE_TO_TABLE)
                                    .description("Measure '" + measure.getName() + "' references '" + refTable + "'")
                                    .build());
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts table references from a DAX expression using ANTLR with regex fallback.
     */
    private List<String> extractDaxTableReferences(String expression) {
        DaxExpressionParserAntlr.ParseResult antlrResult = DaxExpressionParserAntlr.parse(expression);
        if (antlrResult.isParsed() && !antlrResult.getReferencedTables().isEmpty()) {
            return antlrResult.getReferencedTableList();
        }
        return DaxExpressionParser.extractReferencedTables(expression);
    }

    /**
     * Adds data source nodes discovered by ANTLR-based M expression parsing.
     */
    private void addAntlrDataSourceNodes(LineageGraph graph,
                                          PowerQueryExpressionParserAntlr.ParseResult result,
                                          String tableNodeId, String tableName) {
        for (PowerQueryLineageVisitor.DataSourceRef dsRef : result.getDataSourceRefs()) {
            String dsNodeId = "antlr::" + dsRef.sourceType() + "::" + dsRef.server() + dsRef.database() + dsRef.url();
            if (!graph.hasNode(dsNodeId)) {
                String dsName = buildAntlrDataSourceName(dsRef);
                graph.addNode(LineageNode.builder()
                        .id(dsNodeId)
                        .name(dsName)
                        .nodeType(LineageNode.NodeType.DATASOURCE)
                        .additionalInfo(dsRef.sourceType())
                        .build());
            }
            graph.addEdge(LineageEdge.builder()
                    .sourceId(dsNodeId)
                    .targetId(tableNodeId)
                    .edgeType(LineageEdge.EdgeType.SQL_TABLE_TO_PBI_TABLE)
                    .description(dsRef.sourceType() + " source feeds PBI table '" + tableName + "'")
                    .build());
        }
    }

    private String buildAntlrDataSourceName(PowerQueryLineageVisitor.DataSourceRef dsRef) {
        StringBuilder sb = new StringBuilder(dsRef.sourceType());
        if (!dsRef.server().isEmpty()) {
            sb.append(": ").append(dsRef.server());
            if (!dsRef.database().isEmpty()) {
                sb.append("/").append(dsRef.database());
            }
        } else if (!dsRef.url().isEmpty()) {
            sb.append(": ").append(dsRef.url());
        }
        return sb.toString();
    }

    private String buildDatasourceName(DataSource ds) {
        String type = ds.getDatasourceType() != null ? ds.getDatasourceType() : "Unknown";
        String server = ds.getServer();
        String database = ds.getDatabase();

        if (!server.isEmpty() && !database.isEmpty()) {
            return type + ": " + server + "/" + database;
        }
        if (!server.isEmpty()) {
            return type + ": " + server;
        }
        String url = ds.getUrl();
        if (!url.isEmpty()) {
            return type + ": " + url;
        }
        String path = ds.getPath();
        if (!path.isEmpty()) {
            return type + ": " + path;
        }
        String sharePoint = ds.getSharePointSiteUrl();
        if (!sharePoint.isEmpty()) {
            return type + ": " + sharePoint;
        }
        return type + ": " + ds.getDatasourceId();
    }
}
