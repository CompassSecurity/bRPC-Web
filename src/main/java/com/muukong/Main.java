package com.muukong;

import com.muukong.parsing.ProtoscopeParserVisitor;
import com.muukong.protobuf.PBDisassembler;
import com.muukong.protobuf.PBMessage;
import com.muukong.util.Util;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Base64;

/*
 * A simple parsing example :)
 */
public class Main {

    public static void main(String[] args) {

        String input ="""
        7: {"test vector abc 123"} # blubber
        7: {"321"}
        7: {"deadbeef"}
        12: {
            9: 2349656                          # this is a comment :)
            11: {"lorem"}
            11: {"ipsum"}
            11: {"foo"}
            11: {"bar"}
            11: {"test"}
            20: {
                1: 42
                3: {`f0f403a289`}
                5: { 1      23498973465 1337   }
            }
            33: {"yet another test string for youuuu"}
            128: 258469912
        }
        37: 24057234057""";

        ProtoscopeLexer lexer = new ProtoscopeLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ProtoscopeParser parser = new ProtoscopeParser(tokens);

        ParseTree tree = parser.message();
        ParseTreeWalker walker = new ParseTreeWalker();

        ProtoscopeParserVisitor visitor = new ProtoscopeParserVisitor();
        PBMessage message = (PBMessage) visitor.visit(tree);

        System.out.println(message.prettyPrint());
    }
}