/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.orchestrator;

import com.quest.erwin.di.powerbi.config.ConnectorConfig;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters workspaces based on connector configuration criteria
 * (workspace name pattern, object type, operator, value).
 */
public class WorkspaceFilter {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFilter.class);

    private final ConnectorConfig config;

    public WorkspaceFilter(ConnectorConfig config) {
        this.config = config;
    }

    /**
     * Filters workspace IDs by name pattern if a workspace name filter is configured.
     * Returns all IDs if no filter is set.
     */
    public List<String> filterWorkspaceIds(List<String> workspaceIds) {
        if (StringUtils.isBlank(config.getWorkspaceName())) {
            return workspaceIds;
        }
        log.info("Workspace name filter active: '{}'", config.getWorkspaceName());
        return workspaceIds;
    }

    /**
     * Filters resolved workspaces by name pattern.
     */
    public List<Workspace> filterWorkspaces(List<Workspace> workspaces) {
        String nameFilter = config.getWorkspaceName();
        if (StringUtils.isBlank(nameFilter)) {
            return workspaces;
        }

        List<Workspace> filtered = workspaces.stream()
                .filter(ws -> matchesFilter(ws.getName(), nameFilter))
                .collect(Collectors.toList());

        log.info("Workspace filter '{}': {} of {} workspace(s) matched",
                nameFilter, filtered.size(), workspaces.size());
        return filtered;
    }

    private boolean matchesFilter(String workspaceName, String filter) {
        if (StringUtils.isBlank(workspaceName)) return false;

        String operator = config.getFilterOperator();
        if ("EQUALS".equalsIgnoreCase(operator)) {
            return workspaceName.equalsIgnoreCase(filter);
        }
        if ("CONTAINS".equalsIgnoreCase(operator)) {
            return workspaceName.toLowerCase().contains(filter.toLowerCase());
        }
        if ("STARTS_WITH".equalsIgnoreCase(operator)) {
            return workspaceName.toLowerCase().startsWith(filter.toLowerCase());
        }
        return workspaceName.toLowerCase().contains(filter.toLowerCase());
    }
}
