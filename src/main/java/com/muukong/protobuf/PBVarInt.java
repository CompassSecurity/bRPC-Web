package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class PBVarInt implements ISerializable, IParseable {

    private int wireType;
    private BigInteger value;

    public PBVarInt(int value) {
        this.wireType = PBWireTypes.VARINT;
        this.value = BigInteger.valueOf(value);
    }

    public PBVarInt(int value, int wireType) {
        this.wireType = wireType;
        this.value = BigInteger.valueOf(value);
    }

    public PBVarInt(long value) {
        this.wireType = PBWireTypes.VARINT;
        this.value = BigInteger.valueOf(value);
    }

    public PBVarInt(long value, int wireType) {
        this.wireType = wireType;
        this.value = BigInteger.valueOf(value);
    }

    public PBVarInt(BigInteger value) {
        this.wireType = PBWireTypes.VARINT;
        this.value = value;
    }

    public PBVarInt(BigInteger value, int wireType) {
        this.wireType = wireType;
        this.value = value;
    }

    public int toInt() {
        return value.intValue();
    }

    @Override
    public String prettyPrint() {
        return this.prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("%d", value);
    }

    @Override
    public byte[] serializeValue() {

        return serializeValue(wireType);
    }

    private byte[] serializeValue(int wireType) {

        List<Byte> result = new ArrayList<>();

        BigInteger fieldValueTmp = value;
        while ( fieldValueTmp.compareTo(BigInteger.valueOf(128)) != -1 ) { // while fieldValueTmp >= 128

            byte tmp = (byte) (fieldValueTmp.byteValue() & 0x7f);
            tmp |= 0x80;
            result.add(tmp);

            fieldValueTmp = fieldValueTmp.shiftRight(7);
        }
        result.add((byte) fieldValueTmp.byteValue());

        return Util.convertToArray(result);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        return serializeValueWithFieldNumber(fieldNumber, wireType);

    }

    private byte[] serializeValueWithFieldNumber(int fieldNumber, int wireType) {

        List<Byte> result = new ArrayList<Byte>();

        long tag = ((long) fieldNumber << 3) | wireType;
        byte[] tagBytes = new PBVarInt(tag).serializeValue();
        result.addAll(Util.convertToList(tagBytes));

        byte[] varInt = this.serializeValue();
        result.addAll(Util.convertToList(varInt));

        return Util.convertToArray(result);

    }
}
