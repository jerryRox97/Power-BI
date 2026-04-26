/*
 * Power Query M Language Lexer Grammar
 * Based on the COMPLETE Microsoft Power Query M Language Specification:
 *   - https://learn.microsoft.com/en-us/powerquery-m/m-spec-consolidated-grammar
 *   - https://learn.microsoft.com/en-us/powerquery-m/m-spec-lexical-structure
 *   - https://learn.microsoft.com/en-us/powerquery-m/power-query-m-language-specification
 *
 * License: BSD 3-Clause (ANTLR4 runtime)
 *
 * Covers the full M lexical structure per the consolidated grammar:
 *   - Keywords: and, as, each, else, error, false, if, in, is, let, meta,
 *              not, null, or, otherwise, section, shared, then, true, try,
 *              type, #binary, #date, #datetime, #datetimezone, #duration,
 *              #infinity, #nan, #sections, #shared, #table, #time
 *   - Additional contextual keywords: optional, nullable, catch
 *   - Primitive type names (contextual): any, anynonnull, binary, date,
 *     datetime, datetimezone, duration, function, list, logical, none,
 *     null, number, record, table, text, time, type
 *   - Operators and punctuators: , ; = < <= > >= <> + - * / & ( ) [ ] { }
 *     @ ! ? ?? => .. ...
 *   - Identifiers: regular (with dot-chaining), quoted (#"name"),
 *     generalized (for record field names)
 *   - Literals: logical (true, false), number (decimal, hex 0x),
 *     text ("..." with "" escape and #(...) escape sequences),
 *     null, verbatim (#!"...")
 *   - Comments: single-line (//) and delimited block comments
 *   - Character escape sequences: #(cr), #(lf), #(tab), #(XXXX), #(#)
 */
lexer grammar PowerQueryLexer;

// ══════════════════════════════════════════════════════════════════════
// Keywords (per consolidated grammar, section "Keywords and predefined
// identifiers")
// ══════════════════════════════════════════════════════════════════════

AND         : 'and' ;
AS          : 'as' ;
EACH        : 'each' ;
ELSE        : 'else' ;
ERROR       : 'error' ;
FALSE       : 'false' ;
IF          : 'if' ;
IN          : 'in' ;
IS          : 'is' ;
LET         : 'let' ;
META        : 'meta' ;
NOT         : 'not' ;
NULL        : 'null' ;
OR          : 'or' ;
OTHERWISE   : 'otherwise' ;
SECTION     : 'section' ;
SHARED      : 'shared' ;
THEN        : 'then' ;
TRUE        : 'true' ;
TRY         : 'try' ;
TYPE        : 'type' ;

// Additional keywords used in grammar rules
CATCH       : 'catch' ;
OPTIONAL    : 'optional' ;
NULLABLE    : 'nullable' ;

// ══════════════════════════════════════════════════════════════════════
// Intrinsic / Hash Keywords
// Per consolidated grammar: #binary, #date, #datetime, #datetimezone,
//   #duration, #infinity, #nan, #sections, #shared, #table, #time
// ══════════════════════════════════════════════════════════════════════

HASH_TABLE          : '#table' ;
HASH_DATE           : '#date' ;
HASH_DATETIME       : '#datetime' ;
HASH_DATETIMEZONE   : '#datetimezone' ;
HASH_DURATION       : '#duration' ;
HASH_TIME           : '#time' ;
HASH_BINARY         : '#binary' ;
HASH_INFINITY       : '#infinity' ;
HASH_NAN            : '#nan' ;
HASH_SECTIONS       : '#sections' ;
HASH_SHARED         : '#shared' ;

// ══════════════════════════════════════════════════════════════════════
// Primitive Type Names (contextual keywords — recognized in type context)
// Per consolidated grammar: primitive-type production
// ══════════════════════════════════════════════════════════════════════

ANY             : 'any' ;
ANYNONNULL      : 'anynonnull' ;
BINARY          : 'binary' ;
DATE            : 'date' ;
DATETIME        : 'datetime' ;
DATETIMEZONE    : 'datetimezone' ;
DURATION        : 'duration' ;
FUNCTION        : 'function' ;
LIST            : 'list' ;
LOGICAL         : 'logical' ;
NONE            : 'none' ;
NUMBER          : 'number' ;
RECORD          : 'record' ;
TABLE           : 'table' ;
TEXT            : 'text' ;
TIME            : 'time' ;

