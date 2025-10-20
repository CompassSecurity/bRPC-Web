package com.muukong.parsing;

import com.muukong.ProtoscopeLexer;
import com.muukong.ProtoscopeParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

public class ProtoParser {

    private final String input;

    public ProtoParser(String input) {
        this.input = input;
    }

    public ParserResult parse() throws ParseCancellationException {

        ProtoscopeLexer lexer = new ProtoscopeLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ProtoscopeParser parser = new ProtoscopeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        ParseTree tree = parser.requestResponse();

        ProtoscopeParserVisitor visitor = new ProtoscopeParserVisitor();
        ParserResult res = (ParserResult) visitor.visit(tree);

        return res;
    }

}
