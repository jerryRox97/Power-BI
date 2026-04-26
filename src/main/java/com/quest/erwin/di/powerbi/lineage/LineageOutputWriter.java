/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.lineage;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quest.erwin.di.powerbi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Writes lineage graph to JSON output for ACP consumption.
 */
public class LineageOutputWriter {

    private static final Logger log = LoggerFactory.getLogger(LineageOutputWriter.class);

    private final File outputDir;

    public LineageOutputWriter(String outputDirectory) {
        this.outputDir = new File(outputDirectory, "lineage");
        this.outputDir.mkdirs();
    }

    /**
     * Writes the lineage graph as a JSON file with nodes and edges.
     */
    public void write(LineageGraph graph) throws IOException {
        ObjectNode root = JsonUtil.createObject();

        ArrayNode nodesArray = JsonUtil.createArray();
        for (LineageNode node : graph.getNodes().values()) {
            ObjectNode nodeObj = JsonUtil.createObject();
            nodeObj.put("id", node.getId());
            nodeObj.put("name", node.getName());
            nodeObj.put("type", node.getNodeType().name());
            if (node.getWorkspaceId() != null) {
                nodeObj.put("workspaceId", node.getWorkspaceId());
            }
            if (node.getWorkspaceName() != null) {
                nodeObj.put("workspaceName", node.getWorkspaceName());
            }
            if (node.getAdditionalInfo() != null) {
                nodeObj.put("additionalInfo", node.getAdditionalInfo());
            }
            nodesArray.add(nodeObj);
        }
        root.set("nodes", nodesArray);

        ArrayNode edgesArray = JsonUtil.createArray();
        for (LineageEdge edge : graph.getEdges()) {
            ObjectNode edgeObj = JsonUtil.createObject();
            edgeObj.put("sourceId", edge.getSourceId());
            edgeObj.put("targetId", edge.getTargetId());
            edgeObj.put("edgeType", edge.getEdgeType().name());
            if (edge.getDescription() != null) {
                edgeObj.put("description", edge.getDescription());
            }

            LineageNode sourceNode = graph.getNode(edge.getSourceId());
            LineageNode targetNode = graph.getNode(edge.getTargetId());
            if (sourceNode != null) edgeObj.put("sourceName", sourceNode.getName());
            if (targetNode != null) edgeObj.put("targetName", targetNode.getName());

            edgesArray.add(edgeObj);
        }
        root.set("edges", edgesArray);

        ObjectNode summary = JsonUtil.createObject();
        summary.put("totalNodes", graph.nodeCount());
        summary.put("totalEdges", graph.edgeCount());
        root.set("summary", summary);

        File outputFile = new File(outputDir, "lineage_graph.json");
        JsonUtil.writeToFile(root, outputFile);
        log.info("Lineage graph written: {} nodes, {} edges -> {}",
                graph.nodeCount(), graph.edgeCount(), outputFile.getAbsolutePath());
    }
}
