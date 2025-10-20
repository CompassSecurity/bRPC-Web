package com.muukong.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.*;

public class BurpHttpRequestEditorProvider implements HttpRequestEditorProvider {

    private final MontoyaApi api;

    BurpHttpRequestEditorProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext editorCreationContext) {

        return new BurpWebRpcRequestHandler(api, editorCreationContext);

    }

}
