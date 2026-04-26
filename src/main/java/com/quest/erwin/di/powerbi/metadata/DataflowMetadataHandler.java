/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.quest.erwin.di.powerbi.model.Dataflow;
import com.quest.erwin.di.powerbi.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles dataflow metadata extraction.
 */
public class DataflowMetadataHandler {

    private static final Logger log = LoggerFactory.getLogger(DataflowMetadataHandler.class);

    public void handle(Dataflow dataflow, Workspace workspace, MetadataResult result) {
        log.debug("Processing dataflow: {} ({}) in workspace: {}",
                dataflow.getName(), dataflow.getObjectId(), workspace.getName());
        result.addDataflow(dataflow);
    }
}
