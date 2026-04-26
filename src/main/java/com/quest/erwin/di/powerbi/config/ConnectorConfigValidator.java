/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.config;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates {@link ConnectorConfig} before connector execution.
 */
public final class ConnectorConfigValidator {

    private ConnectorConfigValidator() {}

    public static void validate(ConnectorConfig config) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(config.getClientId())) {
            errors.add("clientId is required");
        }
        if (StringUtils.isBlank(config.getClientSecret())) {
            errors.add("clientSecret is required");
        }
        if (StringUtils.isBlank(config.getTenantId())) {
            errors.add("tenantId is required");
        }
        if (config.getScanBatchSize() < 1 || config.getScanBatchSize() > 100) {
            errors.add("scanBatchSize must be between 1 and 100");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid connector configuration: " + String.join("; ", errors));
        }
    }
}
