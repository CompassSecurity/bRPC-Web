package com.muukong.protobuf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PBDisassemblerTest {

    // -----------------------------------------------------------------------
    // Helpers for building raw protobuf binary
    // -----------------------------------------------------------------------

    /** Encode a non-negative value as a protobuf base-128 varint. */
    private static byte[] varint(long v) {
        byte[] buf = new byte[10];
        int pos = 0;
        while (v >= 0x80) {
            buf[pos++] = (byte) ((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        buf[pos++] = (byte) (v & 0x7F);
        byte[] out = new byte[pos];
        System.arraycopy(buf, 0, out, 0, pos);
        return out;
    }

    /** Concatenate byte arrays. */
    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    /** Encode a value as 4 little-endian bytes (wire type I32). */
    private static byte[] le32(long v) {
        return new byte[]{
            (byte)  (v        & 0xFF),
            (byte) ((v >>  8) & 0xFF),
            (byte) ((v >> 16) & 0xFF),
            (byte) ((v >> 24) & 0xFF)
        };
    }

    /** Encode a value as 8 little-endian bytes (wire type I64). */
    private static byte[] le64(long v) {
        return new byte[]{
            (byte)  (v        & 0xFF),
            (byte) ((v >>  8) & 0xFF),
            (byte) ((v >> 16) & 0xFF),
            (byte) ((v >> 24) & 0xFF),
            (byte) ((v >> 32) & 0xFF),
            (byte) ((v >> 40) & 0xFF),
            (byte) ((v >> 48) & 0xFF),
            (byte) ((v >> 56) & 0xFF)
        };
    }

    /** Build a protobuf field: tag varint + raw value bytes. */
    private static byte[] field(int fieldNumber, int wireType, byte[] valueBytes) {
        return concat(varint(((long) fieldNumber << 3) | wireType), valueBytes);
    }

    /** Build a wire-type-2 (LEN) field: tag + length-varint + payload. */
    private static byte[] lenField(int fieldNumber, byte[] data) {
        return field(fieldNumber, PBWireTypes.LEN, concat(varint(data.length), data));
    }

    /** Disassemble and return the top-level prettyPrint output. */
    private static String pp(byte[] input) {
        return new PBDisassembler(input).disassemble().prettyPrint();
    }

    // -----------------------------------------------------------------------
    // Empty input
    // -----------------------------------------------------------------------

    @Test
    void emptyInput_producesEmptyMessage() {
        assertEquals("", pp(new byte[0]));
    }

    @Test
    void emptyMessage_serializesToZeroBytes() {
        PBMessage empty = new PBDisassembler(new byte[0]).disassemble();
        assertArrayEquals(new byte[0], empty.serializeValue());
    }

    @Test
    void emptySubMessage_roundTrip() {
        // A LEN field whose payload is an empty protobuf message (0 bytes).
        // Before the fix, PBMessage.serializeValue() returned 5 zero bytes for
        // empty messages, so this round-trip would produce a 5-byte payload instead.
        byte[] input = lenField(1, new byte[0]);   // field 1, length 0 → empty string {""}
        // The disassembler treats a zero-length LEN as an empty string.
        // Serialising that PBString back must reproduce the original 2-byte encoding.
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // VARINT fields
    // -----------------------------------------------------------------------

    @Test
    void varint_singleByte_value42() {
        byte[] input = field(1, PBWireTypes.VARINT, varint(42));
        assertEquals("1: 42", pp(input));
    }

    @Test
    void varint_multiByte_value150() {
        // 150 encodes as [0x96, 0x01] — the canonical varint example from the protobuf docs
        byte[] input = field(1, PBWireTypes.VARINT, varint(150));
        assertEquals("1: 150", pp(input));
    }

    @Test
    void varint_zero() {
        byte[] input = field(3, PBWireTypes.VARINT, varint(0));
        assertEquals("3: 0", pp(input));
    }

    // -----------------------------------------------------------------------
    // I32 fields
    // -----------------------------------------------------------------------

    @Test
    void i32_value300() {
        byte[] input = field(1, PBWireTypes.I32, le32(300));
        assertEquals("1: 300i32", pp(input));
    }

    @Test
    void i32_roundTrip() {
        byte[] input = field(2, PBWireTypes.I32, le32(0xDEADBEEFL));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // I64 fields
    // -----------------------------------------------------------------------

    @Test
    void i64_largePowerOfTen() {
        long value = 1_000_000_000_000L;
        byte[] input = field(1, PBWireTypes.I64, le64(value));
        assertEquals("1: " + value + "i64", pp(input));
    }

    @Test
    void i64_roundTrip() {
        byte[] input = field(1, PBWireTypes.I64, le64(0x0102030405060708L));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // LEN fields — string heuristic
    // -----------------------------------------------------------------------

    @Test
    void len_emptyPayload_treatedAsEmptyString() {
        byte[] input = lenField(1, new byte[0]);
        assertEquals("1: {\"\"}", pp(input));
    }

    @Test
    void len_asciiString_hello() {
        byte[] input = lenField(2, "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("2: {\"hello\"}", pp(input));
    }

    @Test
    void len_utf8MultiByteString() {
        // é = U+00E9 = 0xC3 0xA9 in UTF-8
        String s = "héllo";
        byte[] input = lenField(1, s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("1: {\"héllo\"}", pp(input));
    }

    @Test
    void len_stringWithTabInMiddle_treatedAsString() {
        // Tab is allowed anywhere except at the leading or trailing position
        byte[] input = lenField(1, "hel\tlo".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("1: {\"hel\tlo\"}", pp(input));
    }

    // -----------------------------------------------------------------------
    // LEN fields — leading/trailing whitespace control chars rejected as string
    // -----------------------------------------------------------------------

    @Test
    void len_leadingTab_notTreatedAsString() {
        byte[] input = lenField(1, "\thello".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(pp(input).contains("\""),
            "A leading tab must not produce a quoted string");
    }

    @Test
    void len_trailingNewline_notTreatedAsString() {
        byte[] input = lenField(1, "hello\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(pp(input).contains("\""),
            "A trailing newline must not produce a quoted string");
    }

    @Test
    void len_trailingCr_notTreatedAsString() {
        byte[] input = lenField(1, "hello\r".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertFalse(pp(input).contains("\""),
            "A trailing CR must not produce a quoted string");
    }

    // -----------------------------------------------------------------------
    // LEN fields — sub-message heuristic
    // -----------------------------------------------------------------------

    @Test
    void len_nestedMessage_decodedAsSubMessage() {
        // 0x08 (the tag byte for field-1 varint) is a control char < 0x09, so the
        // string heuristic rejects it and the sub-message attempt succeeds.
        byte[] inner = field(1, PBWireTypes.VARINT, varint(42));
        byte[] input = lenField(2, inner);
        assertEquals("2: {\n  1: 42\n}", pp(input));
    }

    @Test
    void len_subMessageWinsOverPacked_whenBothAreValid() {
        // Three repetitions of "field 1, varint N" are both valid as a sub-message
        // and as packed varints. The disassembler tries sub-message first.
        byte[] inner = concat(
            field(1, PBWireTypes.VARINT, varint(1)),
            field(1, PBWireTypes.VARINT, varint(2)),
            field(1, PBWireTypes.VARINT, varint(3))
        );
        byte[] input = lenField(2, inner);
        // A sub-message produces indented lines; a flat packed field does not.
        assertTrue(pp(input).contains("{\n"),
            "Sub-message interpretation (checked first) should win over packed repeated");
    }

    @Test
    void len_subMessage_roundTrip() {
        byte[] inner = field(1, PBWireTypes.VARINT, varint(99));
        byte[] input = lenField(2, inner);
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // LEN fields — packed repeated heuristic
    // -----------------------------------------------------------------------

    @Test
    void len_packedRepeatedVarInts_threeSingleByteValues() {
        // [0x01, 0x02, 0x03]:
        //   0x01 < 0x09  →  not a printable string
        //   tag=0x01 → field_number=0, wire_type=I64 → needs 8 bytes → sub-message fails
        //   three single-byte varints 1,2,3 → packed succeeds
        byte[] input = lenField(1, new byte[]{0x01, 0x02, 0x03});
        assertEquals("1: {1 2 3}", pp(input));
    }

    @Test
    void len_packedRepeated_roundTrip() {
        byte[] input = lenField(1, new byte[]{0x01, 0x02, 0x03});
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // LEN fields — byte-sequence fallback
    // -----------------------------------------------------------------------

    @Test
    void len_incompleteVarint_fallsBackToByteSequence() {
        // 0x80 has the continuation bit set but is the only byte — an incomplete varint.
        // Not valid UTF-8; fails sub-message; fails packed → raw byte sequence.
        byte[] input = lenField(1, new byte[]{(byte) 0x80});
        assertEquals("1: {`80`}", pp(input));
    }

    @Test
    void len_byteSequence_roundTrip() {
        byte[] data = new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        byte[] input = lenField(1, data);
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // Multiple fields in one message
    // -----------------------------------------------------------------------

    @Test
    void multipleFields_varintThenString() {
        byte[] input = concat(
            field(1, PBWireTypes.VARINT, varint(42)),
            lenField(2, "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        assertEquals("1: 42\n2: {\"hello\"}", pp(input));
    }

    @Test
    void multipleFields_allWireTypes() {
        byte[] input = concat(
            field(1, PBWireTypes.VARINT, varint(7)),
            field(2, PBWireTypes.I32,   le32(42)),
            field(3, PBWireTypes.I64,   le64(123)),
            lenField(4, "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        String result = pp(input);
        assertTrue(result.contains("1: 7"));
        assertTrue(result.contains("2: 42i32"));
        assertTrue(result.contains("3: 123i64"));
        assertTrue(result.contains("4: {\"abc\"}"));
    }

    // -----------------------------------------------------------------------
    // Invalid / unsupported wire types
    // -----------------------------------------------------------------------

    @Test
    void invalidWireType_throws() {
        // Wire type 6 is not defined in the protobuf spec.
        byte[] input = varint((1L << 3) | 6);
        assertThrows(RuntimeException.class,
            () -> new PBDisassembler(input).disassemble());
    }

    @Test
    void sgroupWireType_throws() {
        byte[] input = varint((1L << 3) | PBWireTypes.SGROUP);
        assertThrows(RuntimeException.class,
            () -> new PBDisassembler(input).disassemble());
    }

    @Test
    void egroupWireType_throws() {
        byte[] input = varint((1L << 3) | PBWireTypes.EGROUP);
        assertThrows(RuntimeException.class,
            () -> new PBDisassembler(input).disassemble());
    }

    // -----------------------------------------------------------------------
    // Round-trip: disassemble(binary).serializeValue() == binary
    // -----------------------------------------------------------------------

    @Test
    void roundTrip_varint() {
        byte[] input = field(1, PBWireTypes.VARINT, varint(12345));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    @Test
    void roundTrip_i32() {
        byte[] input = field(1, PBWireTypes.I32, le32(300));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    @Test
    void roundTrip_i64() {
        byte[] input = field(1, PBWireTypes.I64, le64(1_000_000_000_000L));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    @Test
    void roundTrip_string() {
        byte[] input = lenField(1, "round-trip".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    @Test
    void roundTrip_multipleFields_allWireTypes() {
        // Also includes a nested sub-message in field 4
        byte[] input = concat(
            field(1, PBWireTypes.VARINT, varint(1)),
            field(2, PBWireTypes.I32,   le32(2)),
            lenField(3, "three".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            lenField(4, field(1, PBWireTypes.VARINT, varint(4)))   // nested sub-message
        );
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    // -----------------------------------------------------------------------
    // Large integration test: 32 top-level fields, 8 levels of nesting
    // -----------------------------------------------------------------------

    /**
     * Builds the shared binary fixture used by the big-message tests.
     *
     * Structure:
     *   Depth 1  — top-level message (field 29 contains depth 2)
     *   Depth 2  — field 2 contains depth 3  …
     *   Depth 8  — innermost; no further nesting
     *
     * Each nested level starts with a VARINT field so that the first byte of the
     * encoded sub-message is tag 0x08 (ASCII backspace, < 0x09), ensuring the
     * string heuristic is rejected and the sub-message path is always taken.
     */
    private static byte[] buildBigMessageInput() {
        java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;

        byte[] depth8 = concat(
            field(1, PBWireTypes.VARINT, varint(8888)),
            lenField(3, "depth8".getBytes(utf8)),
            field(4, PBWireTypes.I32,   le32(88))
        );
        byte[] depth7 = concat(
            field(1, PBWireTypes.VARINT, varint(7777)),
            lenField(2, depth8),
            lenField(3, "depth7".getBytes(utf8)),
            field(4, PBWireTypes.I64,   le64(77))
        );
        byte[] depth6 = concat(
            field(1, PBWireTypes.VARINT, varint(6666)),
            lenField(2, depth7),
            lenField(3, "depth6".getBytes(utf8)),
            field(4, PBWireTypes.I32,   le32(66))
        );
        byte[] depth5 = concat(
            field(1, PBWireTypes.VARINT, varint(5555)),
            lenField(2, depth6),
            lenField(3, "depth5".getBytes(utf8)),
            field(4, PBWireTypes.I64,   le64(55))
        );
        byte[] depth4 = concat(
            field(1, PBWireTypes.VARINT, varint(4444)),
            lenField(2, depth5),
            lenField(3, "depth4".getBytes(utf8)),
            field(4, PBWireTypes.I32,   le32(44))
        );
        byte[] depth3 = concat(
            field(1, PBWireTypes.VARINT, varint(3333)),
            lenField(2, depth4),
            lenField(3, "depth3".getBytes(utf8)),
            field(4, PBWireTypes.I64,   le64(33))
        );
        byte[] depth2 = concat(
            field(1, PBWireTypes.VARINT, varint(2222)),
            lenField(2, depth3),
            lenField(3, "depth2".getBytes(utf8)),
            field(4, PBWireTypes.I32,   le32(22))
        );

        return concat(
            // VARINTs — single-byte, multi-byte, and boundary values
            field( 1, PBWireTypes.VARINT, varint(1)),
            field( 2, PBWireTypes.VARINT, varint(150)),
            field( 3, PBWireTypes.VARINT, varint(300)),
            field( 4, PBWireTypes.VARINT, varint(0)),
            field( 5, PBWireTypes.VARINT, varint(127)),
            field( 6, PBWireTypes.VARINT, varint(128)),
            field( 7, PBWireTypes.VARINT, varint(16383)),
            field( 8, PBWireTypes.VARINT, varint(16384)),
            field( 9, PBWireTypes.VARINT, varint(2097151)),
            field(10, PBWireTypes.VARINT, varint(2097152)),
            // I32
            field(11, PBWireTypes.I32, le32(0)),
            field(12, PBWireTypes.I32, le32(300)),
            field(13, PBWireTypes.I32, le32(0x7FFFFFFFL)),
            field(14, PBWireTypes.I32, le32(0xDEADBEEFL)),
            field(15, PBWireTypes.I32, le32(12345678)),
            // I64
            field(16, PBWireTypes.I64, le64(0)),
            field(17, PBWireTypes.I64, le64(1_000_000_000_000L)),
            field(18, PBWireTypes.I64, le64(Long.MAX_VALUE)),
            field(19, PBWireTypes.I64, le64(-1L)),
            field(20, PBWireTypes.I64, le64(987654321098765L)),
            // Strings
            lenField(21, "".getBytes(utf8)),
            lenField(22, "hello".getBytes(utf8)),
            lenField(23, "world".getBytes(utf8)),
            lenField(24, "héllo".getBytes(utf8)),
            lenField(25, "tab\tinside".getBytes(utf8)),
            lenField(26, "multiple words".getBytes(utf8)),
            lenField(27, "0123456789".getBytes(utf8)),
            lenField(28, "special !@#$".getBytes(utf8)),
            // Deeply nested sub-message (8 levels)
            lenField(29, depth2),
            // More fields after the nested part
            field(30, PBWireTypes.VARINT, varint(9999)),
            lenField(31, "after-nested".getBytes(utf8)),
            field(32, PBWireTypes.I32,   le32(42))
        );
    }

    @Test
    void roundTrip_bigMessage() {
        byte[] input = buildBigMessageInput();
        assertArrayEquals(input, new PBDisassembler(input).disassemble().serializeValue());
    }

    @Test
    void bigMessage_32topLevelFields_8levelsDeep() {
        byte[] input = buildBigMessageInput();

        String result = pp(input);

        // All 32 top-level fields appear as lines starting with "<number>: "

        // (indented inner lines and closing braces do not match this pattern)
        long topLevelFieldCount = result.lines()
            .filter(line -> line.matches("\\d+: .*"))
            .count();
        assertEquals(32, topLevelFieldCount);

        // Spot-check representative top-level values across all wire types
        assertTrue(result.contains("2: 150"));
        assertTrue(result.contains("6: 128"));
        assertTrue(result.contains("12: 300i32"));
        assertTrue(result.contains("17: 1000000000000i64"));
        assertTrue(result.contains("19: -1i64"));
        assertTrue(result.contains("22: {\"hello\"}"));
        assertTrue(result.contains("25: {\"tab\tinside\"}"));
        assertTrue(result.contains("30: 9999"));
        assertTrue(result.contains("32: 42i32"));

        // The string "depth8" must appear, proving the parser reached level 8
        assertTrue(result.contains("depth8"));

        // depth-8 content is indented by 7 × 2 = 14 spaces
        assertTrue(result.contains("              1: 8888"));
        assertTrue(result.contains("              3: {\"depth8\"}"));
    }
}
