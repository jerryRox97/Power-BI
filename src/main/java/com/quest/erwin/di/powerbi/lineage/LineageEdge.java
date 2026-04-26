/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.lineage;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a directed edge in the lineage graph: source → target.
 */
@Getter
@Builder
@ToString
public class LineageEdge {

    /**
     * Types of lineage relationships.
     */
    public enum EdgeType {
        DATASOURCE_TO_DATASET,
        DATASET_TO_REPORT,
        DATASET_TO_DASHBOARD,
        DATASET_TO_DATASET,
        DATAFLOW_TO_DATASET,
        REPORT_TO_DASHBOARD,
        TABLE_TO_DATASET,
        SQL_TABLE_TO_PBI_TABLE,
        TABLE_TO_TABLE
    }

    private final String sourceId;
    private final String targetId;
    private final EdgeType edgeType;
    private final String description;
}
