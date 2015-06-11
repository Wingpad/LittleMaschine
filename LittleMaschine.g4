/**
 * Grammar for the LVM Machine`
 */
grammar LittleMaschine;

program
  : ((label EOL?)* statement (EOL | EOF))+
  ;

label
  : Identifier Colon
  ;

statement
  : singleOperandInstr
  | dualOperandInstr
  | noOperandInstr
  ;

noOperandInstr
  : NoOperandOpcode
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
  : Pointer? LBracket expression RBracket
  ;

expression
  : operand
  | expression (Add | Subtract | Multiply) operand
  ;

LBracket: '[';
RBracket: ']';
Add:      '+';
Subtract: '-';
Multiply: '*';
Colon:    ':';
Period:   '.';
Comma:    ',';
Dollar:   '$';

SizeSpecifier
  : 'dw'
  | [wb]
  ;

Literal
  : Hex
  | Binary
  | Octal
  | Integer
  | String
  | Char
  ;

Integer: [1-9] [0-9]* ;
HexDigit: [0-9a-fA-F] ;
OctalDigit: [0-7] ;
Hex: '0' [xX] HexDigit+ ;
Binary: '0' [bB] [10]+ ;
Octal: '0' OctalDigit+ ;
String: '"' ( ~'"' | '\\' '"' )* '"' ;
Char: '\'' ( ~'\'' | '\\' ~[\s] ) '\'' ;

Pointer
  : 'ptr'
  | '^'
  ;

DualOperandOpcode
  : 'add'
  | 'sub'
  | 'mul'
  | 'div'
  | 'mod'
  | 'shl'
  | 'shr'
  | 'mov'
  | 'xchg'
  | 'and'
  | 'or'
  | 'xor'
  | 'nand'
  | 'nor'
  | 'xnor'
  | 'cmp'
  | 'lea'
  ;

SingleOperandOpcode
  : 'pop'
  | 'push'
  | 'clr'
  | 'inc'
  | 'dec'
  | 'not'
  | 'j'
  | 'je'
  | 'jne'
  | 'jg'
  | 'jge'
  | 'jl'
  | 'jle'
  | 'call'
  | 'int'
  ;

NoOperandOpcode
  : 'hlt'
  | 'nop'
  | 'ret'
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
