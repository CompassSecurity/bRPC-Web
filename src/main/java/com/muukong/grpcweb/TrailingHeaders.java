package com.muukong.grpcweb;

import com.muukong.parsing.IParseable;
import com.muukong.protobuf.ISerializable;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.List;

public class TrailingHeaders implements ISerializable, IParseable {

    private List<HttpHeader> headers = new ArrayList<>();

    public void addHeader(HttpHeader header) {
        headers.add(header);
    }

    public void addHeaders(List<HttpHeader> headers) {
        this.headers.addAll(headers);
    }

    public void clearHeaders() {
        headers.clear();
    }

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    @Override
    public String prettyPrint() {
        return prettyPrint("");
    }

    @Override
    public String prettyPrint(String indent) {

        StringBuilder sb = new StringBuilder();

        sb.append(indent + "[\n");
        for ( HttpHeader h : headers ) {
            sb.append(h.prettyPrint(indent + "\t"));
            sb.append("\n");
        }
        sb.append(indent + "]");

        return sb.toString();
    }

    @Override
    public byte[] serializeValue() {
        return serializeValueWithFieldNumber(-1);
    }

    @Override
    public byte[] serializeValueWithFieldNumber(int fieldNumber) {

        if ( headers.size() == 0 ) {
            return new byte[0]; // return empty byte string if there are no trailing headers
        }

        List<Byte> trailingHeaderBytes = new ArrayList<>();
        for ( HttpHeader header : headers ) {
            byte[] headerBytes = String.format("%s:%s\r\n", header.getKey(), header.getValue()).getBytes();
            trailingHeaderBytes.addAll(Util.convertToList(headerBytes));
        }

        byte[] trailingHeaderBytesLength = Util.encodeLength((byte) 0x80, trailingHeaderBytes.size());

        List<Byte> output = new ArrayList<>();
        output.addAll(Util.convertToList(trailingHeaderBytesLength));
        output.addAll(trailingHeaderBytes);
        return Util.convertToArray(output);
    }
}
