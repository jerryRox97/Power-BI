/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.acp;

import com.quest.erwin.di.powerbi.auth.ClientCredentialsProvider;
import com.quest.erwin.di.powerbi.auth.TokenStore;
import com.quest.erwin.di.powerbi.client.PowerBIClient;
import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import com.quest.erwin.di.powerbi.config.ConnectorConfigValidator;
import com.quest.erwin.di.powerbi.glossary.GlossaryBuilder;
import com.quest.erwin.di.powerbi.glossary.GlossaryEntry;
import com.quest.erwin.di.powerbi.glossary.GlossaryOutputWriter;
import com.quest.erwin.di.powerbi.lineage.LineageBuilder;
import com.quest.erwin.di.powerbi.lineage.LineageGraph;
import com.quest.erwin.di.powerbi.lineage.LineageOutputWriter;
import com.quest.erwin.di.powerbi.metadata.MetadataExtractor;
import com.quest.erwin.di.powerbi.metadata.MetadataOutputWriter;
import com.quest.erwin.di.powerbi.metadata.MetadataResult;
import com.quest.erwin.di.powerbi.model.ScanResultResponse;
import com.quest.erwin.di.powerbi.orchestrator.ConnectorOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * ACP (Adapter Connector Package) entry point.
 * This class is the bridge between the erwin DI platform and the connector logic.
 * The platform calls {@link #execute(Map)} with connector options from the UI.
 */
public class AcpConnectorAdapter {

    private static final Logger log = LoggerFactory.getLogger(AcpConnectorAdapter.class);

    private final AcpConfigMapper configMapper;

    public AcpConnectorAdapter() {
        this.configMapper = new AcpConfigMapper();
    }

    /**
     * Main entry point called by the erwin DI platform.
     * Receives connector options as key-value pairs and runs the full workflow.
     *
     * @param options connector options from the ACP UI
     */
    public void execute(Map<String, String> options) {
        log.info("ACP Connector Adapter: execution started");

        try {
            ConnectorConfig config = configMapper.map(options);
            ConnectorOrchestrator orchestrator = new ConnectorOrchestrator(config);
            orchestrator.run();

            log.info("ACP Connector Adapter: execution completed successfully");
        } catch (Exception e) {
            log.error("ACP Connector Adapter: execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Power BI connector execution failed", e);
        }
    }

    /**
     * Validates connector options without executing.
     * Called by the erwin DI platform during connector configuration.
     */
    public boolean validate(Map<String, String> options) {
        try {
            ConnectorConfig config = configMapper.map(options);
            ConnectorConfigValidator.validate(config);
            return true;
        } catch (Exception e) {
            log.warn("Validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Tests connectivity with the provided credentials.
     * Called by the erwin DI platform "Test Connection" button.
     */
    public boolean testConnection(Map<String, String> options) {
        try {
            ConnectorConfig config = configMapper.map(options);
            ConnectorConfigValidator.validate(config);

            ClientCredentialsProvider provider = new ClientCredentialsProvider(
                    config.getTenantId(), config.getClientId(), config.getClientSecret());
            TokenStore tokenStore = new TokenStore(provider);
            PowerBIClient client = new PowerBIClient(tokenStore);
            client.getAccessToken();

            log.info("Connection test successful");
            return true;
        } catch (Exception e) {
            log.warn("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
