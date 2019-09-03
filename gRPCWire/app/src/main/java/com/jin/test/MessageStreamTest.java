package com.jin.test;


import com.jin.codegenmodel.HelloServiceClient;
import com.jin.grpc.GrpcClient;
import com.realtime.grpc.protocode.*;
import com.squareup.wire.MessageSink;
import com.squareup.wire.MessageSource;
import kotlin.Pair;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.channels.ReceiveChannel;
import kotlinx.coroutines.channels.SendChannel;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageStreamTest {
    public static void main(String[] args) throws IOException {

        System.out.println("test start");
//        HttpUrl url = new HttpUrl.Builder().scheme("http").host("100.64.228.163").port(8899).build();//office
        HttpUrl url = new HttpUrl.Builder().scheme("http").host("10.0.0.49").port(8899).build();//home
        List<Protocol> protocolList = new ArrayList<>();
        protocolList.add(Protocol.H2_PRIOR_KNOWLEDGE);

        GrpcClient grpcClient = new GrpcClient.Builder().client(
                new OkHttpClient.Builder().protocols(protocolList).build()
        ).baseUrl(url).build();

        HelloServiceClient client = grpcClient.create(JvmClassMappingKt.getKotlinClass(HelloServiceClient.class));

        //streaming blocking usage;
        Pair<MessageSink<StreamRequest>, MessageSource<StreamResponse>> pair = client.streamingTalk();
        MessageSink<StreamRequest> requestMessageSink = pair.getFirst();
        MessageSource<StreamResponse> responseMessageSource = pair.getSecond();

        requestMessageSink.write(StreamRequest.newBuilder().setRequestInfo("helloworld ").setRequestTime(System.currentTimeMillis()).setUUID("uuid-123-123-12345").build());

        StreamResponse response = responseMessageSource.read();
        System.out.println("response data = " + response);
    }
}
