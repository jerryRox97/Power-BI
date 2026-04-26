/*
 * DAX (Data Analysis Expressions) Parser Grammar
 * Based on the COMPLETE Microsoft DAX documentation:
 *   - https://learn.microsoft.com/en-us/dax/dax-syntax-reference
 *   - https://learn.microsoft.com/en-us/dax/dax-operator-reference
 *   - https://learn.microsoft.com/en-us/dax/dax-queries
 *   - https://learn.microsoft.com/en-us/dax/statements-dax
 *
 * Covers:
 *   - DEFINE block with MEASURE, VAR, TABLE, COLUMN definitions
 *   - EVALUATE statements with optional ORDER BY and START AT
 *   - FUNCTION definitions (DEFINE FUNCTION name = (params) => body)
 *   - VAR / RETURN expressions
 *   - Full operator precedence:
 *       || > && > NOT/IN > =,==,<>,<,>,<=,>= > & > +,- > *,/ > ^ > unary(+/-) > primary
 *   - Table constructors: {(v1, v2), (v3, v4)}
 *   - Qualified column references: 'Table'[Column], Table[Column]
 *   - Unqualified bracket references: [Measure]
 *   - Function calls with arbitrary nesting
 *   - Literals: numbers, strings, date/time (dt"..."), TRUE, FALSE, BLANK()
 */
parser grammar DaxParser;

options { tokenVocab = DaxLexer; }

// ══════════════════════════════════════════════════════════════════════
// Entry Points
// ══════════════════════════════════════════════════════════════════════

daxExpression
    : daxQuery EOF
    | expression EOF
    ;

// ══════════════════════════════════════════════════════════════════════
// DAX Query (DEFINE / EVALUATE)
// Per: https://learn.microsoft.com/en-us/dax/dax-queries
// ══════════════════════════════════════════════════════════════════════

daxQuery
    : defineBlock? evaluateStatement+
    ;

// ── DEFINE Block ────────────────────────────────────────────────────
// Per: https://learn.microsoft.com/en-us/dax/statements-dax
// Can contain MEASURE, VAR, TABLE, COLUMN, FUNCTION definitions

defineBlock
    : DEFINE defineItem+
    ;

defineItem
    : measureDefinition
    | varDefinition
    | tableDefinition
    | columnDefinition
    | functionDefinition
    ;

// DEFINE MEASURE 'Table'[Measure] = expression
measureDefinition
    : MEASURE tableRef BRACKET_REF EQ expression
    ;

// DEFINE VAR name = expression
varDefinition
    : VAR IDENTIFIER EQ expression
    ;

// DEFINE TABLE name = expression
tableDefinition
    : TABLE_KW IDENTIFIER EQ expression
    ;

// DEFINE COLUMN 'Table'[Column] = expression
columnDefinition
    : COLUMN_KW tableRef BRACKET_REF EQ expression
    ;

// DEFINE FUNCTION name(params) = RETURN expression
// Per: https://learn.microsoft.com/en-us/dax/statements-dax#function
functionDefinition
    : FUNCTION_KW IDENTIFIER LPAREN functionParamList? RPAREN EQ RETURN expression
    ;

functionParamList
    : functionParam ( COMMA functionParam )*
    ;

functionParam
    : OPTIONAL_KW? IDENTIFIER
    ;

// ── EVALUATE Statement ──────────────────────────────────────────────
// Per: https://learn.microsoft.com/en-us/dax/dax-queries

evaluateStatement
    : EVALUATE expression orderByClause? startAtClause?
    ;

// ORDER BY col1 ASC, col2 DESC
// Per: https://learn.microsoft.com/en-us/dax/statements-dax#order-by
orderByClause
    : ORDER BY orderByItem ( COMMA orderByItem )*
    ;

orderByItem
    : expression ( ASC | DESC )?
    ;

// START AT value1, value2, ...
// Per: https://learn.microsoft.com/en-us/dax/statements-dax#start-at
startAtClause
    : START AT expression ( COMMA expression )*
    ;

