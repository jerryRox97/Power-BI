/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi;

import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import com.quest.erwin.di.powerbi.orchestrator.ConnectorOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone entry point for the Power BI connector.
 * Can be run from command line or used for testing outside the ACP platform.
 * For ACP deployment, use {@link com.quest.erwin.di.powerbi.acp.AcpConnectorAdapter} instead.
 *
 * Usage: java -jar powerbi-connector-v2.jar --clientId=... --clientSecret=... --tenantId=...
 */
public class PowerBIConnectorEntry {

    private static final Logger log = LoggerFactory.getLogger(PowerBIConnectorEntry.class);

    public static void main(String[] args) {
        log.info("Power BI Connector v2 - Standalone Entry Point");

        try {
            ConnectorConfig config = parseArgs(args);
            ConnectorOrchestrator orchestrator = new ConnectorOrchestrator(config);
            orchestrator.run();
        } catch (Exception e) {
            log.error("Connector execution failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static ConnectorConfig parseArgs(String[] args) {
        ConnectorConfig.ConnectorConfigBuilder builder = ConnectorConfig.builder();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    applyArg(builder, parts[0], parts[1]);
                }
            }
        }

        return builder.build();
    }

    private static void applyArg(ConnectorConfig.ConnectorConfigBuilder builder, String key, String value) {
        switch (key) {
            case "clientId" -> builder.clientId(value);
            case "clientSecret" -> builder.clientSecret(value);
            case "tenantId" -> builder.tenantId(value);
            case "projectName" -> builder.projectName(value);
            case "workspaceName" -> builder.workspaceName(value);
            case "databaseFilePath" -> builder.databaseFilePath(value);
            case "scanBatchSize" -> builder.scanBatchSize(Integer.parseInt(value));
            case "throttleDelayMs" -> builder.throttleDelayMs(Long.parseLong(value));
            case "incrementalLoad" -> builder.incrementalLoad(Boolean.parseBoolean(value));
            case "filterObjectType" -> builder.filterObjectType(value);
            case "filterOperator" -> builder.filterOperator(value);
            case "filterValue" -> builder.filterValue(value);
            case "outputDirectory" -> builder.outputDirectory(value);
            default -> log.warn("Unknown argument: {}", key);
        }
    }
}
