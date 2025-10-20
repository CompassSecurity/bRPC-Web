package com.muukong.grpcweb;

import com.muukong.protobuf.PBDisassembler;
import com.muukong.protobuf.PBMessage;
import com.muukong.util.Util;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Represents the body of a gRPC HTTP request / response body. The following content types are supported:
 * - application/grpc-web and application/grpc-web+proto (these are treated equally)
 * - application/grpc-web-text
 */
public class GrpcWebRequestResponse {

    private final GrpcWebContentTypes gRpcWebType;
    private final byte[] grpcMessage;
    private final List<PBMessage> pbMessages = new ArrayList<>(); // TODO: rename
    private final TrailingHeaders trailingHeaders = new TrailingHeaders();

    private GrpcWebRequestResponse(byte[] grpcMessage, GrpcWebContentTypes gRpcWebType) {
        this.gRpcWebType = gRpcWebType;
        this.grpcMessage = grpcMessage;
    }

    /**
     * Initialize using a request / response body of type application/grpc-web (or application/grpc-web+proto)
     *
     * @param requestResponseBody   Byte string containing request / response body
     * @return  A GrpcWebRequestResponse object initialized with `requestResponseBody`
     */
    public static GrpcWebRequestResponse fromGrpcWebProto(byte[] requestResponseBody) {

        GrpcWebRequestResponse grpcWebRequestResponse = new GrpcWebRequestResponse(requestResponseBody, GrpcWebContentTypes.GRPC_WEB_PROTO);
        grpcWebRequestResponse.deserializeGrpcWebProto();
        return grpcWebRequestResponse;

    }

    /**
     * Initialize using a request / response body of type application/grpc-web+text
     *
     * @param requestResponseBody   Byte string containing request / response body
     * @return  A GrpcWebRequestResponse object initialized with `requestResponseBody`
     */
    public static GrpcWebRequestResponse fromGrpcWebText(byte[] requestResponseBody) {

       /*
        The following method supports the following two scenarios:
        1. A single base64 encoded message that contains all protobuf messages and trailing headers. In this case,
           we simply base64-decode the payload and then use the proto deserialization.
        2. A streaming scenario where multiple base64-encoded blocks are chained together:
           <Base64-Block-1><Base64-Block-2>...<Base64-Block-n>
           We assume that each block contains a single protobuf message (this can include trailing headers)
       */

       GrpcWebRequestResponse grpcWebRequestResponse;
       byte[] grpcWebProto;

       try { // Attempt to decode and then disassemble as single base64-encoded blob (i.e. case 1.)
           grpcWebProto = Base64.getDecoder().decode(requestResponseBody);
           grpcWebRequestResponse = new GrpcWebRequestResponse(grpcWebProto, GrpcWebContentTypes.GRPC_WEB_TEXT);
           grpcWebRequestResponse.deserializeGrpcWebProto(); // Check if the protobuf message can be deserialized.
           return grpcWebRequestResponse;
       } catch ( Exception ex) {
           // Attempt to decode as multiple streamed messages (i.e. case 2.)
           grpcWebRequestResponse = new GrpcWebRequestResponse(requestResponseBody, GrpcWebContentTypes.GRPC_WEB_TEXT_STREAM);
           if ( grpcWebRequestResponse.tryDecodeBase64Stream(requestResponseBody) ) {
               return grpcWebRequestResponse; // success
           } else {
               throw new RuntimeException("Failed to disassemble message"); // fail
           }
       }
    }

    /**
     * This methods decodes a stream of base64-encoded protobuf messages:
     * <Base64-encoded protobuf message 1><Base64-encoded protobuf message 2>...
     *
     * @param grpcWebTextBody Byte string containing request / response body
     * @return  Returns true on success and false otherwise
     */
    private boolean tryDecodeBase64Stream(byte[] grpcWebTextBody) {

        /*
         I currently do not know how to detect the boundaries of the base64-encoded protobuf blocks. I therefore
         use a heuristic where I try to base64-decode the largest possible chunks ("from the right");

         Start with interval [0,length) and find largest index i s.t. [0, i) can be base64-decoded without an
         error. Then continue with interval [i, length) until the interval is empty.

         TODO: check if this can be improved
        */

        /*
          TODO: The gRPC-Web specification says that a client may not assume that base64 encoded chunks
          are complete messages:

          "While the server runtime will always base64-encode and flush gRPC messages atomically the client
           library should not assume base64 padding always happens at the boundary of message frames. That is,
           the implementation may send base64-encoded "chunks" with potential padding whenever the runtime
           needs to flush a byte buffer."
           (See https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md)
         */

        int left = 0;
        int right = grpcWebTextBody.length;

        while ( right > left ) {

            byte[] chunk = new byte[right - left];
            System.arraycopy(grpcWebTextBody, left, chunk, 0, right - left);

            try { // try base64-decode current chunk
                byte[] grpcWebProto = Base64.getDecoder().decode(chunk);

                if ( grpcWebProto[0] == (byte)0x00 ) { // message

                    GrpcWebRequestResponse tmp = GrpcWebRequestResponse.fromGrpcWebProto(grpcWebProto);

                    List<PBMessage> messages = tmp.getMessages();
                    pbMessages.addAll(messages);

                    left = right;
                    right = grpcWebTextBody.length;
                } else { // trailing header

                    String headers = new String(grpcWebProto);
                    headers = headers.substring(5, headers.length());


                    for ( String header : headers.split("\r\n") ) {
                        String[] split = header.split(":");
                        String key = split[0];
                        String value = split.length == 2 ? split[1] : "0"; // Handle case where header value is empty
                        trailingHeaders.addHeader(new HttpHeader(key, value));
                    }

                    left = right; // Trailing header is always last. TODO: check if this always holds
                }

            } catch ( Exception ex ) {
                --right;
                if ( left == right )
                    return false;
            }
        }

        return true;

    }

