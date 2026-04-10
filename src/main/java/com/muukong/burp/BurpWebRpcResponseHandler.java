package com.muukong.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import com.muukong.parsing.ParserResult;
import com.muukong.parsing.ProtoParser;
import com.muukong.grpcweb.GrpcWebRequestResponse;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.awt.*;
import java.util.List;

public class BurpWebRpcResponseHandler implements ExtensionProvidedHttpResponseEditor {

    private final RawEditor responseEditor;
    private final MontoyaApi api;
    private GrpcWebRequestResponse grpcWebRequestResponse;
    private HttpRequestResponse requestResponse;
    private Logging logging;

    BurpWebRpcResponseHandler(MontoyaApi api, EditorCreationContext creationContext) {

        this.api = api;
        this.logging = api.logging();

        if ( creationContext.editorMode() == EditorMode.READ_ONLY ) {
            responseEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
        } else {
            responseEditor = api.userInterface().createRawEditor();
        }

    }

    @Override
    public HttpResponse getResponse() {

        final HttpResponse response = requestResponse.response();

        if ( !isModified() )
            return response;

        final String data = new String(responseEditor.getContents().getBytes(), java.nio.charset.StandardCharsets.UTF_8);

        try {
            ProtoParser pp = new ProtoParser(data);
            ParserResult res = pp.parse();

            grpcWebRequestResponse.setPbMessages(res.getMessages());
            grpcWebRequestResponse.setTrailingHeaders(res.getTrailingHeadersList());

            return response.withBody(ByteArray.byteArray(grpcWebRequestResponse.serialize()));
        } catch ( ParseCancellationException ex ) {
            final String errorMessage = String.format("Syntax error in gRPC-Web request: %s", ex.getMessage());
            logging.logToOutput(errorMessage);
            return response;
        } catch ( Exception ex2 ) {
            final String errorMessage = String.format("Unknown error occurred: %s", ex2.getMessage());
            logging.logToError(errorMessage);
            logging.logToError(errorMessage.toString());
            return response;
        }

    }

    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {

        this.requestResponse = httpRequestResponse;

        HttpResponse response = httpRequestResponse.response();
        if ( BurpUtils.isGrpcWebText(response) ) {
            byte[] body = response.body().getBytes();
            grpcWebRequestResponse = GrpcWebRequestResponse.fromGrpcWebText(body);
        } else {
            grpcWebRequestResponse = GrpcWebRequestResponse.fromGrpcWebProto(response.body().getBytes());
        }

        responseEditor.setContents(ByteArray.byteArray(grpcWebRequestResponse.prettyPrint().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse httpRequestResponse) {

        final HttpResponse response = httpRequestResponse.response();

        List<HttpHeader> headers = response.headers();
        for ( HttpHeader h : headers ) {

            if ( h.name().equalsIgnoreCase("content-type")) {
                String val = h.value().toLowerCase();
                return val.contains("application/grpc-web");
            }
        }

        return false;
    }

    @Override
    public String caption() {
        return "gRPC-Web";
    }

    @Override
    public Component uiComponent() {
        return responseEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return responseEditor.isModified();
    }

}
