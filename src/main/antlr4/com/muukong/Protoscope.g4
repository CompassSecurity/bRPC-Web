grammar Protoscope;

requestResponse
    : message? (';' message)* ';'? trailingHeaders
    ;

message
    : keyValuePair+
    ;

keyValuePair
    : Integer ':' fieldValue // (field_number : field_value)+
    ;

fieldValue
    : ('{\r\n' | '{\n')  message '}'  # SubMessage // We allow both LF and CRLF files (the latter is needed for Burp)
    | '{' (prfInitializer)+ '}' # PRF
    | Integer                   # VarInt
    | NonVarInt32               # NonVarInt32
    | NonVarInt64               # NonVarInt64
    | StringLiteral             # StringLiteral
    | HexString                 # HexString
    ;

prfInitializer
    : Integer
    ;

trailingHeaders
    :   '[' header* ']'
    ;

header
    : (HeaderString ':' HeaderString)
    ;

HeaderString
    : '"' StringCharacters? '"'
    ;

Integer
     :  NonzeroDigit Digits?
     ;

NonVarInt32
    :   NonzeroDigit Digits? 'i32'
    ;

NonVarInt64
    :   NonzeroDigit Digits? 'i64'
    ;

StringLiteral
    : '{"' StringCharacters? '"}'
    ;

fragment
StringCharacters
    : StringCharacter+
    ;

fragment
StringCharacter
	:	~["\\]
	|	EscapeSequence
	;

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
    |   UnicodeEscape // This is not in the spec but prevents having to preprocess the input
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
OctalDigit
	:	[0-7]
	;

HexString
    : '{`' HexCharacters? '`}'
    ;

HexCharacters
    :   HexDigit+
    ;

Digits
    : [0-9]+
    ;

fragment
Digit
    :   [0-9]
    ;

fragment
NonzeroDigit
    :   [1-9]
    ;

fragment
HexDigit
    :   [0-9a-fA-F]
    ;

WS
    :   [ \t\r\n]+ -> skip
    ;

LINE_COMMENT
    :   '#' ~[\r\n]* -> channel(HIDDEN)
    ;
