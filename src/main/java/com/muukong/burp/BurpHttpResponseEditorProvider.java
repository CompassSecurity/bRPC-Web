package com.muukong.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.*;

public class BurpHttpResponseEditorProvider implements HttpResponseEditorProvider {

    private final MontoyaApi api;

    BurpHttpResponseEditorProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext editorCreationContext) {

        return new BurpWebRpcResponseHandler(api, editorCreationContext);

    }
}
