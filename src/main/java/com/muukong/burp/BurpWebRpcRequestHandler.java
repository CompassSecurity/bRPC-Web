package com.muukong.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import com.muukong.parsing.ParserResult;
import com.muukong.parsing.ProtoParser;
import com.muukong.grpcweb.GrpcWebRequestResponse;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.awt.Component;

public class BurpWebRpcRequestHandler implements ExtensionProvidedHttpRequestEditor {

    private final RawEditor requestEditor;
    private final MontoyaApi api;
    private GrpcWebRequestResponse grpcWebRequestResponse;
    private HttpRequestResponse requestResponse;
    private Logging logging;

    BurpWebRpcRequestHandler(MontoyaApi api, EditorCreationContext creationContext) {

        this.api = api;
        this.logging = api.logging();

        if ( creationContext.editorMode() == EditorMode.READ_ONLY ) {
            requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
        } else {
            requestEditor = api.userInterface().createRawEditor();
        }

    }

    @Override
    public HttpRequest getRequest() {

        final HttpRequest request = requestResponse.request();

        if ( !isModified() )
            return request;

        final String data = new String(requestEditor.getContents().getBytes(), java.nio.charset.StandardCharsets.UTF_8);

        try {
            ProtoParser pp = new ProtoParser(data);
            ParserResult res = pp.parse();

            grpcWebRequestResponse.setPbMessages(res.getMessages());
            grpcWebRequestResponse.setTrailingHeaders(res.getTrailingHeadersList());

            return request.withBody(ByteArray.byteArray(grpcWebRequestResponse.serialize()));

        } catch ( ParseCancellationException ex ) {
            final String errorMessage = String.format("Syntax error in gRPC-Web request: %s", ex.getMessage());
            logging.logToOutput(errorMessage);
            return request;
        } catch ( Exception ex2 ) {
            final String errorMessage = String.format("Unknown error occurred: %s", ex2.getMessage());
            logging.logToOutput(errorMessage);
            return request;
        }

    }


    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {

        this.requestResponse = httpRequestResponse;

        final ByteArray body = httpRequestResponse.request().body();

        if ( BurpUtils.isGrpcWebText( httpRequestResponse.request()) ) {
            grpcWebRequestResponse = GrpcWebRequestResponse.fromGrpcWebText(body.getBytes());
        } else {
            grpcWebRequestResponse = GrpcWebRequestResponse.fromGrpcWebProto(body.getBytes());
        }

        requestEditor.setContents(ByteArray.byteArray(grpcWebRequestResponse.prettyPrint().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse httpRequestResponse) {

        return BurpUtils.isGrpcWebProto(httpRequestResponse.request()) ||
               BurpUtils.isGrpcWebText(httpRequestResponse.request());
    }

    @Override
    public String caption() {
        return "gRPC-Web";
    }

    @Override
    public Component uiComponent() {
        return requestEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return requestEditor.isModified();
    }
}
