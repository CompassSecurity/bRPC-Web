package com.muukong.protobuf;

public interface ISerializable {

    /**
     * Pretty prints field in protoscope format (see https://github.com/protocolbuffers/protoscope/blob/main/language.txt)
     *
     * @return String with pretty printed text
     */
    String prettyPrint();

    /**
     * Pretty prints field in protoscope format (see https://github.com/protocolbuffers/protoscope/blob/main/language.txt)
     *
     * @param indent a string that allows to add content before the field
     * @return String with (indented) pretty printed text
     */
    String prettyPrint(final String indent);

    /**
     * Encodes a protobuf field (message) according to https://protobuf.dev/programming-guides/encoding/
     *
     *
     * @return Byte string containing encoded field
     */
    byte[] serializeValue();

    /**
     * Encodes a protobuf field (message) according to https://protobuf.dev/programming-guides/encoding/
     *
     * @param fieldNumber The field number that should be used for encoding the message (this is sometimes necessary
     *                    as certain wire types cannot be serialized independently of the field number)
     * @return
     */
    byte[] serializeValueWithFieldNumber(int fieldNumber);

}
