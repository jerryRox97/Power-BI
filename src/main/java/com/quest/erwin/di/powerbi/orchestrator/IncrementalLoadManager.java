/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.orchestrator;

import com.quest.erwin.di.powerbi.util.JsonUtil;
import com.quest.erwin.di.powerbi.util.TimeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages incremental load state by persisting last run timestamp.
 * Reads/writes a simple JSON state file to track execution history.
 */
public class IncrementalLoadManager {

    private static final Logger log = LoggerFactory.getLogger(IncrementalLoadManager.class);
    private static final String STATE_FILE = "incremental_state.json";

    private final File stateFile;

    public IncrementalLoadManager(String outputDirectory) {
        this.stateFile = new File(outputDirectory, STATE_FILE);
    }

    /**
     * Reads the last run time from the state file. Returns empty string if not found.
     */
    public String getLastRunTime() {
        if (!stateFile.exists()) {
            log.info("No incremental state file found, performing full load");
            return "";
        }
        try {
            String content = Files.readString(stateFile.toPath());
            var json = JsonUtil.parse(content);
            String lastRun = json.has("lastRunTime") ? json.get("lastRunTime").asText("") : "";
            log.info("Last run time from state file: {}", lastRun);
            return lastRun;
        } catch (IOException e) {
            log.warn("Failed to read incremental state file: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Records the current run time in the state file for next incremental load.
     */
    public void recordRunTime() {
        try {
            stateFile.getParentFile().mkdirs();
            ObjectNode state = JsonUtil.createObject();
            state.put("lastRunTime", TimeUtil.nowIso());
            state.put("status", "completed");
            JsonUtil.writeToFile(state, stateFile);
            log.info("Incremental state recorded: {}", stateFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to write incremental state file: {}", e.getMessage());
        }
    }
}
