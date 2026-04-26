/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.util;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses DAX expressions (measures and calculated columns) to extract
 * table and column references for lineage tracking.
 *
 * Handles common DAX patterns:
 * - Table[Column] references
 * - 'Table Name'[Column] (quoted table names with spaces)
 * - RELATED('Table'[Column])
 * - RELATEDTABLE('Table')
 * - CALCULATE, FILTER, SUM, AVERAGE, etc. with table/column args
 * - VAR declarations
 * - MEASURE references like [MeasureName]
 */
public final class DaxExpressionParser {

    private DaxExpressionParser() {}

    private static final Pattern TABLE_COLUMN_REF = Pattern.compile(
            "'([^']+)'\\[([^\\]]+)]");

    private static final Pattern UNQUOTED_TABLE_COLUMN_REF = Pattern.compile(
            "(?<![\\w'])([A-Za-z_]\\w*)\\[([^\\]]+)]");

    private static final Pattern MEASURE_REF = Pattern.compile(
            "(?<![\\w'])\\[([^\\]]+)]");

    private static final Pattern RELATED_TABLE = Pattern.compile(
            "RELATEDTABLE\\s*\\(\\s*'?([^'()]+)'?\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    /**
     * A reference found in a DAX expression.
     */
    @Getter
    @Builder
    @ToString
    public static class DaxReference {

        public enum RefType {
            TABLE_COLUMN,
            MEASURE,
            TABLE_ONLY
        }

        private final RefType refType;
        private final String tableName;
        private final String columnName;
    }

    /**
     * Result of parsing a DAX expression.
     */
    @Getter
    @Builder
    @ToString
    public static class ParseResult {
        private final List<DaxReference> references;
        private final List<String> referencedTables;
        private final List<String> referencedColumns;
        private final List<String> referencedMeasures;
    }

    /**
     * Parses a DAX expression and extracts all table/column/measure references.
     */
    public static ParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return emptyResult();
        }

        List<DaxReference> refs = new ArrayList<>();
        Set<String> tables = new LinkedHashSet<>();
        Set<String> columns = new LinkedHashSet<>();
        Set<String> measures = new LinkedHashSet<>();

        extractQuotedTableColumnRefs(expression, refs, tables, columns);
        extractUnquotedTableColumnRefs(expression, refs, tables, columns);
        extractRelatedTableRefs(expression, refs, tables);
        extractMeasureRefs(expression, refs, measures);

