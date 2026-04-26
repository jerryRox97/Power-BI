/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.acp;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quest.erwin.di.powerbi.glossary.GlossaryEntry;
import com.quest.erwin.di.powerbi.lineage.LineageEdge;
import com.quest.erwin.di.powerbi.lineage.LineageGraph;
import com.quest.erwin.di.powerbi.lineage.LineageNode;
import com.quest.erwin.di.powerbi.metadata.MetadataResult;
import com.quest.erwin.di.powerbi.model.Column;
import com.quest.erwin.di.powerbi.model.DataSource;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.DatasourceUsage;
import com.quest.erwin.di.powerbi.model.Measure;
import com.quest.erwin.di.powerbi.model.Report;
import com.quest.erwin.di.powerbi.model.Table;
import com.quest.erwin.di.powerbi.model.Workspace;
import com.quest.erwin.di.powerbi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Writes ACP-compatible output files:
 * - mapping_spec.json (erwin DI mapping specification rows)
 * - additional_properties.json (business glossary as additional properties)
 * - lineage_mappings.json (lineage edges in erwin DI format)
 */
public class AcpResultWriter {

    private static final Logger log = LoggerFactory.getLogger(AcpResultWriter.class);

    private final File outputDir;

    public AcpResultWriter(String outputDirectory) {
        this.outputDir = new File(outputDirectory, "acp");
        this.outputDir.mkdirs();
    }

    /**
     * Writes all ACP-compatible output files.
     */
    public void write(MetadataResult metadata, LineageGraph lineage, List<GlossaryEntry> glossary) throws IOException {
        writeMappingSpec(metadata);
        writeAdditionalProperties(glossary);
        writeLineageMappings(lineage);
        log.info("ACP output written to {}", outputDir.getAbsolutePath());
    }

    /**
     * Writes mapping specification rows in erwin DI format.
     * Each row represents a source-to-target column mapping.
     */
    private void writeMappingSpec(MetadataResult metadata) throws IOException {
        ArrayNode rows = JsonUtil.createArray();
        int rowIndex = 1;

        for (Workspace ws : metadata.getWorkspaces()) {
            for (Dataset ds : ws.getDatasets()) {
                if (ds.getTables() == null) continue;
                for (Table table : ds.getTables()) {
                    rowIndex = addTableMappingRows(rows, rowIndex, ws, ds, table, metadata);
                }
            }
        }

        ObjectNode root = JsonUtil.createObject();
        root.set("mappingSpecRows", rows);
        root.put("totalRows", rows.size());

        JsonUtil.writeToFile(root, new File(outputDir, "mapping_spec.json"));
        log.info("Mapping spec written: {} rows", rows.size());
    }

    private int addTableMappingRows(ArrayNode rows, int rowIndex,
                                     Workspace ws, Dataset ds, Table table,
                                     MetadataResult metadata) {
        String sourceName = resolveSourceName(ds, metadata);

        if (table.getColumns() != null) {
            for (Column col : table.getColumns()) {
                ObjectNode row = createMappingRow(rowIndex++, sourceName, table.getName(),
                        col.getName(), col.getDataType(), ds.getName(), table.getName(),
                        col.getName(), col.getDataType(), ws.getName());
                rows.add(row);
            }
        }

        if (table.getMeasures() != null) {
            for (Measure m : table.getMeasures()) {
                ObjectNode row = createMappingRow(rowIndex++, ds.getName(), table.getName(),
                        m.getName(), "Measure/DAX", ds.getName(), table.getName(),
                        m.getName(), "Measure/DAX", ws.getName());
                row.put("businessRule", m.getExpression());
                rows.add(row);
            }
        }

        return rowIndex;
    }

