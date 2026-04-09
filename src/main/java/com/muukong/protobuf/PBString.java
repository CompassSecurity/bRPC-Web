package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.nio.charset.StandardCharsets;
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
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        List<Byte> result = new ArrayList<>();

        // Prepend (field_number << 3) | wire_type
        long tag = ((long)fieldNumber << 3) | PBWireTypes.LEN;
        byte[] tagBytes = new PBVarInt(tag).serializeValue();
        result.addAll( Util.convertToList(tagBytes) );

        // Append string byte length encoded in VARINT format (use byte count, not char count)
        byte[] stringBytes = serializeValue();
        byte[] lengthBytes = new PBVarInt(stringBytes.length).serializeValue();
        result.addAll(Util.convertToList(lengthBytes));

        // Append string value itself
        result.addAll(Util.convertToList(stringBytes));

        return Util.convertToArray(result);

    }
}
