grammar g;
prule: 'Hello ' CHAR;
CHAR: 'a'..'z';


insertStatement : INSERT INTO tableName LEFT_PAREN columnNames RIGHT_PAREN VALUES LEFT_PAREN values RIGHT_PAREN SEMICOLON;
updateStatement : UPDATE tableName SET setClause WHERE condition SEMICOLON;
deleteStatement: DELETE FROM tableName WHERE conditions SEMICOLON;
setClause : setItem (COMMA setItem)* ;
setItem : columnName EQUALS valueExpression ;
values: valueExpression (COMMA valueExpression)*;
valueExpression : literalValue;
literalValue : INTEGER | STRING | NULL | IDENTIFIER ;
conditions: condition (COMMA condition)*;
condition : comparisonExpression ;
comparisonExpression : valueExpression comparisonOperator valueExpression ;
comparisonOperator : EQUALS | NOT_EQUALS | LESS_THAN | GREATER_THAN | LESS_THAN_OR_EQUALS | GREATER_THAN_OR_EQUALS ;
tableName : IDENTIFIER ;
columnName : IDENTIFIER ;
columnNames: columnName (COMMA columnName)*;
functionName : IDENTIFIER ;

DELETE: SPACE*[dD][eE][lL][eE][tT][eE]SPACE*;
INSERT: SPACE*[iI][nN][sS][eE][rR][tT]SPACE*;
INTO: [iI][nN][tT][oO]SPACE*;
VALUES: [vV][aA][lL][uU][eE][sS];
UPDATE: SPACE*[uU][pP][dD][aA][tT][eE]SPACE*;
COMMA: SPACE*','SPACE*;
EQUALS: SPACE*'='SPACE*;
INTEGER: SPACE*[0-9]+SPACE*;
//STRING: [a-zA-Z]+;
NULL: SPACE*[nN][uU][lL][lL]SPACE*;
LEFT_PAREN : SPACE*'('SPACE*;
RIGHT_PAREN : SPACE*')'SPACE*;
NOT_EQUALS: SPACE*'<>'SPACE*;
LESS_THAN: SPACE*'<'SPACE*;
LESS_THAN_OR_EQUALS: SPACE*'<='SPACE*;
GREATER_THAN: SPACE*'>'SPACE*;
GREATER_THAN_OR_EQUALS : SPACE*'>='SPACE*;
SEMICOLON: SPACE*';'SPACE*;
SPACE: ' ';
WHERE: SPACE*[wW][hH][eE][rR][eE]SPACE*;
SET: SPACE*[Ss][eE][tT]SPACE*;
FROM: [fF][rR][oO][mM]SPACE*;


IDENTIFIER:   ID_START ID_CONTINUE* ;

ID_START
    :   [a-z]
    ;

ID_CONTINUE
    :   ID_START | DIGIT
    ;

LETTER
    :   [a-zA-Z]
    ;

DIGIT
    :   [0-9]
    ;


STRING: '"' [a-zA-Z0-9_]+ '"';