    private ObjectNode createMappingRow(int rowIndex, String sourceSystem, String sourceTable,
                                         String sourceColumn, String sourceDataType,
                                         String targetSystem, String targetTable,
                                         String targetColumn, String targetDataType,
                                         String workspaceName) {
        ObjectNode row = JsonUtil.createObject();
        row.put("rowIndex", rowIndex);
        row.put("sourceSystem", sourceSystem);
        row.put("sourceTable", sourceTable);
        row.put("sourceColumn", sourceColumn);
        row.put("sourceDataType", sourceDataType);
        row.put("targetSystem", targetSystem);
        row.put("targetTable", targetTable);
        row.put("targetColumn", targetColumn);
        row.put("targetDataType", targetDataType);
        row.put("workspace", workspaceName);
        return row;
    }

    private String resolveSourceName(Dataset ds, MetadataResult metadata) {
        if (ds.getDatasourceUsages() == null || ds.getDatasourceUsages().isEmpty()) {
            return ds.getName();
        }
        DatasourceUsage firstUsage = ds.getDatasourceUsages().get(0);
        DataSource dsource = metadata.getDatasourceMap().get(firstUsage.getDatasourceInstanceId());
        if (dsource == null) {
            return ds.getName();
        }
        String server = dsource.getServer();
        String database = dsource.getDatabase();
        if (!server.isEmpty() && !database.isEmpty()) {
            return server + "/" + database;
        }
        return dsource.getDatasourceType() + "_" + dsource.getDatasourceId().substring(0, 8);
    }

    /**
     * Writes business glossary as ACP additional properties.
     */
    private void writeAdditionalProperties(List<GlossaryEntry> glossary) throws IOException {
        ArrayNode props = JsonUtil.createArray();
        for (GlossaryEntry entry : glossary) {
            ObjectNode prop = JsonUtil.createObject();
            prop.put("name", entry.getName());
            prop.put("description", entry.getDescription() != null ? entry.getDescription() : "");
            prop.put("category", entry.getCategory().name());
            if (entry.getSourceType() != null) prop.put("sourceType", entry.getSourceType());
            if (entry.getWorkspaceName() != null) prop.put("workspace", entry.getWorkspaceName());
            if (entry.getParentName() != null) prop.put("parent", entry.getParentName());
            if (entry.getDataType() != null) prop.put("dataType", entry.getDataType());
            if (entry.getOwner() != null) prop.put("owner", entry.getOwner());
            props.add(prop);
        }

        ObjectNode root = JsonUtil.createObject();
        root.set("additionalProperties", props);
        root.put("totalEntries", props.size());

        JsonUtil.writeToFile(root, new File(outputDir, "additional_properties.json"));
        log.info("Additional properties written: {} entries", props.size());
    }

    /**
     * Writes lineage edges in erwin DI mapping format.
     */
    private void writeLineageMappings(LineageGraph lineage) throws IOException {
        ArrayNode mappings = JsonUtil.createArray();
        for (LineageEdge edge : lineage.getEdges()) {
            ObjectNode mapping = JsonUtil.createObject();
            LineageNode source = lineage.getNode(edge.getSourceId());
            LineageNode target = lineage.getNode(edge.getTargetId());

            mapping.put("sourceId", edge.getSourceId());
            mapping.put("sourceName", source != null ? source.getName() : edge.getSourceId());
            mapping.put("sourceType", source != null ? source.getNodeType().name() : "UNKNOWN");
            mapping.put("targetId", edge.getTargetId());
            mapping.put("targetName", target != null ? target.getName() : edge.getTargetId());
            mapping.put("targetType", target != null ? target.getNodeType().name() : "UNKNOWN");
            mapping.put("edgeType", edge.getEdgeType().name());
            if (edge.getDescription() != null) mapping.put("description", edge.getDescription());

            mappings.add(mapping);
        }

        ObjectNode root = JsonUtil.createObject();
        root.set("lineageMappings", mappings);
        root.put("totalMappings", mappings.size());

        JsonUtil.writeToFile(root, new File(outputDir, "lineage_mappings.json"));
        log.info("Lineage mappings written: {} edges", mappings.size());
    }
}
