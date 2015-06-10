/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar LittleMaschine;

program
    : (statement (EOL | EOF))+
    ;

statement
    : singleOperandInstr
    | dualOperandInstr
    ;

singleOperandInstr
    : SingleOperandOpcode (Period SizeSpecifier)? operand
    ;

dualOperandInstr
    : DualOperandOpcode (Period SizeSpecifier)? operand Comma operand
    ;

operand
    : Register
    | Identifier
    | Literal
    | addressExpression
    ;

addressExpression
    : Pointer? LBracket Literal RBracket
    ;

LBracket: '[';
RBracket: ']';
Add: '+';
Multiply: '*';
Subtract: '-';
Period: '.';
Comma: ',';
Dollar: '$';

Literal
    : Hex
    | Binary
    | Integer
    | String
    | Char
    ;

Integer: [1-9] [0-9]*;
HexDigit: [0-9a-fA-F] ;
Hex: '0' [xX] HexDigit+ ;
Binary: '0' [bB] [10]+ ;
String : '"' ( ~'"' | '\\' '"' )* '"' ;
Char : '\'' ( ~'\'' | '\\' '\'' ) '\'' ;

Pointer
    : 'ptr'
    | '^'
    ;

SizeSpecifier
    : 'b'
    | 'w'
    | 'dw'
    ;

DualOperandOpcode
    : 'add'
    | 'mov'
    ;

SingleOperandOpcode
    : 'push'
    ;

Register
    : Dollar [a-zA-Z0-9]+
    ;

Identifier
    : [_a-zA-Z] [_a-zA-Z0-9]*
    ;

EOL
    : '\r'? '\n'
    ;

Comment
    : ';' ~[\r\n]* -> skip
    ;

Whitespace
    : [ \t] -> skip
    ;