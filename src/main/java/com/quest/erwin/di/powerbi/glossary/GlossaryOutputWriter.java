/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.glossary;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quest.erwin.di.powerbi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes business glossary entries to JSON output files.
 * Groups entries by category for organized output.
 */
public class GlossaryOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(GlossaryOutputWriter.class);

    private final File outputDir;

    public GlossaryOutputWriter(String outputDirectory) {
        this.outputDir = new File(outputDirectory, "glossary");
        this.outputDir.mkdirs();
    }

    /**
     * Writes all glossary entries to a JSON file, grouped by category.
     */
    public void write(List<GlossaryEntry> entries) throws IOException {
        ObjectNode root = JsonUtil.createObject();

        Map<GlossaryEntry.Category, List<GlossaryEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(GlossaryEntry::getCategory));

        for (Map.Entry<GlossaryEntry.Category, List<GlossaryEntry>> entry : grouped.entrySet()) {
            ArrayNode categoryArray = JsonUtil.createArray();
            for (GlossaryEntry glossaryEntry : entry.getValue()) {
                categoryArray.add(buildEntryNode(glossaryEntry));
            }
            root.set(entry.getKey().name().toLowerCase(), categoryArray);
        }

        ObjectNode summary = JsonUtil.createObject();
        summary.put("totalEntries", entries.size());
        for (Map.Entry<GlossaryEntry.Category, List<GlossaryEntry>> entry : grouped.entrySet()) {
            summary.put(entry.getKey().name().toLowerCase() + "Count", entry.getValue().size());
        }
        root.set("summary", summary);

        File outputFile = new File(outputDir, "business_glossary.json");
        JsonUtil.writeToFile(root, outputFile);
        log.info("Business glossary written: {} entries -> {}", entries.size(), outputFile.getAbsolutePath());
    }

    private ObjectNode buildEntryNode(GlossaryEntry entry) {
        ObjectNode node = JsonUtil.createObject();
        node.put("name", entry.getName());
        putIfNotNull(node, "description", entry.getDescription());
        node.put("category", entry.getCategory().name());
        putIfNotNull(node, "sourceType", entry.getSourceType());
        putIfNotNull(node, "workspaceName", entry.getWorkspaceName());
        putIfNotNull(node, "workspaceId", entry.getWorkspaceId());
        putIfNotNull(node, "parentName", entry.getParentName());
        putIfNotNull(node, "parentId", entry.getParentId());
        putIfNotNull(node, "dataType", entry.getDataType());
        putIfNotNull(node, "expression", entry.getExpression());
        putIfNotNull(node, "owner", entry.getOwner());
        putIfNotNull(node, "additionalProperties", entry.getAdditionalProperties());
        return node;
    }

    private void putIfNotNull(ObjectNode node, String field, String value) {
        if (value != null && !value.isEmpty()) {
            node.put(field, value);
        }
    }
}
