/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.parser.dax;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Facade that wraps the ANTLR4-generated DAX lexer/parser and the
 * {@link DaxLineageVisitor} into a single entry point for lineage extraction.
 *
 * <p>Usage:
 * <pre>
 *   DaxExpressionParserAntlr.ParseResult result = DaxExpressionParserAntlr.parse(daxExpression);
 *   result.getReferencedTables();   // Set of table names
 *   result.getReferencedColumns();  // Set of ColumnReference records
 *   result.getFunctionCalls();      // Set of FunctionReference records
 * </pre>
 *
 * <p>Falls back gracefully: if parsing fails (malformed DAX), returns empty results
 * rather than throwing exceptions.
 */
public final class DaxExpressionParserAntlr {

    private static final Logger log = LoggerFactory.getLogger(DaxExpressionParserAntlr.class);

    private DaxExpressionParserAntlr() {}

    /**
     * Result of ANTLR-based DAX parsing.
     */
    public static final class ParseResult {
        private final Set<String> referencedTables;
        private final Set<DaxLineageVisitor.ColumnReference> referencedColumns;
        private final Set<String> unresolvedBracketRefs;
        private final Set<DaxLineageVisitor.FunctionReference> functionCalls;
        private final Set<DaxLineageVisitor.MeasureDefinitionRef> definedMeasures;
        private final Set<String> definedVars;
        private final boolean parsed;

        ParseResult(DaxLineageVisitor visitor) {
            this.referencedTables = visitor.getReferencedTables();
            this.referencedColumns = visitor.getReferencedColumns();
            this.unresolvedBracketRefs = visitor.getUnresolvedBracketRefs();
            this.functionCalls = visitor.getFunctionCalls();
            this.definedMeasures = visitor.getDefinedMeasures();
            this.definedVars = visitor.getDefinedVars();
            this.parsed = true;
        }

        ParseResult() {
            this.referencedTables = Collections.emptySet();
            this.referencedColumns = Collections.emptySet();
            this.unresolvedBracketRefs = Collections.emptySet();
            this.functionCalls = Collections.emptySet();
            this.definedMeasures = Collections.emptySet();
            this.definedVars = Collections.emptySet();
            this.parsed = false;
        }

        public Set<String> getReferencedTables() { return referencedTables; }
        public Set<DaxLineageVisitor.ColumnReference> getReferencedColumns() { return referencedColumns; }
        public Set<String> getUnresolvedBracketRefs() { return unresolvedBracketRefs; }
        public Set<DaxLineageVisitor.FunctionReference> getFunctionCalls() { return functionCalls; }
        public Set<DaxLineageVisitor.MeasureDefinitionRef> getDefinedMeasures() { return definedMeasures; }
        public Set<String> getDefinedVars() { return definedVars; }
        public boolean isParsed() { return parsed; }

        public List<String> getReferencedTableList() {
            return List.copyOf(referencedTables);
        }
    }

    /**
     * Parses a DAX expression using the ANTLR4 grammar and extracts lineage references.
     *
     * @param expression the DAX expression text
     * @return parsed result with tables, columns, functions, measures, and variables
     */
    public static ParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return new ParseResult();
        }

        try {
            DaxLexer lexer = new DaxLexer(CharStreams.fromString(expression));
            lexer.removeErrorListeners();

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            DaxParser parser = new DaxParser(tokens);
            parser.removeErrorListeners();

            ParseTree tree = parser.daxExpression();

            DaxLineageVisitor visitor = new DaxLineageVisitor();
            visitor.visit(tree);

            return new ParseResult(visitor);
        } catch (Exception e) {
            log.debug("ANTLR DAX parse failed for expression (length={}), returning empty result: {}",
                    expression.length(), e.getMessage());
            return new ParseResult();
        }
    }

    /**
     * Convenience method: extracts just the referenced table names from a DAX expression.
     */
    public static List<String> extractReferencedTables(String expression) {
        return parse(expression).getReferencedTableList();
    }
}