    public static GrpcWebRequestResponse fromProtoscope(List<PBMessage> message, List<HttpHeader> trailingHeaders) {

        // TODO
        throw new RuntimeException("Not implemented");

    }

    public PBMessage getMessage(int i) {
        return pbMessages.get(i);
    }

    public List<PBMessage> getMessages() {
        return pbMessages;
    }

    public void setMessage(PBMessage message, int i) {
        pbMessages.set(i, message);
    }

    public void removeMessage(int i) {
        pbMessages.remove(i);
    }

    public void setPbMessages(List<PBMessage> messages) {
        pbMessages.clear();
        pbMessages.addAll(messages);
    }

    public void setTrailingHeaders(List<HttpHeader> headers) {
        trailingHeaders.clearHeaders();
        trailingHeaders.addHeaders(headers);
    }

    private void deserializeGrpcWebProto() {

        int cursor = 0;
        while (cursor < grpcMessage.length) {

            byte b = grpcMessage[cursor];
            final int length = (((int) grpcMessage[cursor + 4]) & 0xff) |
                    ((((int) grpcMessage[cursor + 3]) & 0xffff) << 8) |
                    ((((int) grpcMessage[cursor + 2]) & 0xffffff) << 16) |
                    ((((int) grpcMessage[cursor + 1]) & 0xffffffff) << 24);
            cursor += 5;

            if (b == 0) { // message

                if ( length == 0 ) { // Add empty message
                    pbMessages.add(new PBMessage());
                    continue;
                }

                byte[] grpcMessage = new byte[length];
                System.arraycopy(this.grpcMessage, cursor, grpcMessage, 0, length);

                try {
                    PBDisassembler disassembler = new PBDisassembler(grpcMessage);
                    PBMessage message = disassembler.disassemble();
                    pbMessages.add(message);
                } catch (Exception ex) { // Log exception details and display error message
                    System.err.println(ex.toString());
                    throw ex;
                }

                cursor += length;

            } else { // trailing header

                byte[] trailer = new byte[length];
                System.arraycopy(grpcMessage, cursor, trailer, 0, length);

                String trailerString = new String(trailer);
                for (String header : trailerString.split("\r\n")) {

                    String[] tmp = header.split(":");
                    trailingHeaders.addHeader(new HttpHeader(tmp[0], tmp[1]));

                    cursor += length;
                }
            }
        }
    }

    public byte[] serialize() {

        switch (gRpcWebType) {
            case GRPC_WEB_PROTO:
                return serializeProto();
            case GRPC_WEB_TEXT:
                return Base64.getEncoder().encode(serializeProto());
            case GRPC_WEB_TEXT_STREAM:
                return serializeTextStream();
            default:
                throw new RuntimeException("Invalid gRPC-Web type");
        }
    }

    private byte[] serializeTextStream() {

        List<Byte> output = new ArrayList<>();

        for ( PBMessage message : pbMessages ) {
            byte[] tmp = Base64.getEncoder().encode(message.serializeValue());
            output.addAll(Util.convertToList(tmp));
        }

        byte[] tmp = trailingHeaders.serializeValue();
        byte[] tmpBase64 = Base64.getEncoder().encode(tmp);
        output.addAll(Util.convertToList(tmpBase64));

        return Util.convertToArray(output);
    }

    private byte[] serializeProto() {

        List<Byte> output = new ArrayList<>();

        for ( PBMessage message : pbMessages ) {

            byte[] messageBytes = message.serializeValue();
            byte[] messageLengthBytes = Util.encodeLength((byte)0, messageBytes.length);

            output.addAll(Util.convertToList(messageLengthBytes));
            output.addAll(Util.convertToList(messageBytes));
        }

        byte[] trailingHeaderBytes = trailingHeaders.serializeValue();
        output.addAll(Util.convertToList(trailingHeaderBytes));

        return Util.convertToArray(output);

    }

    public String prettyPrint() {

        StringBuilder sb = new StringBuilder();

        sb.append("### Protobuf Messages ###\n\n");

        if ( pbMessages.size() == 0 ) {
            sb.append("# Empty - no protobuf messages to display\n\n");
        } else {

            for ( int i = 0; i < pbMessages.size(); ++i ) {

                String pretty = pbMessages.get(i).prettyPrint();
                sb.append(pretty);

                if ( i + 1 < pbMessages.size() )
                    sb.append("\n;\n");

            }
        }

        sb.append("\n\n### Trailing Headers ###\n\n");
        sb.append(trailingHeaders.prettyPrint());

        return sb.toString();
    }
}