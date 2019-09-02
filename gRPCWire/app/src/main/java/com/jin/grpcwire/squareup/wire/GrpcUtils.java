package com.jin.grpcwire.squareup.wire;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.MessageLite;
import com.squareup.wire.ProtoAdapter;

import java.lang.reflect.Method;

public class GrpcUtils {
    public static <MessageBody extends MessageLite> MessageBody parseFromByteString(ByteString bytesData,
                                                                                    Class<MessageBody> classType) {
        try {
            Method parseMethod = classType.getMethod("parseFrom", ByteString.class);
            MessageBody result = (MessageBody) parseMethod.invoke(null, bytesData);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;//or return a empty object
    }

    public static <MessageBody extends GeneratedMessageLite> MessageBody encodeMessage(ByteString bytesData,
                                                                                       Class<MessageBody> classType) {
        try {
            Method parseMethod = classType.getMethod("parseFrom", ByteString.class);
            MessageBody result = (MessageBody) parseMethod.invoke(null, bytesData);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;//or return a empty object
    }


    public static <MessageBody extends GeneratedMessageLite> MessageBody getDefaultInstance(ByteString bytesData,
                                                                                            Class<MessageBody> classType) {
        try {
            Method parseMethod = classType.getMethod("parseFrom", ByteString.class);
            MessageBody result = (MessageBody) parseMethod.invoke(null, bytesData);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;//or return a empty object
    }


    public static Class<?> getClassByString(String adapterString) {
        try {
            return Class.forName(adapterString);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to access " + adapterString, e);
        }
    }
}
