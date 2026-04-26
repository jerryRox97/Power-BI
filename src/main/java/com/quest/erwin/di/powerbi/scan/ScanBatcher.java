/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.scan;

import com.quest.erwin.di.powerbi.api.ScanApi;
import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import com.quest.erwin.di.powerbi.model.ScanResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Batches workspace IDs into groups of {@code scanBatchSize} and scans them sequentially.
 * Power BI API limits scan requests to 100 workspaces per call.
 * Applies throttle delay between batches to avoid rate limiting.
 */
public class ScanBatcher {

    private static final Logger log = LoggerFactory.getLogger(ScanBatcher.class);

    private final ScanApi scanApi;
    private final int batchSize;
    private final long throttleDelayMs;

    public ScanBatcher(ScanApi scanApi, ConnectorConfig config) {
        this.scanApi = scanApi;
        this.batchSize = config.getScanBatchSize();
        this.throttleDelayMs = config.getThrottleDelayMs();
    }

    /**
     * Splits workspace IDs into batches, scans each, and aggregates results.
     */
    public ScanResultResponse scanAll(List<String> workspaceIds) {
        List<List<String>> batches = partition(workspaceIds, batchSize);
        log.info("Scanning {} workspace(s) in {} batch(es) (batchSize={})",
                workspaceIds.size(), batches.size(), batchSize);

        ScanResultResponse aggregated = new ScanResultResponse();

        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            log.info("Processing batch {}/{} ({} workspace(s))", i + 1, batches.size(), batch.size());

            ScanResultResponse batchResult = scanApi.scanAndWait(batch);
            mergeResults(aggregated, batchResult);

            if (i < batches.size() - 1) {
                throttle(i + 1, batches.size());
            }
        }

        log.info("Scan complete: {} workspace(s), {} datasource instance(s)",
                aggregated.getWorkspaces().size(), aggregated.getDatasourceInstances().size());
        return aggregated;
    }

    private void mergeResults(ScanResultResponse target, ScanResultResponse source) {
        if (source.getWorkspaces() != null) {
            target.getWorkspaces().addAll(source.getWorkspaces());
        }
        if (source.getDatasourceInstances() != null) {
            target.getDatasourceInstances().addAll(source.getDatasourceInstances());
        }
    }

    private void throttle(int currentBatch, int totalBatches) {
        if (throttleDelayMs <= 0) {
            return;
        }
        log.info("Throttling between batch {}/{}: waiting {}ms", currentBatch, totalBatches, throttleDelayMs);
        try {
            Thread.sleep(throttleDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Throttle interrupted");
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
