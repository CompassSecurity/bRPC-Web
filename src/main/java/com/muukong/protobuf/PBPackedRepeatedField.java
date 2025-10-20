package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a packed repeated file (i.e., wire type LEN) as defined in
 * https://protobuf.dev/programming-guides/encoding/#packed.
 */
public class PBPackedRepeatedField implements ISerializable, IParseable {

    private List<PBVarInt> fields = new ArrayList<>();

    public void addField(PBVarInt field) {
        fields.add(field);
    }

    @Override
    public String prettyPrint() {
        return prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        for ( int i = 0; i < fields.size(); ++i ) {

            ISerializable f = fields.get(i);
            sb.append(f.prettyPrint());

            if ( i + 1 < fields.size() )    // Append space except for last element
                sb.append(" ");
        }
        sb.append("}");

        return sb.toString();
    }

    @Override
    public byte[] serializeValue() {

        List<Byte> result = new ArrayList<>();
        for ( PBVarInt field : fields ) {
            byte[] tmp = field.serializeValue();
            result.addAll(Util.convertToList(tmp));
        }

        return Util.convertToArray(result);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        byte[] fieldBytes = serializeValue();
        final int length = fieldBytes.length;

        PBKeyValuePair prefix = new PBKeyValuePair(fieldNumber, new PBVarInt(length, PBWireTypes.LEN));
        byte[] prefixBytes = prefix.serializeValue();

        byte[] result = new byte[prefixBytes.length + fieldBytes.length];

        System.arraycopy(prefixBytes, 0, result, 0, prefixBytes.length);
        System.arraycopy(fieldBytes, 0, result, prefixBytes.length, fieldBytes.length);

        return result;
    }
}
