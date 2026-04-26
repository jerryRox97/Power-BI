/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.lineage;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a node in the lineage graph.
 * Each node is a Power BI artifact (datasource, dataset, report, dashboard, dataflow).
 */
@Getter
@Builder
@ToString(of = {"id", "name", "nodeType"})
public class LineageNode {

    /**
     * Types of nodes in the Power BI lineage graph.
     */
    public enum NodeType {
        DATASOURCE, DATASET, TABLE, REPORT, DASHBOARD, DATAFLOW, WORKSPACE
    }

    private final String id;
    private final String name;
    private final NodeType nodeType;
    private final String workspaceId;
    private final String workspaceName;
    private final String additionalInfo;
}
