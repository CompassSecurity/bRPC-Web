package com.muukong.parsing;

import com.muukong.ProtoscopeBaseVisitor;
import com.muukong.ProtoscopeParser;
import com.muukong.grpcweb.HttpHeader;
import com.muukong.grpcweb.TrailingHeaders;
import com.muukong.protobuf.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class ProtoscopeParserVisitor extends ProtoscopeBaseVisitor<IParseable> {

    @Override
    public IParseable visitRequestResponse(ProtoscopeParser.RequestResponseContext ctx) {


        List<PBMessage> messages = new ArrayList<>();
        TrailingHeaders trailingHeaders = new TrailingHeaders();

        for ( ParseTree child : ctx.children) {

            IParseable tmp = visit(child);
            if ( tmp instanceof PBMessage ) {
                PBMessage message = (PBMessage) tmp;
                messages.add(message);
            } else if ( tmp instanceof TrailingHeaders) {
                trailingHeaders = (TrailingHeaders) tmp;
            } else {
                // skip
            }
        }

        return new ParserResult(messages, trailingHeaders);
    }

    @Override
    public IParseable visitMessage(ProtoscopeParser.MessageContext ctx) {

        PBMessage message = new PBMessage();

        for ( ParseTree child : ctx.children ) {

            PBKeyValuePair pair = (PBKeyValuePair) visit(child);
            message.addField(pair);
        }

        return message;
    }

    @Override
    public IParseable visitKeyValuePair(ProtoscopeParser.KeyValuePairContext ctx) {

        int fieldNumber = Integer.valueOf(ctx.getChild(0).getText());
        ISerializable fieldValue = (ISerializable) visit(ctx.getChild(2));

        return new PBKeyValuePair(fieldNumber, fieldValue);
    }

    @Override
    public IParseable visitSubMessage(ProtoscopeParser.SubMessageContext ctx) {

        ParseTree pt = ctx.children.get(1);
        PBMessage msg = (PBMessage) visit(pt);
        return new PBSubMessage(msg);
    }

    @Override
    public IParseable visitVarInt(ProtoscopeParser.VarIntContext ctx) {

        BigInteger bigInt = new BigInteger(ctx.Integer().getText());
        return new PBVarInt(bigInt);

    }

    @Override
    public IParseable visitNonVarInt32(ProtoscopeParser.NonVarInt32Context ctx) {

        String tmp = ctx.NonVarInt32().getText();
        tmp = tmp.substring(0, tmp.length() - 3); // drop "i32" suffix
        return new PBNonVarInt32(Long.valueOf(tmp));

    }

    @Override
    public IParseable visitNonVarInt64(ProtoscopeParser.NonVarInt64Context ctx) {

        // Warning: this might not work for the largest possible NonVarInt64 values
        String tmp = ctx.NonVarInt64().getText();
        tmp = tmp.substring(0, tmp.length() - 3); // drop "i64" suffix
        return new PBNonVarInt64(Long.valueOf(tmp));

    }

    @Override
    public IParseable visitStringLiteral(ProtoscopeParser.StringLiteralContext ctx) {

        String stringLiteral = ctx.StringLiteral().getText();

        // Drop {" and "} of literal at the beginning and end, respectively
        return new PBString(stringLiteral.substring(2, stringLiteral.length() - 2));
    }

    @Override
    public IParseable visitHexString(ProtoscopeParser.HexStringContext ctx) {

        String hexLiteral = ctx.HexString().getText();
        String hexCharacters = hexLiteral.substring(2, hexLiteral.length()-2); // Drop {` and `}

        return new PBByteSequence(-1, HexFormat.of().parseHex(hexCharacters));
    }

    @Override
    public IParseable visitPRF(ProtoscopeParser.PRFContext ctx) {

        PBPackedRepeatedField prf = new PBPackedRepeatedField();
        for ( int i = 1; i < ctx.children.size() - 1; ++i ) {

            ParseTree tree = ctx.getChild(i);
            PBVarInt tmp = (PBVarInt) visit(tree);
            prf.addField(tmp);

        }

        return prf;
    }

    @Override
    public IParseable visitPrfInitializer(ProtoscopeParser.PrfInitializerContext ctx) {
        BigInteger bigInt = new BigInteger(ctx.Integer().getText());
        return new PBVarInt(bigInt);
    }

    @Override
    public IParseable visitTrailingHeaders(ProtoscopeParser.TrailingHeadersContext ctx) {

        TrailingHeaders trailingHeaders = new TrailingHeaders();

        for ( int i = 1; i < ctx.children.size() - 1; ++i ) {

            HttpHeader header = (HttpHeader) visit(ctx.children.get(i));
            trailingHeaders.addHeader(header);

        }

        return trailingHeaders;
    }

    @Override
    public IParseable visitHeader(ProtoscopeParser.HeaderContext ctx) {

        String headerKey = ctx.children.get(0).getText();
        headerKey = headerKey.substring(1, headerKey.length() - 1);

        String headerValue = ctx.children.get(2).getText();
        headerValue = headerValue.substring(1, headerValue.length() - 1);

        return new HttpHeader(headerKey, headerValue);
    }

}
