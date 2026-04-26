/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.parser.dax;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks a DAX parse tree (produced by the ANTLR4 DaxParser) and collects
 * lineage-relevant references:
 * <ul>
 *   <li>Table references (quoted: 'TableName' and unquoted: TableName)</li>
 *   <li>Qualified column references: 'Table'[Column] or Table[Column]</li>
 *   <li>Unqualified bracket references: [MeasureOrColumn]</li>
 *   <li>Function calls (RELATED, RELATEDTABLE, CALCULATETABLE, etc.)</li>
 *   <li>DEFINE MEASURE / TABLE / COLUMN definitions</li>
 *   <li>VAR definitions with RETURN expressions</li>
 * </ul>
 */
public class DaxLineageVisitor extends DaxParserBaseVisitor<Void> {

    /** Represents a qualified column reference: tableName + columnName. */
    public record ColumnReference(String tableName, String columnName) {}

    /** Represents a function invocation with its fully-qualified name. */
    public record FunctionReference(String functionName, List<String> argumentTableRefs) {}

    /** Represents a DEFINE MEASURE definition. */
    public record MeasureDefinitionRef(String tableName, String measureName) {}

    private final Set<String> referencedTables = new LinkedHashSet<>();
    private final Set<ColumnReference> referencedColumns = new LinkedHashSet<>();
    private final Set<String> unresolvedBracketRefs = new LinkedHashSet<>();
    private final Set<FunctionReference> functionCalls = new LinkedHashSet<>();
    private final Set<MeasureDefinitionRef> definedMeasures = new LinkedHashSet<>();
    private final Set<String> definedVars = new LinkedHashSet<>();

    // ── Accessors ────────────────────────────────────────────────────

    public Set<String> getReferencedTables() {
        return referencedTables;
    }

    public Set<ColumnReference> getReferencedColumns() {
        return referencedColumns;
    }

    public Set<String> getUnresolvedBracketRefs() {
        return unresolvedBracketRefs;
    }

    public Set<FunctionReference> getFunctionCalls() {
        return functionCalls;
    }

    public Set<MeasureDefinitionRef> getDefinedMeasures() {
        return definedMeasures;
    }

    public Set<String> getDefinedVars() {
        return definedVars;
    }

    // ── Visitor Overrides ────────────────────────────────────────────

    @Override
    public Void visitQualifiedColumnRef(DaxParser.QualifiedColumnRefContext ctx) {
        String tableName = stripTableQuotes(ctx.tableRef().getText());
        String columnName = stripBrackets(ctx.BRACKET_REF().getText());
        referencedTables.add(tableName);
        referencedColumns.add(new ColumnReference(tableName, columnName));
        return visitChildren(ctx);
    }

    @Override
    public Void visitTableRef(DaxParser.TableRefContext ctx) {
        if (ctx.getParent() instanceof DaxParser.QualifiedColumnRefContext) {
            return null;
        }
        String tableName = stripTableQuotes(ctx.getText());
        referencedTables.add(tableName);
        return visitChildren(ctx);
    }

    @Override
    public Void visitBracketRef(DaxParser.BracketRefContext ctx) {
        if (ctx.getParent() instanceof DaxParser.QualifiedColumnRefContext) {
            return null;
        }
        String refName = stripBrackets(ctx.getText());
        unresolvedBracketRefs.add(refName);
        return visitChildren(ctx);
    }

    @Override
    public Void visitFunctionCall(DaxParser.FunctionCallContext ctx) {
        String funcName = ctx.functionName().getText().toUpperCase();
        List<String> argTableRefs = new ArrayList<>();

        if (ctx.argumentList() != null) {
            for (DaxParser.ExpressionContext argExpr : ctx.argumentList().expression()) {
                String argText = argExpr.getText();
                if (isTableReference(argText)) {
                    argTableRefs.add(stripTableQuotes(argText));
                }
            }
        }

        functionCalls.add(new FunctionReference(funcName, argTableRefs));
        return visitChildren(ctx);
    }

    @Override
    public Void visitMeasureDefinition(DaxParser.MeasureDefinitionContext ctx) {
        if (ctx.tableRef() != null && ctx.BRACKET_REF() != null) {
            String tableName = stripTableQuotes(ctx.tableRef().getText());
            String measureName = stripBrackets(ctx.BRACKET_REF().getText());
            definedMeasures.add(new MeasureDefinitionRef(tableName, measureName));
            referencedTables.add(tableName);
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitVarDefinition(DaxParser.VarDefinitionContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            definedVars.add(ctx.IDENTIFIER().getText());
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitVarReturnExpression(DaxParser.VarReturnExpressionContext ctx) {
        if (ctx.VAR() != null && ctx.IDENTIFIER() != null) {
            definedVars.add(ctx.IDENTIFIER().getText());
        }
        return visitChildren(ctx);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String stripTableQuotes(String name) {
        if (name.startsWith("'") && name.endsWith("'")) {
            return name.substring(1, name.length() - 1).replace("''", "'");
        }
        return name;
    }

    private static String stripBrackets(String ref) {
        if (ref.startsWith("[") && ref.endsWith("]")) {
            return ref.substring(1, ref.length() - 1);
        }
        return ref;
    }

    private static boolean isTableReference(String text) {
        return (text.startsWith("'") && text.endsWith("'"))
                || text.matches("[A-Za-z_][A-Za-z0-9_]*");
    }
}
