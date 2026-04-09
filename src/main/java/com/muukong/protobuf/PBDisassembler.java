package com.muukong.protobuf;

import com.muukong.util.Util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * A disassembler for protobuf messages that on input a protobuf message attempts to disassemble it
 * based on heuristics. No protobuf message files are required.
 */
public class PBDisassembler {

    private final byte[] input;     // Protobuf message to disassemble
    private int cursor = 0;         // Points to the next character that should be processed

    private PBMessage message = new PBMessage();    // Disassembled protobuf message

    public PBDisassembler(byte[] input) {
        this.input = input;
    }

    public PBMessage disassemble() {

        while ( hasNext() ) {

            /*
             Extract field_number and wire_type from current VARINT
             (see https://protobuf.dev/programming-guides/encoding/#structure)
            */

            final int tag = disassembleVarInt().toInt();
            final int fieldNumber = (tag >> 3) & 0x1fffffff;
            final int wireType = tag & 0x7;


            // Disassemble field based on wire type
            ISerializable fieldValue;
            switch ( wireType ) {
                case PBWireTypes.VARINT:
                    fieldValue = disassembleVarInt(); break;
                case PBWireTypes.I64:
                    fieldValue = disassembleI64(); break;
                case PBWireTypes.LEN:
                    fieldValue = disassembleLen(fieldNumber); break;
                case PBWireTypes.SGROUP:
                    throw new RuntimeException("Wire type SGROUP is deprecated and currently not supported.");
                case PBWireTypes.EGROUP:
                    throw new RuntimeException("Wire type EGROUP is deprecated and currently not supported.");
                case PBWireTypes.I32:
                    fieldValue = disassembleI32(); break;
                default:
                    throw new RuntimeException("Invalid wire type found during parsing: " + wireType);
            }

            message.addField(new PBKeyValuePair(fieldNumber, fieldValue));  // Add disassembled field

        }

        return message;
    }

    /**
     * Disassembles a field of type Base 128 VARINT (see https://protobuf.dev/programming-guides/encoding/#varints)
     *
     * @return PBVarInt object
     */
    private PBVarInt disassembleVarInt() {

        BigInteger result = BigInteger.valueOf(0);

        for ( int i = 0; i < input.length; ++i ) {

            final byte token = input[cursor];

            BigInteger tmp = BigInteger.valueOf((byte) (token & 0x7f));  // mask continuation bit
            tmp = tmp.shiftLeft(7 * i);                               // shift value (it's base 128)
            result = result.add(tmp);

            ++cursor;

            if ( (token & 0x80) == 0 ) {    // if continuation bit is not set, we are done

                PBVarInt varInt = new PBVarInt(result);
                return varInt;
            }
        }

        // We should never reach this state as it means that we have exhausted the input but have not
        // yet found a valid VARINT.
        throw new RuntimeException("Invalid input encountered while parsing VARINT.");
    }

    /**
     * Disassembles a field of wire type I64 (see https://protobuf.dev/programming-guides/encoding/#non-varints)
     *
     * @return PBNonVarInt64 object
     */
    private PBNonVarInt64 disassembleI64() {

        byte[] tmp = new byte[8];
        System.arraycopy(input, cursor, tmp, 0, 8);
        cursor += 8;

        return new PBNonVarInt64(tmp);
    }

    /**
     * Disassembles a field of wire type I32 (see https://protobuf.dev/programming-guides/encoding/#non-varints)
     *
     * @return PBNonVarInt32 object
     */
    private PBNonVarInt32 disassembleI32() {

        byte[] tmp = new byte[4];
        System.arraycopy(input, cursor, tmp, 0, 4);
        cursor += 4;

        return new PBNonVarInt32(tmp);
    }

    /**
     * Attempt to parse bytes as packed repeated fields (see https://protobuf.dev/programming-guides/encoding/#packed).
     *
     *  If it fails, the state is restored (i.e., the state is exactly as it was before the method was invoked).
     *
     * @param length    number of bytes that make up the packed repeated fields
     * @return Returns a PackedRepeatedField object on success, and null otherwise
     */
    private PBPackedRepeatedField attemptDisassembleAsPackedRepeatedFields(int length) {

        final int cursorBackup = cursor;

        try {

            PBPackedRepeatedField prf = new PBPackedRepeatedField();

            while ( cursor < cursorBackup + length ) {
                PBVarInt tmp = disassembleVarInt();
                prf.addField(tmp);
            }

            if ( isRemainingMessageParseable() ) {
                return prf;
            } else {
                cursor = cursorBackup;  // Restore state (TODO: is this necessary?)
                return null;
            }
        } catch ( Exception ex ) {
            cursor = cursorBackup;  // Restore state
            return null;
        }

    }

