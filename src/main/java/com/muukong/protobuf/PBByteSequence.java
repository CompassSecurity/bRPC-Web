package com.muukong.protobuf;

import com.muukong.parsing.IParseable;

import java.util.HexFormat;

/**
 * Represents a field of wire type LEN that cannot be recognized as anything more specific, such as
 * a string or a sub-message.
 */
public class PBByteSequence implements ISerializable, IParseable {

    int fieldNumber;
    private byte[] value;

    public PBByteSequence(int fieldNumber, byte[] value) {
        this.fieldNumber = fieldNumber;
        this.value = value;
    }

    @Override
    public String prettyPrint() {
        return prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("{`%s`}", HexFormat.of().formatHex(value));
    }

    @Override
    public byte[] serializeValue() {
        return serializeValueWithFieldNumber(fieldNumber);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        PBVarInt length = new PBVarInt(value.length, PBWireTypes.LEN);
        PBKeyValuePair lengthPair = new PBKeyValuePair(fieldNumber, length);

        byte[] lengthBytes = lengthPair.serializeValueWithFieldNumber(fieldNumber);

        byte[] result = new byte[lengthBytes.length + value.length];
        System.arraycopy(lengthBytes, 0, result, 0, lengthBytes.length);
        System.arraycopy(value, 0, result, lengthBytes.length, value.length);

        return result;
    }
}
