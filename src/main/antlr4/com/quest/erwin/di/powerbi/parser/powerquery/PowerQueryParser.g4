/*
 * Power Query M Language Parser Grammar
 * Based on the COMPLETE Microsoft Power Query M Language Specification:
 *   - https://learn.microsoft.com/en-us/powerquery-m/m-spec-consolidated-grammar
 *   - https://learn.microsoft.com/en-us/powerquery-m/power-query-m-language-specification
 *
 * Covers:
 *   - document = expression | section-document
 *   - let ... in ... expressions (step-by-step transformation chains)
 *   - if ... then ... else ... conditionals
 *   - each expressions (shorthand lambdas: each _[Column])
 *   - Function definitions and invocations
 *   - Record/List/Table expressions
 *   - Field access, item access, projection
 *   - All operators with correct precedence
 *   - Type expressions (table, record, list, function, nullable, primitive)
 *   - Error handling (try/otherwise/catch)
 *   - Section documents
 *   - Transform data functions and data source connectors (as invocations)
 */
parser grammar PowerQueryParser;

options { tokenVocab = PowerQueryLexer; }

// ======================================================================
// Document (Top-Level Entry Point)
// ======================================================================

document
    : sectionDocument EOF
    | expression EOF
    ;

// ======================================================================
// Section Document
// ======================================================================

sectionDocument
    : literalAttributes? SECTION sectionName? SEMI sectionMembers
    ;

sectionName
    : identifier
    ;

sectionMembers
    : sectionMember*
    ;

sectionMember
    : literalAttributes? SHARED? sectionMemberName EQ expression SEMI
    ;

sectionMemberName
    : identifier
    ;

// ======================================================================
// Literal Attributes
// ======================================================================

literalAttributes
    : recordLiteral
    ;

recordLiteral
    : LBRACKET literalFieldList? RBRACKET
    ;

literalFieldList
    : literalField ( COMMA literalField )*
    ;

literalField
    : fieldName EQ anyLiteral
    ;

anyLiteral
    : recordLiteral
    | listLiteral
    | logicalLiteral
    | numberLiteral
    | stringLiteral
    | nullLiteral
    ;

listLiteral
    : LBRACE anyLiteral ( COMMA anyLiteral )* RBRACE
    | LBRACE RBRACE
    ;

logicalLiteral
    : TRUE | FALSE
    ;

numberLiteral
    : INTEGER_NUMBER | DECIMAL_NUMBER | HEX_NUMBER
    | HASH_INFINITY | HASH_NAN
    ;

stringLiteral
    : STRING_LITERAL
    ;

nullLiteral
    : NULL
    ;

// ======================================================================
// Expressions
// ======================================================================

expression
    : letExpression
    | ifExpression
    | eachExpression
    | functionExpression
    | errorRaisingExpression
    | errorHandlingExpression
    | logicalOrExpression
    ;

// -- Let Expression ---------------------------------------------------

letExpression
    : LET variableList IN expression
    ;

variableList
    : variable ( COMMA variable )*
    ;

variable
    : variableName EQ expression
    ;

variableName
    : identifier
    ;

// -- If Expression ----------------------------------------------------

ifExpression
    : IF expression THEN expression ELSE expression
    ;

// -- Each Expression --------------------------------------------------

eachExpression
    : EACH eachExpressionBody
    ;

eachExpressionBody
    : functionBody
    ;

// -- Function Expression ----------------------------------------------

functionExpression
    : LPAREN parameterList? RPAREN returnType? ARROW functionBody
    ;

functionBody
    : expression
    ;

parameterList
    : fixedParameterList
    | fixedParameterList COMMA optionalParameterList
    | optionalParameterList
    ;

fixedParameterList
    : parameter ( COMMA parameter )*
    ;

optionalParameterList
    : optionalParameter ( COMMA optionalParameter )*
    ;

optionalParameter
    : OPTIONAL parameter
    ;

parameter
    : parameterName parameterType?
    ;

parameterName
    : identifier
    ;

parameterType
    : assertion
    ;

returnType
    : assertion
    ;

assertion
    : AS nullableOrNonNullablePrimitiveType
    ;

// -- Error Raising ----------------------------------------------------

errorRaisingExpression
    : ERROR expression
    ;

// -- Error Handling ---------------------------------------------------

errorHandlingExpression
    : TRY expression otherwiseClause
    | TRY expression catchClause
    | TRY expression
    ;

otherwiseClause
    : OTHERWISE expression
    ;

catchClause
    : CATCH catchFunction
    ;

catchFunction
    : LPAREN identifier? RPAREN ARROW expression
    ;

// ======================================================================
// Operator Expressions -- Precedence (lowest to highest)
// ======================================================================

logicalOrExpression
    : logicalAndExpression ( OR logicalAndExpression )*
    ;

logicalAndExpression
    : notExpression ( AND notExpression )*
    ;

notExpression
    : NOT notExpression
    | nullCoalescingExpression
    ;

nullCoalescingExpression
    : isExpression ( QQMARK isExpression )*
    ;

isExpression
    : asExpression ( IS nullablePrimitiveType )?
    ;

asExpression
    : equalityExpression ( AS nullablePrimitiveType )?
    ;

equalityExpression
    : relationalExpression ( ( EQ | NEQ ) relationalExpression )*
    ;

relationalExpression
    : additiveExpression ( ( LT | GT | LTE | GTE ) additiveExpression )*
    ;

additiveExpression
    : multiplicativeExpression ( ( PLUS | MINUS | AMP ) multiplicativeExpression )*
    ;

multiplicativeExpression
    : metadataExpression ( ( STAR | DIV ) metadataExpression )*
    ;

metadataExpression
    : unaryExpression ( META unaryExpression )?
    ;