    /**
     * Attempt to parse bytes as a sub-message (see https://protobuf.dev/programming-guides/encoding/#packed).
     *
     *  If it fails, the state is restored (i.e., the state is exactly as it was before the method was invoked).
     *
     * @param length    number of bytes that make up the packed repeated fields
     * @return Returns a PBSubMessageobject on success, and null otherwise
     */
    private PBSubMessage attemptDisassembleAsSubMessage(int length) {

        byte[] data = new byte[length];
        System.arraycopy(input, cursor, data, 0, length);

        int cursorBackup = cursor;
        try {

            PBDisassembler p = new PBDisassembler(data);
            PBMessage message = p.disassemble();

            cursor += length;

            if ( isRemainingMessageParseable() ) {
                return new PBSubMessage(message);
            } else {
                cursor = cursorBackup;  // Restore state (TODO: is this necessary?)
                return null;
            }
        } catch ( Exception ex ) {
            cursor = cursorBackup;  // Restore state
            return null;
        }
    }

    /**
     * This method is used for wire type LEN when all other disassembling attempts (e.g., string,
     * packed repeated fields) have failed. The method simply consumes `length` bytes and store them. This
     * never fails (for valid protobuf messages).
     *
     * @param length    number of bytes that make up the packed repeated fields
     * @return Returns a PBByteSequence object
     */
    private PBByteSequence disassembleAsByteSequence(int fieldNumber, int length) {

        byte[] byteSequence = new byte[length];
        System.arraycopy(input, cursor, byteSequence, 0, length);

        cursor += length;

        return new PBByteSequence(fieldNumber, byteSequence);
    }

    /**
     * Disassembles a field of wire type LEN(see https://protobuf.dev/programming-guides/encoding/#non-varints). As
     * there are multiple ways to interpret such a field (string, sub-message, packed repeated field, byte sequence),
     * heuristics are used to determine the best way to disassemble it.
     *
     * @return Returns either of the following: PBString, PBSubMessage, PBPackedRepeatedFields, or
     *         PBByteSequence
     */
    private ISerializable disassembleLen(int fieldNumber) {

        final int length = disassembleVarInt().toInt();   // We can (rather) safely assume that the length fits into an int

        // An empty LEN field is unambiguously an empty string — skip all heuristics.
        if ( length == 0 ) {
            return new PBString("");
        }

        /*
         The following heuristic is employed:
         (1) If all characters are printable UTF-8 characters, disassemble it as a string and exit.
         (2) Otherwise, attempt to parse bytes as sub-message. Exit on success.
         (3) Otherwise, attempt to parse message as packed repeated fields. Exit on success.
         (4) Otherwise, consume `length` bytes (this should always succeed) and continue
        */

        // Case (1): try to decode as UTF-8 and verify all characters are printable
        boolean isString = false;
        String stringValue = null;
        {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                                                     .onMalformedInput(CodingErrorAction.REPORT)
                                                     .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                stringValue = decoder.decode(ByteBuffer.wrap(input, cursor, length)).toString();
                // Reject strings that contain non-printable control characters
                // (allow common whitespace: tab=0x09, LF=0x0A, CR=0x0D)
                isString = true;
                for ( int i = 0; i < stringValue.length(); ++i ) {
                    char c = stringValue.charAt(i);
                    if ( c < 0x09 || (c > 0x0D && c < 0x20) || c == 0x7F ) {
                        isString = false;
                        break;
                    }
                }
            } catch ( CharacterCodingException e ) {
                isString = false;
            }
        }

        if ( isString ) {
            cursor += length;
            return new PBString( stringValue );
        }

        // Case (2)
        PBSubMessage subMessage = attemptDisassembleAsSubMessage(length);
        if ( subMessage != null )
            return subMessage;

        // Case (3)
        PBPackedRepeatedField prf = attemptDisassembleAsPackedRepeatedFields(length);
        if ( prf != null )
            return prf;

        // Case (4)
        PBByteSequence pbs = disassembleAsByteSequence(fieldNumber, length);
        return pbs;
    }

    /**
     * While disassembling the message, it can happen that we disassemble a part of the message incorrectly
     * (e.g., we interpret something as a sub-message instead of packed repeated fields). This can work locally
     * but render the remainder of the message invalid (i.e. it cannot be disassembled). This method checks if
     * the remaining message can be parsed without errors.
     *
     * @return true if remaining message can be disassembled, false otherwise
     */
    private boolean isRemainingMessageParseable() {

        final int cursorBackup = cursor;

        // Copy remaining message to `bytesLeft`
        byte[] bytesLeft = new byte[input.length - cursor];
        System.arraycopy(input, cursor, bytesLeft, 0, input.length - cursor);

        // Initialize new disassembler instance
        PBDisassembler p = new PBDisassembler(bytesLeft);

        // Attempt to disassemble remaining message
        try {
            PBMessage m = p.disassemble();
        } catch ( Exception ex ) { // Fail
            cursor = cursorBackup;
            return false;
        }

        return true;    // Success
    }

    /**
     * Checks if there is non-processed input left.
     *
     * @return true if there is some unprocessed input left, false otherwise
     */
    private boolean hasNext() {
        return cursor < input.length;
    }

}
