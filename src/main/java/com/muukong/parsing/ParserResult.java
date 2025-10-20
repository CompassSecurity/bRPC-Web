package com.muukong.parsing;

import com.muukong.grpcweb.HttpHeader;
import com.muukong.grpcweb.TrailingHeaders;
import com.muukong.protobuf.PBMessage;

import java.util.List;

public class ParserResult implements IParseable {

    List<PBMessage> messages;
    TrailingHeaders trailingHeaders;

    public ParserResult(List<PBMessage> messages, TrailingHeaders trailingHeaders) {
        this.messages = messages;
        this.trailingHeaders = trailingHeaders;
    }

    public List<PBMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<PBMessage> messages) {
        this.messages = messages;
    }

    public TrailingHeaders getTrailingHeaders() {
        return trailingHeaders;
    }

    public void setTrailingHeaders(TrailingHeaders trailingHeaders) {
        this.trailingHeaders = trailingHeaders;
    }

    public List<HttpHeader> getTrailingHeadersList() {
        return trailingHeaders.getHeaders();
    }
}
