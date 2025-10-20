package com.muukong.protobuf;

/**
 * Protobuf wire types as defined in https://protobuf.dev/programming-guides/encoding/#structure
 */
public class PBWireTypes {
    public final static int VARINT = 0;
    public final static int I64 = 1;
    public final static int LEN = 2;
    public final static int SGROUP = 3; // deprecated and thus currently not supported
    public final static int EGROUP = 4; // deprecated and thus currently not supported
    public final static int I32 = 5;
};