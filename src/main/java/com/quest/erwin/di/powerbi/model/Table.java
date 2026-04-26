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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a table within a Power BI dataset.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"name", "isHidden", "storageMode"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Table {

    private String name;
    private boolean isHidden;
    private String storageMode;

    @Builder.Default private List<Column> columns = new ArrayList<>();
    @Builder.Default private List<Measure> measures = new ArrayList<>();
    @Builder.Default private List<TableSource> source = new ArrayList<>();
}
