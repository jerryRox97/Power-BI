/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.parser.powerquery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Walks a Power Query M parse tree (produced by the ANTLR4 PowerQueryParser)
 * and collects lineage-relevant references:
 * <ul>
 *   <li>Function invocations (Sql.Database, Table.SelectRows, etc.)</li>
 *   <li>Data source connection details (server, database, URL)</li>
 *   <li>Let-expression step names and their assignments</li>
 *   <li>Field access references (record field navigation)</li>
 *   <li>Identifier references (step references within let blocks)</li>
 *   <li>String literals (for extracting SQL queries, server names, etc.)</li>
 * </ul>
 */
public class PowerQueryLineageVisitor extends PowerQueryParserBaseVisitor<Void> {

    /** Represents a function invocation with arguments captured as text. */
    public record FunctionInvocation(String functionName, List<String> arguments) {}

    /** Represents a let-expression variable (step) assignment. */
    public record LetStep(String stepName, String expressionText) {}

    /** Represents a data source reference extracted from M expressions. */
    public record DataSourceRef(String sourceType, String server,
                                String database, String url) {}

    private final Set<FunctionInvocation> functionInvocations = new LinkedHashSet<>();
    private final List<LetStep> letSteps = new ArrayList<>();
    private final Set<String> referencedIdentifiers = new LinkedHashSet<>();
    private final Set<String> fieldAccessNames = new LinkedHashSet<>();
    private final Set<String> stringLiterals = new LinkedHashSet<>();
    private final Set<DataSourceRef> dataSourceRefs = new LinkedHashSet<>();
    private final Map<String, String> stepExpressions = new LinkedHashMap<>();

    // -- Accessors --------------------------------------------------------

    public Set<FunctionInvocation> getFunctionInvocations() {
        return functionInvocations;
    }

    public List<LetStep> getLetSteps() {
        return letSteps;
    }

    public Set<String> getReferencedIdentifiers() {
        return referencedIdentifiers;
    }

    public Set<String> getFieldAccessNames() {
        return fieldAccessNames;
    }

    public Set<String> getStringLiterals() {
        return stringLiterals;
    }

    public Set<DataSourceRef> getDataSourceRefs() {
        return dataSourceRefs;
    }

    public Map<String, String> getStepExpressions() {
        return stepExpressions;
    }

    // -- Visitor Overrides ------------------------------------------------

    @Override
    public Void visitLetExpression(PowerQueryParser.LetExpressionContext ctx) {
        if (ctx.variableList() != null) {
            for (PowerQueryParser.VariableContext varCtx : ctx.variableList().variable()) {
                String stepName = extractIdentifierText(varCtx.variableName());
                String exprText = varCtx.expression() != null ? varCtx.expression().getText() : "";
                letSteps.add(new LetStep(stepName, exprText));
                stepExpressions.put(stepName, exprText);
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitInvokeExpr(PowerQueryParser.InvokeExprContext ctx) {
        String funcName = ctx.primaryExpression().getText();
        List<String> args = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (PowerQueryParser.ExpressionContext argExpr : ctx.argumentList().expression()) {
                args.add(argExpr.getText());
            }
        }
        functionInvocations.add(new FunctionInvocation(funcName, args));
        extractDataSourceFromInvocation(funcName, args);
        return visitChildren(ctx);
    }

    @Override
    public Void visitFieldAccessExpr(PowerQueryParser.FieldAccessExprContext ctx) {
        if (ctx.fieldSelector() != null && ctx.fieldSelector().fieldName() != null) {
            String fieldName = extractFieldNameText(ctx.fieldSelector().fieldName());
            fieldAccessNames.add(fieldName);
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitIdentExpr(PowerQueryParser.IdentExprContext ctx) {
        if (ctx.identifierExpression() != null) {
            String idText = ctx.identifierExpression().getText();
            referencedIdentifiers.add(stripQuotedIdentifier(idText));
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitLitExpr(PowerQueryParser.LitExprContext ctx) {
        if (ctx.literalExpression() != null) {
            String text = ctx.literalExpression().getText();
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
                stringLiterals.add(text.substring(1, text.length() - 1));
            }
        }
        return visitChildren(ctx);
    }

    // -- Data Source Extraction --------------------------------------------

    private void extractDataSourceFromInvocation(String funcName, List<String> args) {
        String upperFunc = funcName.replace(".", ".").trim();
        switch (upperFunc) {
            case "Sql.Database" -> extractSqlDatabase(args);
            case "Sql.Databases" -> extractSqlDatabases(args);
            case "Oracle.Database" -> extractOracleDatabase(args);
            case "Odbc.DataSource" -> extractOdbcDataSource(args);
            case "Web.Contents" -> extractWebContents(args);
            case "Excel.Workbook" -> extractExcelWorkbook(args);
            case "Csv.Document" -> extractCsvDocument(args);
            case "OData.Feed" -> extractODataFeed(args);
            case "SharePoint.Tables" -> extractSharePointTables(args);
            case "Databricks.Catalogs" -> extractDatabricksCatalogs(args);
            default -> { /* not a known data source connector */ }
        }
    }

    private void extractSqlDatabase(List<String> args) {
        String server = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        String database = args.size() > 1 ? stripStringLiteral(args.get(1)) : "";
        dataSourceRefs.add(new DataSourceRef("SqlServer", server, database, ""));
    }

    private void extractSqlDatabases(List<String> args) {
        String server = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("SqlServer", server, "", ""));
    }

    private void extractOracleDatabase(List<String> args) {
        String server = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("Oracle", server, "", ""));
    }

    private void extractOdbcDataSource(List<String> args) {
        String dsn = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("ODBC", dsn, "", ""));
    }

    private void extractWebContents(List<String> args) {
        String url = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("Web", "", "", url));
    }

    private void extractExcelWorkbook(List<String> args) {
        String source = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("Excel", "", "", source));
    }

    private void extractCsvDocument(List<String> args) {
        String source = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("CSV", "", "", source));
    }

    private void extractODataFeed(List<String> args) {
        String url = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("OData", "", "", url));
    }

    private void extractSharePointTables(List<String> args) {
        String url = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("SharePoint", "", "", url));
    }

    private void extractDatabricksCatalogs(List<String> args) {
        String host = args.size() > 0 ? stripStringLiteral(args.get(0)) : "";
        dataSourceRefs.add(new DataSourceRef("Databricks", host, "", ""));
    }

    // -- Helpers -----------------------------------------------------------

    private static String extractIdentifierText(PowerQueryParser.VariableNameContext ctx) {
        if (ctx == null || ctx.identifier() == null) return "";
        return stripQuotedIdentifier(ctx.identifier().getText());
    }

    private static String extractFieldNameText(PowerQueryParser.FieldNameContext ctx) {
        if (ctx == null) return "";
        String text = ctx.getText();
        return stripQuotedIdentifier(text);
    }

    private static String stripQuotedIdentifier(String id) {
        if (id == null) return "";
        if (id.startsWith("#\"") && id.endsWith("\"")) {
            return id.substring(2, id.length() - 1);
        }
        if (id.startsWith("@")) {
            return id.substring(1);
        }
        return id;
    }

    private static String stripStringLiteral(String literal) {
        if (literal == null) return "";
        String trimmed = literal.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
