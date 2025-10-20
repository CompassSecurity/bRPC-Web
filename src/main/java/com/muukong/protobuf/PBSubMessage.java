package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.List;

public class PBSubMessage implements ISerializable, IParseable {

    private final static String INDENT = "  "; // double space indentation

    private PBMessage message;

    public PBSubMessage() {
        message = new PBMessage();
    }

    public PBSubMessage(PBMessage message) {
        this.message = message;
    }

    public void addField(PBKeyValuePair pair) {
        message.addField(pair);
    }

    @Override
    public String prettyPrint() {
        return this.prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("{\n%s\n%s}", message.prettyPrint(indent + INDENT), indent);
    }

    @Override
    public byte[] serializeValue() {

        return message.serializeValue();

    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        List<Byte> result = new ArrayList<>();

        long tag = ((long)fieldNumber << 3) | PBWireTypes.LEN;
        byte[] tagBytes = new PBVarInt(tag).serializeValue();

        byte[] messageBytes = message.serializeValue();
        byte[] messageLengthBytes = new PBVarInt(messageBytes.length).serializeValue();

        result.addAll(Util.convertToList(tagBytes));
        result.addAll(Util.convertToList(messageLengthBytes));
        result.addAll(Util.convertToList(messageBytes));

        return Util.convertToArray(result);

    }
}
