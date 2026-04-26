/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.metadata;

import com.quest.erwin.di.powerbi.model.Column;
import com.quest.erwin.di.powerbi.model.Dataset;
import com.quest.erwin.di.powerbi.model.Measure;
import com.quest.erwin.di.powerbi.model.Table;
import com.quest.erwin.di.powerbi.model.TableSource;
import com.quest.erwin.di.powerbi.model.Workspace;
import com.quest.erwin.di.powerbi.util.DaxExpressionParser;
import com.quest.erwin.di.powerbi.util.MExpressionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles dataset metadata extraction: tables, columns, measures, expressions.
 * Parses M (Power Query) table source expressions and DAX measure/column expressions
 * to extract referenced tables, SQL queries, and data source details.
 */
public class DatasetMetadataHandler {

    private static final Logger log = LoggerFactory.getLogger(DatasetMetadataHandler.class);

    public void handle(Dataset dataset, Workspace workspace, MetadataResult result) {
        log.debug("Processing dataset: {} ({}) in workspace: {}",
                dataset.getName(), dataset.getId(), workspace.getName());

        result.addDataset(dataset);

        int tableCount = 0;
        int columnCount = 0;
        int measureCount = 0;
        int sourceCount = 0;
        int calcColumnCount = 0;

        if (dataset.getTables() != null) {
            for (Table table : dataset.getTables()) {
                tableCount++;
                sourceCount += countAndLogTableSources(table, dataset.getName());
                columnCount += countColumns(table);
                calcColumnCount += countAndLogCalcColumns(table, dataset.getName());
                measureCount += countAndLogMeasures(table, dataset.getName());
            }
        }

        int exprCount = dataset.getExpressions() != null ? dataset.getExpressions().size() : 0;

        log.debug("Dataset {} -> {} table(s), {} column(s) ({} calculated), {} measure(s), {} source(s), {} expression(s)",
                dataset.getName(), tableCount, columnCount, calcColumnCount,
                measureCount, sourceCount, exprCount);
    }

    private int countAndLogTableSources(Table table, String datasetName) {
        if (table.getSource() == null) return 0;
        int count = 0;
        for (TableSource src : table.getSource()) {
            count++;
            if (src.getExpression() != null && log.isTraceEnabled()) {
                MExpressionParser.ParseResult parsed = MExpressionParser.parse(src.getExpression());
                if (parsed.getSourceType() != null) {
                    log.trace("Table {}.{} source: type={}, server={}, sql={}",
                            datasetName, table.getName(), parsed.getSourceType(),
                            parsed.getServer(), parsed.getSqlQuery() != null ? "yes" : "no");
                    List<String> refs = parsed.getReferencedTables();
                    if (!refs.isEmpty()) {
                        log.trace("  Referenced SQL tables: {}", refs);
                    }
                }
            }
        }
        return count;
    }

    private int countColumns(Table table) {
        return table.getColumns() != null ? table.getColumns().size() : 0;
    }

    private int countAndLogCalcColumns(Table table, String datasetName) {
        if (table.getColumns() == null) return 0;
        int count = 0;
        for (Column col : table.getColumns()) {
            if (col.getExpression() != null) {
                count++;
                if (log.isTraceEnabled()) {
                    DaxExpressionParser.ParseResult parsed = DaxExpressionParser.parse(col.getExpression());
                    if (!parsed.getReferencedTables().isEmpty()) {
                        log.trace("Calc column {}.{}.{} references tables: {}",
                                datasetName, table.getName(), col.getName(),
                                parsed.getReferencedTables());
                    }
                }
            }
        }
        return count;
    }

    private int countAndLogMeasures(Table table, String datasetName) {
        if (table.getMeasures() == null) return 0;
        int count = table.getMeasures().size();
        if (log.isTraceEnabled()) {
            for (Measure m : table.getMeasures()) {
                if (m.getExpression() != null) {
                    DaxExpressionParser.ParseResult parsed = DaxExpressionParser.parse(m.getExpression());
                    if (!parsed.getReferencedTables().isEmpty()) {
                        log.trace("Measure {}.{}.{} references tables: {}",
                                datasetName, table.getName(), m.getName(),
                                parsed.getReferencedTables());
                    }
                }
            }
        }
        return count;
    }
}
