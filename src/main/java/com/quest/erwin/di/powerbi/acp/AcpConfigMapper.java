/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.acp;

import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Maps ACP connector options (key-value pairs from erwin DI) to {@link ConnectorConfig}.
 * The ACP platform passes options as a Map&lt;String, String&gt; from the connector UI.
 */
public class AcpConfigMapper {

    private static final Logger log = LoggerFactory.getLogger(AcpConfigMapper.class);

    // ACP option keys (must match connector_options.json)
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_CLIENT_SECRET = "clientSecret";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_PROJECT_NAME = "projectName";
    private static final String KEY_WORKSPACE_NAME = "workspaceName";
    private static final String KEY_DATABASE_FILE_PATH = "databaseFilePath";
    private static final String KEY_SCAN_BATCH_SIZE = "scanBatchSize";
    private static final String KEY_THROTTLE_DELAY_MS = "throttleDelayMs";
    private static final String KEY_INCREMENTAL_LOAD = "incrementalLoad";
    private static final String KEY_FILTER_OBJECT_TYPE = "filterObjectType";
    private static final String KEY_FILTER_OPERATOR = "filterOperator";
    private static final String KEY_FILTER_VALUE = "filterValue";
    private static final String KEY_OUTPUT_DIRECTORY = "outputDirectory";

    /**
     * Maps ACP option entries to a ConnectorConfig instance.
     */
    public ConnectorConfig map(Map<String, String> options) {
        log.info("Mapping ACP connector options to configuration");

        ConnectorConfig config = ConnectorConfig.builder()
                .clientId(getOrDefault(options, KEY_CLIENT_ID, ""))
                .clientSecret(getOrDefault(options, KEY_CLIENT_SECRET, ""))
                .tenantId(getOrDefault(options, KEY_TENANT_ID, ""))
                .projectName(getOrDefault(options, KEY_PROJECT_NAME, "PowerBI"))
                .workspaceName(getOrDefault(options, KEY_WORKSPACE_NAME, ""))
                .databaseFilePath(getOrDefault(options, KEY_DATABASE_FILE_PATH, ""))
                .scanBatchSize(getIntOrDefault(options, KEY_SCAN_BATCH_SIZE, 100))
                .throttleDelayMs(getLongOrDefault(options, KEY_THROTTLE_DELAY_MS, 1_800_000L))
                .incrementalLoad(getBoolOrDefault(options, KEY_INCREMENTAL_LOAD, false))
                .filterObjectType(getOrDefault(options, KEY_FILTER_OBJECT_TYPE, ""))
                .filterOperator(getOrDefault(options, KEY_FILTER_OPERATOR, ""))
                .filterValue(getOrDefault(options, KEY_FILTER_VALUE, ""))
                .outputDirectory(getOrDefault(options, KEY_OUTPUT_DIRECTORY, "output"))
                .build();

        log.info("Configuration mapped: project={}, incremental={}, batchSize={}",
                config.getProjectName(), config.isIncrementalLoad(), config.getScanBatchSize());
        return config;
    }

    private String getOrDefault(Map<String, String> options, String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    private int getIntOrDefault(Map<String, String> options, String key, int defaultValue) {
        String val = options.get(key);
        if (val != null && !val.isEmpty()) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer for '{}': '{}', using default: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    private long getLongOrDefault(Map<String, String> options, String key, long defaultValue) {
        String val = options.get(key);
        if (val != null && !val.isEmpty()) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                log.warn("Invalid long for '{}': '{}', using default: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    private boolean getBoolOrDefault(Map<String, String> options, String key, boolean defaultValue) {
        String val = options.get(key);
        if (val != null && !val.isEmpty()) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }
}
