package com.muukong.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class BurpIntegration implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {

        api.extension().setName("bRPC-Web");

        api.userInterface().registerHttpRequestEditorProvider(new BurpHttpRequestEditorProvider(api));
        api.userInterface().registerHttpResponseEditorProvider(new BurpHttpResponseEditorProvider(api));

        api.logging().logToOutput("Extension loaded");
    }

}