// ══════════════════════════════════════════════════════════════════════
// Operators & Punctuators
// Per consolidated grammar: operator-or-punctuator production
//   , ; = < <= > >= <> + - * / & ( ) [ ] { } @ ! ? ?? => .. ...
// Listed longest-match-first for multi-character operators
// ══════════════════════════════════════════════════════════════════════

ELLIPSIS    : '...' ;
DOTDOT      : '..' ;
ARROW       : '=>' ;
QQMARK      : '??' ;
NEQ         : '<>' ;
LTE         : '<=' ;
GTE         : '>=' ;
LT          : '<' ;
GT          : '>' ;
EQ          : '=' ;
PLUS        : '+' ;
MINUS       : '-' ;
STAR        : '*' ;
DIV         : '/' ;
AMP         : '&' ;
COMMA       : ',' ;
SEMI        : ';' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACKET    : '[' ;
RBRACKET    : ']' ;
LBRACE      : '{' ;
RBRACE      : '}' ;
AT          : '@' ;
BANG        : '!' ;
QMARK       : '?' ;
DOT         : '.' ;

// ══════════════════════════════════════════════════════════════════════
// Literals
// Per consolidated grammar: literal production
// ══════════════════════════════════════════════════════════════════════

// ── Verbatim Literal: #!"text content" ──────────────────────────────
// Per consolidated grammar: verbatim-literal production
VERBATIM_LITERAL
    : '#!"' ( '""' | ~["] )* '"'
    ;

// ── String Literal: "text" with "" escape and #(...) escape sequences
// Per consolidated grammar: text-literal production
STRING_LITERAL
    : '"' ( '""' | ESCAPE_SEQ | ~["#] | '#' ~[(] )* '"'
    ;

// ── Number Literals ─────────────────────────────────────────────────
// Per consolidated grammar: number-literal, decimal-number-literal,
//   hexadecimal-number-literal productions

HEX_NUMBER
    : '0' [xX] HEX_DIGIT+
    ;

DECIMAL_NUMBER
    : DIGIT+ '.' DIGIT+ EXPONENT_PART?
    | '.' DIGIT+ EXPONENT_PART?
    | DIGIT+ EXPONENT_PART
    ;

INTEGER_NUMBER
    : DIGIT+
    ;

// ══════════════════════════════════════════════════════════════════════
// Identifiers
// Per consolidated grammar: identifier, regular-identifier,
//   quoted-identifier, generalized-identifier productions
// ══════════════════════════════════════════════════════════════════════

// ── Quoted Identifier: #"My Identifier Name" ────────────────────────
// Per consolidated grammar: quoted-identifier production
QUOTED_IDENTIFIER
    : '#"' ( '""' | ~["] )* '"'
    ;

// ── Regular Identifier ──────────────────────────────────────────────
// Per consolidated grammar: regular-identifier, available-identifier,
//   keyword-or-identifier, identifier-start-character,
//   identifier-part-character productions
// Includes dot-character for dotted identifiers (e.g., Table.SelectRows)
IDENTIFIER
    : IDENT_START IDENT_PART* ( '.' IDENT_START IDENT_PART* )*
    ;

// ══════════════════════════════════════════════════════════════════════
// Whitespace & Comments
// Per consolidated grammar: whitespace, comment productions
// ══════════════════════════════════════════════════════════════════════

WS
    : ( ' ' | '\t' | '\r' | '\n'
      | '\u000B'    // vertical tab
      | '\u000C'    // form feed
      | '\u0085'    // next line
      | '\u2028'    // line separator
      | '\u2029'    // paragraph separator
      )+ -> skip
    ;

// Per consolidated grammar: single-line-comment production
LINE_COMMENT
    : '//' ~[\r\n\u0085\u2028\u2029]* -> skip
    ;

// Per consolidated grammar: delimited-comment production
BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

// ══════════════════════════════════════════════════════════════════════
// Fragments
// ══════════════════════════════════════════════════════════════════════

fragment DIGIT          : [0-9] ;
fragment HEX_DIGIT      : [0-9a-fA-F] ;
fragment IDENT_START    : [a-zA-Z_] ;
fragment IDENT_PART     : [a-zA-Z0-9_] ;
fragment EXPONENT_PART  : [eE] [+-]? DIGIT+ ;

// Character escape sequence: #(cr), #(lf), #(tab), #(XXXX), #(#)
fragment ESCAPE_SEQ
    : '#(' ESCAPE_BODY ( ',' ESCAPE_BODY )* ')'
    ;

fragment ESCAPE_BODY
    : 'cr'
    | 'lf'
    | 'tab'
    | '#'
    | HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    | HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
