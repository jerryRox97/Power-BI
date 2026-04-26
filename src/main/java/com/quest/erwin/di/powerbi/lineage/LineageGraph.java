/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.lineage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds the complete lineage graph: nodes and edges.
 */
@Getter
public class LineageGraph {

    private final Map<String, LineageNode> nodes = new LinkedHashMap<>();
    private final List<LineageEdge> edges = new ArrayList<>();

    public void addNode(LineageNode node) {
        nodes.putIfAbsent(node.getId(), node);
    }

    public void addEdge(LineageEdge edge) {
        edges.add(edge);
    }

    public LineageNode getNode(String id) {
        return nodes.get(id);
    }

    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    /**
     * Returns edges where the given node is the target (i.e., upstream lineage).
     */
    public List<LineageEdge> getIncomingEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.getTargetId().equals(nodeId))
                .collect(Collectors.toList());
    }

    /**
     * Returns edges where the given node is the source (i.e., downstream lineage).
     */
    public List<LineageEdge> getOutgoingEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.getSourceId().equals(nodeId))
                .collect(Collectors.toList());
    }
}