        return ParseResult.builder()
                .references(refs)
                .referencedTables(new ArrayList<>(tables))
                .referencedColumns(new ArrayList<>(columns))
                .referencedMeasures(new ArrayList<>(measures))
                .build();
    }

    /**
     * Extracts just the table names referenced in a DAX expression.
     */
    public static List<String> extractReferencedTables(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> tables = new LinkedHashSet<>();
        extractQuotedTableNames(expression, tables);
        extractUnquotedTableNames(expression, tables);
        return new ArrayList<>(tables);
    }

    /**
     * Extracts table[column] pairs from a DAX expression.
     */
    public static List<String> extractTableColumnPairs(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        List<String> pairs = new ArrayList<>();

        Matcher m1 = TABLE_COLUMN_REF.matcher(expression);
        while (m1.find()) {
            pairs.add(m1.group(1) + "." + m1.group(2));
        }

        Matcher m2 = UNQUOTED_TABLE_COLUMN_REF.matcher(expression);
        while (m2.find()) {
            String table = m2.group(1);
            if (!isDaxKeyword(table)) {
                pairs.add(table + "." + m2.group(2));
            }
        }

        return pairs;
    }

    private static void extractQuotedTableColumnRefs(String expr,
            List<DaxReference> refs, Set<String> tables, Set<String> columns) {
        Matcher m = TABLE_COLUMN_REF.matcher(expr);
        while (m.find()) {
            String table = m.group(1);
            String column = m.group(2);
            refs.add(DaxReference.builder()
                    .refType(DaxReference.RefType.TABLE_COLUMN)
                    .tableName(table)
                    .columnName(column)
                    .build());
            tables.add(table);
            columns.add(table + "." + column);
        }
    }

    private static void extractUnquotedTableColumnRefs(String expr,
            List<DaxReference> refs, Set<String> tables, Set<String> columns) {
        Matcher m = UNQUOTED_TABLE_COLUMN_REF.matcher(expr);
        while (m.find()) {
            String table = m.group(1);
            if (isDaxKeyword(table)) continue;
            String column = m.group(2);
            refs.add(DaxReference.builder()
                    .refType(DaxReference.RefType.TABLE_COLUMN)
                    .tableName(table)
                    .columnName(column)
                    .build());
            tables.add(table);
            columns.add(table + "." + column);
        }
    }

    private static void extractRelatedTableRefs(String expr,
            List<DaxReference> refs, Set<String> tables) {
        Matcher m = RELATED_TABLE.matcher(expr);
        while (m.find()) {
            String table = m.group(1).trim();
            refs.add(DaxReference.builder()
                    .refType(DaxReference.RefType.TABLE_ONLY)
                    .tableName(table)
                    .build());
            tables.add(table);
        }
    }

    private static void extractMeasureRefs(String expr,
            List<DaxReference> refs, Set<String> measures) {
        Matcher m = MEASURE_REF.matcher(expr);
        while (m.find()) {
            int start = m.start();
            if (start > 0) {
                char prev = expr.charAt(start - 1);
                if (prev == '\'' || Character.isLetterOrDigit(prev) || prev == '_') {
                    continue;
                }
            }
            String name = m.group(1);
            refs.add(DaxReference.builder()
                    .refType(DaxReference.RefType.MEASURE)
                    .columnName(name)
                    .build());
            measures.add(name);
        }
    }

    private static void extractQuotedTableNames(String expr, Set<String> tables) {
        Matcher m = TABLE_COLUMN_REF.matcher(expr);
        while (m.find()) {
            tables.add(m.group(1));
        }
    }

    private static void extractUnquotedTableNames(String expr, Set<String> tables) {
        Matcher m = UNQUOTED_TABLE_COLUMN_REF.matcher(expr);
        while (m.find()) {
            String table = m.group(1);
            if (!isDaxKeyword(table)) {
                tables.add(table);
            }
        }
    }

    private static boolean isDaxKeyword(String word) {
        return switch (word.toUpperCase()) {
            case "CALCULATE", "CALCULATETABLE", "FILTER", "ALL", "ALLEXCEPT",
                 "ALLSELECTED", "VALUES", "DISTINCT", "SUMMARIZE", "SUMMARIZECOLUMNS",
                 "ADDCOLUMNS", "SELECTCOLUMNS", "TOPN", "GENERATE", "GENERATEALL",
                 "CROSSJOIN", "NATURALLEFTOUTERJOIN", "NATURALINNERJOIN",
                 "UNION", "INTERSECT", "EXCEPT", "DATATABLE", "GENERATESERIES",
                 "ROW", "TREATAS", "GROUPBY", "ROLLUPGROUP", "ROLLUP",
                 "SUM", "SUMX", "AVERAGE", "AVERAGEX", "COUNT", "COUNTX",
                 "COUNTA", "COUNTAX", "COUNTROWS", "COUNTBLANK",
                 "MIN", "MINX", "MAX", "MAXX", "DIVIDE",
                 "IF", "SWITCH", "AND", "OR", "NOT", "TRUE", "FALSE", "BLANK",
                 "RELATED", "RELATEDTABLE", "LOOKUPVALUE", "USERELATIONSHIP",
                 "EARLIER", "EARLIEST", "VAR", "RETURN", "IN",
                 "YEAR", "MONTH", "DAY", "TODAY", "NOW", "DATE", "HOUR",
                 "MINUTE", "SECOND", "CALENDAR", "CALENDARAUTO",
                 "FORMAT", "CONCATENATE", "CONCAT", "LEFT", "RIGHT", "MID",
                 "LEN", "TRIM", "UPPER", "LOWER", "SUBSTITUTE", "REPLACE",
                 "FIND", "SEARCH", "EXACT", "FIXED", "REPT", "UNICODE", "UNICHAR",
                 "ABS", "ROUND", "ROUNDUP", "ROUNDDOWN", "INT", "MOD",
                 "POWER", "SQRT", "LOG", "LOG10", "EXP", "FACT",
                 "ISBLANK", "ISERROR", "ISLOGICAL", "ISNONTEXT", "ISNUMBER", "ISTEXT",
                 "HASONEVALUE", "HASONEFILTER", "ISFILTERED", "ISCROSSFILTERED",
                 "SELECTEDVALUE", "FIRSTDATE", "LASTDATE",
                 "DATESYTD", "DATESMTD", "DATESQTD",
                 "TOTALYTD", "TOTALMTD", "TOTALQTD",
                 "SAMEPERIODLASTYEAR", "DATEADD", "DATESINPERIOD",
                 "PREVIOUSDAY", "PREVIOUSMONTH", "PREVIOUSQUARTER", "PREVIOUSYEAR",
                 "NEXTDAY", "NEXTMONTH", "NEXTQUARTER", "NEXTYEAR",
                 "PARALLELPERIOD", "OPENINGBALANCEMONTH", "CLOSINGBALANCEMONTH",
                 "OPENINGBALANCEQUARTER", "CLOSINGBALANCEQUARTER",
                 "OPENINGBALANCEYEAR", "CLOSINGBALANCEYEAR",
                 "RANKX", "PERCENTILEX", "PERCENTILE", "MEDIAN", "MEDIANX",
                 "STDEV", "STDEVX", "PRODUCT", "PRODUCTX",
                 "PATHCONTAINS", "PATHITEM", "PATHLENGTH", "PATH",
                 "USERCULTURE", "USERNAME", "USERPRINCIPALNAME",
                 "CONTAINSSTRING", "CONTAINSSTRINGEXACT",
                 "COMBINEVALUES", "COALESCE", "CONVERT",
                 "SELECTEDMEASURE", "SELECTEDMEASURENAME",
                 "ALLNOBLANKROW", "KEEPFILTERS", "REMOVEFILTERS" -> true;
            default -> false;
        };
    }

    private static ParseResult emptyResult() {
        return ParseResult.builder()
                .references(Collections.emptyList())
                .referencedTables(Collections.emptyList())
                .referencedColumns(Collections.emptyList())
                .referencedMeasures(Collections.emptyList())
                .build();
    }
}
