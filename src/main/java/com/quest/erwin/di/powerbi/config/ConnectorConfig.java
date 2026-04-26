/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.config;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable connector configuration. Built once at startup from ACP options or CLI arguments.
 */
@Getter
@Builder
@ToString(exclude = "clientSecret")
public class ConnectorConfig {

    private final String clientId;
    private final String clientSecret;
    private final String tenantId;

    private final String projectName;
    @Builder.Default private final String workspaceName = "";
    @Builder.Default private final String databaseFilePath = "";

    @Builder.Default private final int scanBatchSize = 100;
    @Builder.Default private final long throttleDelayMs = 1_800_000L; // 30 minutes
    @Builder.Default private final int maxThrottleBatchCount = 250;

    @Builder.Default private final boolean incrementalLoad = false;
    @Builder.Default private final String lastRunTime = "";

    @Builder.Default private final String filterObjectType = "";
    @Builder.Default private final String filterOperator = "";
    @Builder.Default private final String filterValue = "";

    @Builder.Default private final String outputDirectory = "output";
}
