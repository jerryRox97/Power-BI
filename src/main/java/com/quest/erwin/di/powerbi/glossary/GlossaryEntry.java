/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.glossary;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a business glossary entry for erwin DI platform.
 * Maps Power BI artifacts (datasets, tables, columns, measures) to business terms.
 */
@Getter
@Builder
@ToString(of = {"name", "category", "sourceType"})
public class GlossaryEntry {

    /**
     * Categories for glossary entries.
     */
    public enum Category {
        DATASET, TABLE, COLUMN, MEASURE, EXPRESSION, REPORT, DASHBOARD, DATAFLOW, DATASOURCE, WORKSPACE
    }

    private final String name;
    private final String description;
    private final Category category;
    private final String sourceType;
    private final String workspaceName;
    private final String workspaceId;
    private final String parentName;
    private final String parentId;
    private final String dataType;
    private final String expression;
    private final String owner;
    private final String additionalProperties;
}