// ══════════════════════════════════════════════════════════════════════
// Expressions — Operator Precedence (lowest to highest)
// Per: https://learn.microsoft.com/en-us/dax/dax-operator-reference
//
//   1. || (Logical OR)
//   2. && (Logical AND)
//   3. NOT (Logical NOT) / IN
//   4. = == <> < > <= >= (Comparison)
//   5. & (Text concatenation)
//   6. + - (Additive)
//   7. * / (Multiplicative)
//   8. ^ (Exponentiation)
//   9. Unary + - (Sign)
//  10. Primary (function calls, references, literals, parens)
// ══════════════════════════════════════════════════════════════════════

expression
    : varReturnExpression
    | orExpression
    ;

// VAR x = expr VAR y = expr ... RETURN expr
// Per: https://learn.microsoft.com/en-us/dax/statements-dax#var
varReturnExpression
    : VAR IDENTIFIER EQ expression varReturnExpression
    | VAR IDENTIFIER EQ expression RETURN expression
    ;

// Level 1: ||
orExpression
    : andExpression ( OR_OP andExpression )*
    ;

// Level 2: &&
andExpression
    : notExpression ( AND_OP notExpression )*
    ;

// Level 3: NOT / IN
notExpression
    : NOT_KW notExpression
    | inExpression
    ;

inExpression
    : comparisonExpression ( IN tableConstructor )?
    ;

// Level 4: = == <> < > <= >=
comparisonExpression
    : concatExpression ( comparisonOperator concatExpression )*
    ;

comparisonOperator
    : EQ | STRICT_EQ | NEQ | LT | GT | LTE | GTE
    ;

// Level 5: & (text concatenation)
concatExpression
    : additiveExpression ( AMP additiveExpression )*
    ;

// Level 6: + -
additiveExpression
    : multiplicativeExpression ( ( PLUS | MINUS ) multiplicativeExpression )*
    ;

// Level 7: * /
multiplicativeExpression
    : powerExpression ( ( STAR | DIV ) powerExpression )*
    ;

// Level 8: ^
powerExpression
    : unaryExpression ( CARET unaryExpression )*
    ;

// Level 9: unary + -
unaryExpression
    : MINUS unaryExpression
    | PLUS unaryExpression
    | primaryExpression
    ;

// ══════════════════════════════════════════════════════════════════════
// Primary Expressions
// ══════════════════════════════════════════════════════════════════════

primaryExpression
    : functionCall
    | qualifiedColumnRef
    | tableRef
    | bracketRef
    | literal
    | parenExpression
    | tableConstructor
    ;

// ── Table Constructor ───────────────────────────────────────────────
// Per: https://learn.microsoft.com/en-us/dax/dax-syntax-reference
// { (v1, v2), (v3, v4) } or { v1, v2, v3 }

tableConstructor
    : LBRACE tableConstructorRow ( COMMA tableConstructorRow )* RBRACE
    | LBRACE RBRACE
    ;

tableConstructorRow
    : LPAREN expression ( COMMA expression )* RPAREN
    | expression
    ;

// ── Function Call ───────────────────────────────────────────────────
// FUNCNAME( arg1, arg2, ... ) — supports dotted names like PATHITEM.PATHITEMREVERSE

functionCall
    : functionName LPAREN argumentList? RPAREN
    ;

functionName
    : IDENTIFIER ( DOT IDENTIFIER )*
    ;

argumentList
    : expression ( COMMA expression )*
    ;

// ── Parenthesized Expression ────────────────────────────────────────

parenExpression
    : LPAREN expression RPAREN
    ;

// ══════════════════════════════════════════════════════════════════════
// References
// ══════════════════════════════════════════════════════════════════════

// Qualified column/measure reference: 'Table'[Column] or Table[Column]
qualifiedColumnRef
    : tableRef BRACKET_REF
    ;

// Table reference: quoted 'Table Name' or unquoted TableName
tableRef
    : QUOTED_TABLE_NAME
    | IDENTIFIER
    ;

// Unqualified bracket reference: [MeasureName] or [ColumnName]
bracketRef
    : BRACKET_REF
    ;

// ══════════════════════════════════════════════════════════════════════
// Literals
// Per: https://learn.microsoft.com/en-us/dax/dax-syntax-reference
// ══════════════════════════════════════════════════════════════════════

literal
    : INTEGER_LITERAL
    | DECIMAL_LITERAL
    | STRING_LITERAL
    | DATETIME_LITERAL
    | TRUE_KW
    | FALSE_KW
    | BLANK_KW LPAREN RPAREN
    ;
