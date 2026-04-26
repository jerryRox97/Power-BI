/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a datasource instance from the scan result.
 * Connection details vary by datasource type (Sql, Web, File, etc.).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"datasourceId", "datasourceType"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSource {

    private String datasourceId;
    private String datasourceType;
    private String gatewayId;
    private JsonNode connectionDetails;

    public String getServer() {
        return extractDetail("server");
    }

    public String getDatabase() {
        return extractDetail("database");
    }

    public String getUrl() {
        return extractDetail("url");
    }

    public String getPath() {
        return extractDetail("path");
    }

    public String getDomain() {
        return extractDetail("domain");
    }

    public String getSharePointSiteUrl() {
        return extractDetail("sharePointSiteUrl");
    }

    public String getExtensionDataSourceKind() {
        return extractDetail("extensionDataSourceKind");
    }

    private String extractDetail(String field) {
        if (connectionDetails != null && connectionDetails.has(field)) {
            return connectionDetails.get(field).asText("");
        }
        return "";
    }
}
