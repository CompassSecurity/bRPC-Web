package com.muukong.protobuf;

import com.muukong.parsing.IParseable;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a (top-level) protobuf message.
 */
public class PBMessage implements ISerializable, IParseable {

    private List<PBKeyValuePair> fields = new ArrayList<>();

    public PBMessage() {
        // nothing to do
    }

    public void addField(PBKeyValuePair field) {
        fields.add(field);
    }

    public PBKeyValuePair getField(int fieldNumber) {

        for ( PBKeyValuePair pair : fields ) {
            if ( pair.getFieldNumber() == fieldNumber )
                return pair;
        }

        return null;
    }

    @Override
    public String prettyPrint() {
        return this.prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < fields.size(); ++i ) {

            PBKeyValuePair pair = fields.get(i);
            sb.append(pair.prettyPrint(indent));

            if ( i + 1 < fields.size() ) // Append newline if it's not the last field
                sb.append("\n");
        }

        return sb.toString();

    }

    @Override
    public byte[] serializeValue() {

        if ( fields.size() == 0 ) { // Empty message
            return new byte[0];
        }

        List<Byte> result = new ArrayList<>();
        for ( PBKeyValuePair field : fields ) {
            byte[] tmp = field.serializeValue();
            result.addAll(Util.convertToList(tmp));
        }

        return Util.convertToArray(result);

    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {
        throw new RuntimeException("Protobuf messages do not have a field number; therefore, this method cannot be invoked on this object.");
    }

}
