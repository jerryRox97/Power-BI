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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses M (Power Query) expressions to extract data source references,
 * SQL queries, table names, and connection details.
 *
 * Handles common M patterns:
 * - Value.NativeQuery(..., "SELECT * FROM schema.table", ...)
 * - Sql.Database("server", "database")
 * - Sql.Databases("server")
 * - Oracle.Database("server", [Query="SELECT ..."])
 * - Odbc.DataSource("dsn", [Query="SELECT ..."])
 * - Web.Contents("url")
 * - Excel.Workbook(Web.Contents("url"))
 * - SharePoint.Tables("url")
 * - Databricks.Catalogs("host", "warehouse", [Catalog="cat"])
 * - Table.FromRows(...) (inline/hardcoded data)
 * - #"Navigation Step" patterns
 */
public final class MExpressionParser {

    private MExpressionParser() {}

    private static final Pattern NATIVE_QUERY = Pattern.compile(
            "Value\\.NativeQuery\\s*\\([^,]+,\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SQL_DATABASE = Pattern.compile(
            "Sql\\.Database\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SQL_DATABASES = Pattern.compile(
            "Sql\\.Databases\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ORACLE_DATABASE = Pattern.compile(
            "Oracle\\.Database\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ODBC_DATASOURCE = Pattern.compile(
            "Odbc\\.DataSource\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WEB_CONTENTS = Pattern.compile(
            "Web\\.Contents\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SHAREPOINT_TABLES = Pattern.compile(
            "SharePoint\\.Tables\\s*\\(\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXCEL_WORKBOOK = Pattern.compile(
            "Excel\\.Workbook\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATABRICKS_CATALOGS = Pattern.compile(
            "Databricks\\.Catalogs\\s*\\(([^,]+),\\s*([^,]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CATALOG_PARAM = Pattern.compile(
            "Catalog\\s*=\\s*\"?([^\"\\],}]+)\"?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TABLE_FROM_ROWS = Pattern.compile(
            "Table\\.FromRows\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SQL_FROM_TABLE = Pattern.compile(
            "(?:FROM|JOIN)\\s+([\\w.]+(?:\\s+(?:AS\\s+)?\\w+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern QUERY_OPTION = Pattern.compile(
            "\\[\\s*Query\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Result of parsing an M expression.
     */
    @Getter
    @Builder
    @ToString
    public static class ParseResult {
        private final String sourceType;
        private final String server;
        private final String database;
        private final String catalog;
        private final String url;
        private final String sqlQuery;
        private final List<String> referencedTables;
        private final boolean inlineData;
    }

    /**
     * Parses an M expression and extracts source references.
     */
    public static ParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return emptyResult();
        }

        ParseResult.ParseResultBuilder builder = ParseResult.builder()
                .referencedTables(new ArrayList<>())
                .inlineData(false);

        if (parseDatabricks(expression, builder)) return builder.build();
        if (parseSqlDatabase(expression, builder)) return builder.build();
        if (parseOracleDatabase(expression, builder)) return builder.build();
        if (parseOdbcDatasource(expression, builder)) return builder.build();
        if (parseWebContents(expression, builder)) return builder.build();
        if (parseSharePoint(expression, builder)) return builder.build();
        if (parseTableFromRows(expression, builder)) return builder.build();
        if (parseNativeQuery(expression, builder)) return builder.build();

        return emptyResult();
    }

    /**
     * Extracts SQL query text from an M expression if present.
     */
    public static String extractSqlQuery(String expression) {
        if (expression == null) return null;

        Matcher nq = NATIVE_QUERY.matcher(expression);
        if (nq.find()) return nq.group(1).trim();

        Matcher qo = QUERY_OPTION.matcher(expression);
        if (qo.find()) return qo.group(1).trim();

        return null;
    }

    /**
     * Extracts table names referenced in SQL within the M expression.
     */
    public static List<String> extractReferencedTables(String expression) {
        String sql = extractSqlQuery(expression);
        if (sql == null) return Collections.emptyList();
        return extractTablesFromSql(sql);
    }

    private static boolean parseDatabricks(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = DATABRICKS_CATALOGS.matcher(expr);
        if (!m.find()) return false;

        b.sourceType("Databricks");
        String host = cleanParamRef(m.group(1));
        b.server(host);

        Matcher catM = CATALOG_PARAM.matcher(expr);
        if (catM.find()) {
            b.catalog(cleanParamRef(catM.group(1)));
        }

        String sql = extractSqlQuery(expr);
        if (sql != null) {
            b.sqlQuery(sql);
            b.referencedTables(extractTablesFromSql(sql));
        }
        return true;
    }

    private static boolean parseSqlDatabase(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = SQL_DATABASE.matcher(expr);
        if (!m.find()) {
            Matcher m2 = SQL_DATABASES.matcher(expr);
            if (!m2.find()) return false;
            b.sourceType("SqlServer");
            b.server(m2.group(1));
            extractQueryOption(expr, b);
            return true;
        }
        b.sourceType("SqlServer");
        b.server(m.group(1));
        b.database(m.group(2));
        extractQueryOption(expr, b);
        return true;
    }

    private static boolean parseOracleDatabase(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = ORACLE_DATABASE.matcher(expr);
        if (!m.find()) return false;
        b.sourceType("Oracle");
        b.server(m.group(1));
        extractQueryOption(expr, b);
        return true;
    }

    private static boolean parseOdbcDatasource(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = ODBC_DATASOURCE.matcher(expr);
        if (!m.find()) return false;
        b.sourceType("ODBC");
        b.server(m.group(1));
        extractQueryOption(expr, b);
        return true;
    }

    private static boolean parseWebContents(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = WEB_CONTENTS.matcher(expr);
        if (!m.find()) return false;
        b.url(m.group(1));
        if (EXCEL_WORKBOOK.matcher(expr).find()) {
            b.sourceType("Excel");
        } else {
            b.sourceType("Web");
        }
        return true;
    }

    private static boolean parseSharePoint(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = SHAREPOINT_TABLES.matcher(expr);
        if (!m.find()) return false;
        b.sourceType("SharePoint");
        b.url(m.group(1));
        return true;
    }

    private static boolean parseTableFromRows(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = TABLE_FROM_ROWS.matcher(expr);
        if (!m.find()) return false;
        b.sourceType("InlineData");
        b.inlineData(true);
        return true;
    }

    private static boolean parseNativeQuery(String expr, ParseResult.ParseResultBuilder b) {
        Matcher m = NATIVE_QUERY.matcher(expr);
        if (!m.find()) return false;
        b.sourceType("NativeQuery");
        String sql = m.group(1).trim();
        b.sqlQuery(sql);
        b.referencedTables(extractTablesFromSql(sql));
        return true;
    }

    private static void extractQueryOption(String expr, ParseResult.ParseResultBuilder b) {
        String sql = extractSqlQuery(expr);
        if (sql != null) {
            b.sqlQuery(sql);
            b.referencedTables(extractTablesFromSql(sql));
        }
    }

    private static List<String> extractTablesFromSql(String sql) {
        List<String> tables = new ArrayList<>();
        Matcher m = SQL_FROM_TABLE.matcher(sql);
        while (m.find()) {
            String table = m.group(1).trim();
            String[] parts = table.split("\\s+");
            tables.add(parts[0]);
        }
        return tables;
    }

    private static String cleanParamRef(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static ParseResult emptyResult() {
        return ParseResult.builder()
                .referencedTables(Collections.emptyList())
                .inlineData(false)
                .build();
    }
}
