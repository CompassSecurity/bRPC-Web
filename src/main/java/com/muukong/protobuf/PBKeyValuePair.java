package com.muukong.protobuf;

import com.muukong.parsing.IParseable;

/**
 * Represents a protobuf key value pair where key corresponds to the field number and the value corresponds
 * to the field value.
 */
public class PBKeyValuePair implements ISerializable, IParseable {

    int fieldNumber;
    ISerializable fieldValue;

    public PBKeyValuePair(int fieldNumber, ISerializable fieldValue) {
        this.fieldNumber = fieldNumber;
        this.fieldValue = fieldValue;
    }

    public int getFieldNumber() {
        return fieldNumber;
    }

    @Override
    public String prettyPrint() {
        return this.prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("%s%d: %s", indent, fieldNumber, fieldValue.prettyPrint(indent));
    }

    @Override
    public byte[] serializeValue() {
        return fieldValue.serializeValueWithFieldNumber(fieldNumber);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {
        return fieldValue.serializeValueWithFieldNumber(fieldNumber);
    }

}
