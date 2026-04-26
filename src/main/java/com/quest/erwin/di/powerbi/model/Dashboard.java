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
 * Represents a Power BI dashboard from the scan result.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "displayName"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Dashboard {

    private String id;
    private String displayName;
    private boolean isReadOnly;

    @Builder.Default private List<Tile> tiles = new ArrayList<>();
    @Builder.Default private List<PowerBIUser> users = new ArrayList<>();
}
