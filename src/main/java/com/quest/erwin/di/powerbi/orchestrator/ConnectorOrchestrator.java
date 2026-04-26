/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.orchestrator;

import com.quest.erwin.di.powerbi.api.ScanApi;
import com.quest.erwin.di.powerbi.api.WorkspaceApi;
import com.quest.erwin.di.powerbi.auth.ClientCredentialsProvider;
import com.quest.erwin.di.powerbi.auth.TokenProvider;
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
import com.quest.erwin.di.powerbi.scan.TenantScanner;
import com.quest.erwin.di.powerbi.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main orchestrator for the Power BI connector workflow.
 * Coordinates all phases: authenticate, scan, extract metadata, build lineage, create glossary, write output.
 */
public class ConnectorOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ConnectorOrchestrator.class);

    private final ConnectorConfig config;

    public ConnectorOrchestrator(ConnectorConfig config) {
        this.config = config;
    }

    /**
     * Executes the full connector workflow:
     * 1. Validate configuration
     * 2. Authenticate with Azure AD
     * 3. Scan workspaces (full or incremental)
     * 4. Extract metadata
     * 5. Build lineage graph
     * 6. Create business glossary
     * 7. Write all outputs
     * 8. Record execution state
     */
    public void run() {
        long startTime = System.currentTimeMillis();
        log.info("=== Power BI Connector v2 Starting ===");

        validateConfig();
        PowerBIClient client = authenticate();
        ScanResultResponse scanResult = scanWorkspaces(client);
        MetadataResult metadata = extractMetadata(scanResult);
        LineageGraph lineage = buildLineage(metadata);
        List<GlossaryEntry> glossary = buildGlossary(metadata);
        writeOutputs(metadata, lineage, glossary);
        recordState();

        log.info("=== Power BI Connector v2 Complete ({}) ===", TimeUtil.formatDuration(startTime));
    }

    private void validateConfig() {
        log.info("Step 1: Validating configuration");
        ConnectorConfigValidator.validate(config);
        log.info("Configuration validated successfully");
    }

    private PowerBIClient authenticate() {
        log.info("Step 2: Authenticating with Azure AD");
        TokenProvider provider = new ClientCredentialsProvider(
                config.getTenantId(), config.getClientId(), config.getClientSecret());
        TokenStore tokenStore = new TokenStore(provider);
        PowerBIClient client = new PowerBIClient(tokenStore);
        client.getAccessToken();
        log.info("Authentication successful");
        return client;
    }

    private ScanResultResponse scanWorkspaces(PowerBIClient client) {
        log.info("Step 3: Scanning workspaces");
        WorkspaceApi workspaceApi = new WorkspaceApi(client);
        ScanApi scanApi = new ScanApi(client);

        ConnectorConfig scanConfig = resolveIncrementalConfig();
        TenantScanner scanner = new TenantScanner(workspaceApi, scanApi, scanConfig);
        ScanResultResponse scanResult = scanner.scan();

        WorkspaceFilter filter = new WorkspaceFilter(config);
        scanResult.setWorkspaces(filter.filterWorkspaces(scanResult.getWorkspaces()));

        log.info("Scan complete: {} workspace(s), {} datasource(s)",
                scanResult.getWorkspaces().size(),
                scanResult.getDatasourceInstances() != null ? scanResult.getDatasourceInstances().size() : 0);
        return scanResult;
    }

    private MetadataResult extractMetadata(ScanResultResponse scanResult) {
        log.info("Step 4: Extracting metadata");
        MetadataExtractor extractor = new MetadataExtractor();
        return extractor.extract(scanResult);
    }

    private LineageGraph buildLineage(MetadataResult metadata) {
        log.info("Step 5: Building lineage graph");
        LineageBuilder builder = new LineageBuilder();
        return builder.build(metadata);
    }

    private List<GlossaryEntry> buildGlossary(MetadataResult metadata) {
        log.info("Step 6: Creating business glossary");
        GlossaryBuilder builder = new GlossaryBuilder();
        return builder.build(metadata);
    }

    private void writeOutputs(MetadataResult metadata, LineageGraph lineage, List<GlossaryEntry> glossary) {
        log.info("Step 7: Writing outputs to {}", config.getOutputDirectory());
        try {
            MetadataOutputWriter metadataWriter = new MetadataOutputWriter(config.getOutputDirectory());
            metadataWriter.write(metadata);

            LineageOutputWriter lineageWriter = new LineageOutputWriter(config.getOutputDirectory());
            lineageWriter.write(lineage);

            GlossaryOutputWriter glossaryWriter = new GlossaryOutputWriter(config.getOutputDirectory());
            glossaryWriter.write(glossary);
        } catch (Exception e) {
            log.error("Failed to write outputs: {}", e.getMessage(), e);
            throw new RuntimeException("Output writing failed", e);
        }
    }

    private void recordState() {
        log.info("Step 8: Recording execution state");
        IncrementalLoadManager loadManager = new IncrementalLoadManager(config.getOutputDirectory());
        loadManager.recordRunTime();
    }

    private ConnectorConfig resolveIncrementalConfig() {
        if (!config.isIncrementalLoad()) {
            return config;
        }
        IncrementalLoadManager loadManager = new IncrementalLoadManager(config.getOutputDirectory());
        String lastRunTime = loadManager.getLastRunTime();
        return ConnectorConfig.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .tenantId(config.getTenantId())
                .scanBatchSize(config.getScanBatchSize())
                .throttleDelayMs(config.getThrottleDelayMs())
                .incrementalLoad(true)
                .lastRunTime(lastRunTime)
                .outputDirectory(config.getOutputDirectory())
                .build();
    }
}
