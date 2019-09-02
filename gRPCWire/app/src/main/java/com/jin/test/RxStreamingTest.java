package com.jin.test;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jin.codegenmodel.HelloServiceClient;
import com.jin.grpc.GrpcClient;
import com.realtime.grpc.protocode.HelloRequest;
import com.realtime.grpc.protocode.HelloResponse;
import com.realtime.grpc.protocode.StreamRequest;
import com.realtime.grpc.protocode.StreamResponse;
import io.reactivex.functions.Consumer;
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

public class RxStreamingTest {

    public static void main(String[] args) throws IOException {

        System.out.println("test start");
//        HttpUrl url = new HttpUrl.Builder().scheme("http").host("100.64.228.163").port(8899).build();//office
        HttpUrl url = new HttpUrl.Builder().scheme("http").host("10.0.0.49").port(8891).build();//home
        List<Protocol> protocolList = new ArrayList<>();
        protocolList.add(Protocol.H2_PRIOR_KNOWLEDGE);

        GrpcClient grpcClient = new GrpcClient.Builder().client(
                new OkHttpClient.Builder().protocols(protocolList).build()
        ).baseUrl(url).build();

        HelloServiceClient client = grpcClient.create(JvmClassMappingKt.getKotlinClass(HelloServiceClient.class));


        Pair<PublishRelay<StreamRequest>, PublishRelay<StreamResponse>> pair = client.streamingTalkRx();

        PublishRelay<StreamRequest> sendRx = pair.getFirst();
        PublishRelay<StreamResponse> receiveRx = pair.getSecond();

        StreamRequest request = StreamRequest.newBuilder()
                .setRequestInfo("helloworld aaaaa")
                .setRequestTime(System.currentTimeMillis())
                .setUUID("uuid-123-123-12345")
                .build();

        sendRx.accept(request);
        sendRx.accept(request);
        sendRx.accept(request);
        sendRx.accept(request);

        receiveRx.hide().subscribe(new Consumer<StreamResponse>() {
            @Override
            public void accept(StreamResponse streamResponse) throws Exception {
                System.out.println("get message from server = " + streamResponse);
            }
        });
    }
}
