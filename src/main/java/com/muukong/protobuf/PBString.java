package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.List;

public class PBString implements ISerializable, IParseable {

    private String value;

    public PBString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String prettyPrint() {
        return this.prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("{\"%s\"}", value);
    }

    @Override
    public byte[] serializeValue() {

        byte[] valueBytes = new byte[value.length()];

        for ( int i = 0; i < value.length(); ++i )
            valueBytes[i] = (byte) value.charAt(i);

        return valueBytes;
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        List<Byte> result = new ArrayList<>();

        // Prepend (field_number << 3) | wire_type
        long tag = ((long)fieldNumber << 3) | PBWireTypes.LEN;
        byte[] tagBytes = new PBVarInt(tag).serializeValue();
        result.addAll( Util.convertToList(tagBytes) );

        // Append string length encoded in VARINT format (without prefix)
        byte[] lengthBytes = new PBVarInt(value.length()).serializeValue();
        result.addAll(Util.convertToList(lengthBytes));

        // Append string value itself
        byte[] stringBytes = serializeValue();
        result.addAll(Util.convertToList(stringBytes));

        return Util.convertToArray(result);

    }
}
