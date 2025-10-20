package com.muukong.grpcweb;

import com.muukong.parsing.IParseable;
import com.muukong.protobuf.ISerializable;

public class HttpHeader implements ISerializable, IParseable {

    private String key;

    public HttpHeader(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public HttpHeader(HttpHeader header) {
        this.key = header.getKey();
        this.value = header.getValue();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;


    @Override
    public String prettyPrint() {
        return prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {
        return String.format("%s\"%s\": \"%s\"", indent, key, value);
    }

    @Override
    public byte[] serializeValue() {
        return this.serializeValueWithFieldNumber(-1);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {
        return String.format("%s:%s\r\n", key, value).getBytes();
    }
}
