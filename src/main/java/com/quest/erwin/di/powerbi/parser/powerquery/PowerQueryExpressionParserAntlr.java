/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.parser.powerquery;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade that wraps the ANTLR4-generated Power Query M lexer/parser and the
 * {@link PowerQueryLineageVisitor} into a single entry point for lineage extraction.
 *
 * <p>Usage:
 * <pre>
 *   var result = PowerQueryExpressionParserAntlr.parse(mExpression);
 *   result.getFunctionInvocations();  // Set of FunctionInvocation records
 *   result.getLetSteps();             // List of LetStep records
 *   result.getDataSourceRefs();       // Set of DataSourceRef records
 * </pre>
 *
 * <p>Falls back gracefully: if parsing fails (malformed M), returns empty results
 * rather than throwing exceptions.
 */
public final class PowerQueryExpressionParserAntlr {

    private static final Logger log = LoggerFactory.getLogger(PowerQueryExpressionParserAntlr.class);

    private PowerQueryExpressionParserAntlr() {}

    /**
     * Result of ANTLR-based Power Query M parsing.
     */
    public static final class ParseResult {
        private final Set<PowerQueryLineageVisitor.FunctionInvocation> functionInvocations;
        private final List<PowerQueryLineageVisitor.LetStep> letSteps;
        private final Set<String> referencedIdentifiers;
        private final Set<String> fieldAccessNames;
        private final Set<String> stringLiterals;
        private final Set<PowerQueryLineageVisitor.DataSourceRef> dataSourceRefs;
        private final Map<String, String> stepExpressions;
        private final boolean parsed;

        ParseResult(PowerQueryLineageVisitor visitor) {
            this.functionInvocations = visitor.getFunctionInvocations();
            this.letSteps = visitor.getLetSteps();
            this.referencedIdentifiers = visitor.getReferencedIdentifiers();
            this.fieldAccessNames = visitor.getFieldAccessNames();
            this.stringLiterals = visitor.getStringLiterals();
            this.dataSourceRefs = visitor.getDataSourceRefs();
            this.stepExpressions = visitor.getStepExpressions();
            this.parsed = true;
        }

        ParseResult() {
            this.functionInvocations = Collections.emptySet();
            this.letSteps = Collections.emptyList();
            this.referencedIdentifiers = Collections.emptySet();
            this.fieldAccessNames = Collections.emptySet();
            this.stringLiterals = Collections.emptySet();
            this.dataSourceRefs = Collections.emptySet();
            this.stepExpressions = Collections.emptyMap();
            this.parsed = false;
        }

        public Set<PowerQueryLineageVisitor.FunctionInvocation> getFunctionInvocations() { return functionInvocations; }
        public List<PowerQueryLineageVisitor.LetStep> getLetSteps() { return letSteps; }
        public Set<String> getReferencedIdentifiers() { return referencedIdentifiers; }
        public Set<String> getFieldAccessNames() { return fieldAccessNames; }
        public Set<String> getStringLiterals() { return stringLiterals; }
        public Set<PowerQueryLineageVisitor.DataSourceRef> getDataSourceRefs() { return dataSourceRefs; }
        public Map<String, String> getStepExpressions() { return stepExpressions; }
        public boolean isParsed() { return parsed; }
    }

    /**
     * Parses a Power Query M expression using the ANTLR4 grammar and extracts lineage references.
     *
     * @param expression the M expression text
     * @return parsed result with function invocations, let steps, data source refs, etc.
     */
    public static ParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return new ParseResult();
        }

        try {
            PowerQueryLexer lexer = new PowerQueryLexer(CharStreams.fromString(expression));
            lexer.removeErrorListeners();

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PowerQueryParser parser = new PowerQueryParser(tokens);
            parser.removeErrorListeners();

            ParseTree tree = parser.document();

            PowerQueryLineageVisitor visitor = new PowerQueryLineageVisitor();
            visitor.visit(tree);

            return new ParseResult(visitor);
        } catch (Exception e) {
            log.debug("ANTLR Power Query M parse failed for expression (length={}), returning empty result: {}",
                    expression.length(), e.getMessage());
            return new ParseResult();
        }
    }

    /**
     * Convenience method: extracts data source references from an M expression.
     */
    public static Set<PowerQueryLineageVisitor.DataSourceRef> extractDataSources(String expression) {
        return parse(expression).getDataSourceRefs();
    }

    /**
     * Convenience method: extracts let-expression steps from an M expression.
     */
    public static List<PowerQueryLineageVisitor.LetStep> extractLetSteps(String expression) {
        return parse(expression).getLetSteps();
    }
}
