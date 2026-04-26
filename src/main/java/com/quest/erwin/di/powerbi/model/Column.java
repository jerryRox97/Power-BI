/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a column within a Power BI dataset table.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Column {

    private String name;
    private String dataType;
    private boolean isHidden;
    private String columnType;
    private String sourceColumn;
    private String expression;
    private String formatString;
    private String lineageTag;
    private String sourceLineageTag;
    private String summarizeBy;
    private String description;
}
