package com.muukong.burp;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.List;

public class BurpUtils {

    private static String getContentType(List<HttpHeader> headers) {

        for ( HttpHeader h : headers ) {
            if ( h.name().equalsIgnoreCase("content-type")) {
                return h.value();
            }
        }

        return null;
    }

    public static boolean isGrpcWebProto(HttpRequest request) {

        if ( isGrpcWebText(request) )
            return false;

        String contentType = getContentType(request.headers());
        if ( contentType == null )
            return false;

        return contentType.contains("application/grpc-web");

    }

    public static boolean isGrpcWebProto(HttpResponse response) {

        if ( isGrpcWebText(response) )
            return false;

        String contentType = getContentType(response.headers());
        if ( contentType == null )
            return false;

        return contentType.contains("application/grpc-web");

    }

    public static boolean isGrpcWebText(HttpRequest request ) {

        String contentType = getContentType(request.headers());
        if ( contentType == null )
            return false;

        return contentType.contains("application/grpc-web-text");
    }

    public static boolean isGrpcWebText(HttpResponse response ) {

        String contentType = getContentType(response.headers());
        if ( contentType == null )
            return false;

        return contentType.contains("application/grpc-web-text");
    }
}
