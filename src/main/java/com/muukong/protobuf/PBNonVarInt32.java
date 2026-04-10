package com.muukong.protobuf;

import com.muukong.parsing.IParseable;

import java.util.Arrays;

/**
 * Represents a NonVarInt32 as defined in https://protobuf.dev/programming-guides/encoding/#non-varints
 */
public class PBNonVarInt32 implements ISerializable, IParseable {

    private final static int FIELD_VALUE_LENGTH = 4;

    private byte[] value;

    public PBNonVarInt32(long value) {

        this.value = new byte[FIELD_VALUE_LENGTH];
        for ( int i = 0; i < FIELD_VALUE_LENGTH; ++i ) // Little-endian byte order
            this.value[i] = (byte) ( (value >> 8*i) & 0xff );
    }

    public PBNonVarInt32(byte[] value) {

        if ( value.length != FIELD_VALUE_LENGTH )
            throw new RuntimeException("The parameter `value` must have length 8.");

        this.value = Arrays.copyOf(value, value.length);
    }

    @Override
    public String prettyPrint() {
        return prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {

        long result = 0;
        for ( int i = 0; i < FIELD_VALUE_LENGTH; ++i ) {
            long tmp = ((long) value[i]) & 0xff;
            result += (tmp << (8 * i));
        }

        return String.format("%di32", result);
    }

    @Override
    public byte[] serializeValue() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        long tag = ((long)fieldNumber << 3) | PBWireTypes.I32;
        byte[] tagBytes = new PBVarInt(tag).serializeValue();

        byte[] result = new byte[tagBytes.length + FIELD_VALUE_LENGTH];

        System.arraycopy(tagBytes, 0, result, 0, tagBytes.length);
        System.arraycopy(value, 0, result, tagBytes.length, FIELD_VALUE_LENGTH);

        return result;
    }
}
