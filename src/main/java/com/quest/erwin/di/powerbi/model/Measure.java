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
 * Represents a DAX measure within a Power BI dataset table.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"name", "isHidden"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Measure {

    private String name;
    private String expression;
    private boolean isHidden;
    private String formatString;
    private String description;
    private String lineageTag;
}