unaryExpression
    : typeExpression
    | PLUS unaryExpression
    | MINUS unaryExpression
    | NOT unaryExpression
    ;

typeExpression
    : primaryExpression
    | TYPE primaryType
    ;

// ======================================================================
// Primary Expressions
// ======================================================================

primaryExpression
    : literalExpression                                                    # litExpr
    | listExpression                                                       # listExpr
    | recordExpression                                                     # recordExpr
    | identifierExpression                                                 # identExpr
    | sectionAccessExpression                                              # sectionAccessExpr
    | parenthesizedExpression                                              # parenExpr
    | notImplementedExpression                                             # notImplExpr
    | intrinsicExpression                                                  # intrinsicExpr
    | primaryExpression LPAREN argumentList? RPAREN                        # invokeExpr
    | primaryExpression LBRACE itemSelector RBRACE optionalOperator?       # itemAccessExpr
    | primaryExpression fieldSelector                                      # fieldAccessExpr
    | primaryExpression requiredProjection                                 # projectionExpr
    | primaryExpression requiredProjection QMARK                           # optionalProjectionExpr
    ;

intrinsicExpression
    : HASH_TABLE
    | HASH_DATE
    | HASH_DATETIME
    | HASH_DATETIMEZONE
    | HASH_DURATION
    | HASH_TIME
    | HASH_BINARY
    | HASH_SECTIONS
    | HASH_SHARED
    ;

optionalOperator
    : QMARK
    ;

itemSelector
    : expression
    ;

fieldSelector
    : LBRACKET fieldName RBRACKET optionalOperator?
    ;

requiredProjection
    : LBRACKET requiredSelectorList RBRACKET
    ;

requiredSelectorList
    : LBRACKET fieldName RBRACKET ( COMMA LBRACKET fieldName RBRACKET )*
    ;

// -- Literal Expressions ----------------------------------------------

literalExpression
    : INTEGER_NUMBER
    | DECIMAL_NUMBER
    | HEX_NUMBER
    | STRING_LITERAL
    | VERBATIM_LITERAL
    | TRUE
    | FALSE
    | NULL
    | HASH_INFINITY
    | HASH_NAN
    ;

// -- Identifier Expressions -------------------------------------------

identifierExpression
    : identifierReference
    ;

identifierReference
    : exclusiveIdentifierReference
    | inclusiveIdentifierReference
    ;

exclusiveIdentifierReference
    : identifier
    ;

inclusiveIdentifierReference
    : AT identifier
    ;

// -- Section Access Expression ----------------------------------------

sectionAccessExpression
    : identifier BANG identifier
    ;

// -- List Expression --------------------------------------------------

listExpression
    : LBRACE itemList? RBRACE
    ;

itemList
    : item ( COMMA item )*
    ;

item
    : expression ( DOTDOT expression )?
    ;

// -- Record Expression ------------------------------------------------

recordExpression
    : LBRACKET fieldList? RBRACKET
    ;

fieldList
    : field ( COMMA field )*
    ;

field
    : fieldName EQ expression
    ;

// -- Field Name -------------------------------------------------------

fieldName
    : generalizedIdentifier
    | QUOTED_IDENTIFIER
    ;

generalizedIdentifier
    : IDENTIFIER
    | keywordAsIdentifier
    ;

// -- Parenthesized Expression -----------------------------------------

parenthesizedExpression
    : LPAREN expression RPAREN
    ;

// -- Not Implemented Expression ---------------------------------------

notImplementedExpression
    : ELLIPSIS
    ;

// -- Argument List ----------------------------------------------------

argumentList
    : expression ( COMMA expression )*
    ;

// ======================================================================
// Identifiers
// ======================================================================

identifier
    : IDENTIFIER
    | QUOTED_IDENTIFIER
    | keywordAsIdentifier
    ;

keywordAsIdentifier
    : ANY | ANYNONNULL | BINARY | DATE | DATETIME | DATETIMEZONE
    | DURATION | FUNCTION | LIST | LOGICAL | NONE | NUMBER
    | RECORD | TABLE | TEXT | TIME
    ;

// ======================================================================
// Type Rules
// ======================================================================

primaryType
    : primitiveType
    | recordType
    | listType
    | functionType
    | tableType
    ;

tableType
    : TABLE rowType
    ;

rowType
    : LBRACKET fieldSpecificationList? RBRACKET
    ;

recordType
    : LBRACKET openRecordMarker RBRACKET
    | LBRACKET fieldSpecificationList COMMA openRecordMarker RBRACKET
    | LBRACKET fieldSpecificationList? RBRACKET
    ;

openRecordMarker
    : ELLIPSIS
    ;

fieldSpecificationList
    : fieldSpecification ( COMMA fieldSpecification )*
    ;

fieldSpecification
    : OPTIONAL? fieldName fieldTypSpecification?
    ;

fieldTypSpecification
    : EQ primaryType
    ;

listType
    : LBRACE primaryType RBRACE
    ;

functionType
    : FUNCTION LPAREN functionTypeParamList? RPAREN returnType
    ;

functionTypeParamList
    : functionTypeParam ( COMMA functionTypeParam )*
    ;

functionTypeParam
    : parameterName AS primaryType
    | OPTIONAL parameterName AS primaryType
    ;

primitiveType
    : ANY | ANYNONNULL | BINARY | DATE | DATETIME | DATETIMEZONE
    | DURATION | FUNCTION | LIST | LOGICAL | NONE | NULL
    | NUMBER | RECORD | TABLE | TEXT | TIME | TYPE
    ;

nullablePrimitiveType
    : NULLABLE primitiveType
    | primitiveType
    ;

nullableOrNonNullablePrimitiveType
    : NULLABLE primitiveType
    | primitiveType
    ;
