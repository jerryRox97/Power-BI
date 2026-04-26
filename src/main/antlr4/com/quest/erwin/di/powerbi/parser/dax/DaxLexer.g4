/*
 * DAX (Data Analysis Expressions) Lexer Grammar
 * Based on the COMPLETE Microsoft DAX documentation:
 *   - https://learn.microsoft.com/en-us/dax/dax-syntax-reference
 *   - https://learn.microsoft.com/en-us/dax/dax-operator-reference
 *   - https://learn.microsoft.com/en-us/dax/dax-queries
 *   - https://learn.microsoft.com/en-us/dax/statements-dax
 *     (DEFINE, EVALUATE, MEASURE, ORDER BY, START AT, VAR, FUNCTION)
 *
 * License: BSD 3-Clause (ANTLR4 runtime)
 *
 * Covers:
 *   - All DAX keywords: DEFINE, EVALUATE, MEASURE, VAR, RETURN, ORDER, BY,
 *     ASC, DESC, START, AT, TABLE, COLUMN, FUNCTION, OPTIONAL, IN, NOT,
 *     TRUE, FALSE, BLANK
 *   - All DAX operators with correct precedence:
 *       ^ > unary(-) > *, / > +, - > & > =, ==, <>, <, >, <=, >= > IN > NOT > && > ||
 *   - Strict equality (==) and assignment/comparison (=)
 *   - Table constructors: {(v1, v2), (v3, v4)}
 *   - Qualified column references: 'Table Name'[ColumnName], Table[Column]
 *   - Unqualified bracket references: [MeasureName]
 *   - Function calls with arguments
 *   - Literals: integers, decimals, strings, date/time (dt"..."), booleans
 *   - Comments: single-line (//) and block comments
 *   - Case-insensitive keywords via fragments
 */
lexer grammar DaxLexer;

// ── Keywords (case-insensitive) ──────────────────────────────────────

DEFINE      : D E F I N E ;
EVALUATE    : E V A L U A T E ;
MEASURE     : M E A S U R E ;
VAR         : V A R ;
RETURN      : R E T U R N ;
ORDER       : O R D E R ;
BY          : B Y ;
ASC         : A S C ;
DESC        : D E S C ;
START       : S T A R T ;
AT          : A T ;
TABLE_KW    : T A B L E ;
COLUMN_KW   : C O L U M N ;
FUNCTION_KW : F U N C T I O N ;
OPTIONAL_KW : O P T I O N A L ;
IN          : I N ;
NOT_KW      : N O T ;
TRUE_KW     : T R U E ;
FALSE_KW    : F A L S E ;
BLANK_KW    : B L A N K ;

// ── Operators ────────────────────────────────────────────────────────
// Listed longest-match-first for multi-character operators

AND_OP      : '&&' ;
OR_OP       : '||' ;
STRICT_EQ   : '==' ;
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
CARET       : '^' ;
AMP         : '&' ;

// ── Punctuation ─────────────────────────────────────────────────────

LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACE      : '{' ;
RBRACE      : '}' ;
LBRACKET    : '[' ;
RBRACKET    : ']' ;
COMMA       : ',' ;
SEMI        : ';' ;
DOTDOT      : '..' ;
DOT         : '.' ;

// ── Date/Time Literal: dt"2023-01-15" or dt"2023-01-15T10:30:00" ───

DATETIME_LITERAL
    : D T '"' ~["]* '"'
    ;

// ── String Literal: "hello" with "" as escape ────────────────────────

STRING_LITERAL
    : '"' ( '""' | ~["\r\n] )* '"'
    ;

// ── Quoted Table Name: 'My Table Name' with '' as escape ────────────

QUOTED_TABLE_NAME
    : '\'' ( '\'\'' | ~['\r\n] )* '\''
    ;

// ── Column / Measure Bracket Reference: [Column Name] ───────────────

BRACKET_REF
    : '[' ~[\]\r\n]+ ']'
    ;

// ── Number Literals ─────────────────────────────────────────────────

DECIMAL_LITERAL
    : DIGIT+ '.' DIGIT+ ( [eE] [+-]? DIGIT+ )?
    | '.' DIGIT+ ( [eE] [+-]? DIGIT+ )?
    | DIGIT+ [eE] [+-]? DIGIT+
    ;

INTEGER_LITERAL
    : DIGIT+
    ;

// ── Identifiers (unquoted table names, function names, etc.) ────────

IDENTIFIER
    : LETTER ( LETTER | DIGIT | '_' )*
    ;

// ── Whitespace & Comments ───────────────────────────────────────────

WS
    : [ \t\r\n\u000B\u000C]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

// ── Fragments ───────────────────────────────────────────────────────

fragment DIGIT  : [0-9] ;
fragment LETTER : [a-zA-Z_] ;

// Case-insensitive letter fragments for keywords
fragment A : [aA] ; fragment B : [bB] ; fragment C : [cC] ; fragment D : [dD] ;
fragment E : [eE] ; fragment F : [fF] ; fragment G : [gG] ; fragment H : [hH] ;
fragment I : [iI] ; fragment J : [jJ] ; fragment K : [kK] ; fragment L : [lL] ;
fragment M : [mM] ; fragment N : [nN] ; fragment O : [oO] ; fragment P : [pP] ;
fragment Q : [qQ] ; fragment R : [rR] ; fragment S : [sS] ; fragment T : [tT] ;
fragment U : [uU] ; fragment V : [vV] ; fragment W : [wW] ; fragment X : [xX] ;
fragment Y : [yY] ; fragment Z : [zZ] ;
