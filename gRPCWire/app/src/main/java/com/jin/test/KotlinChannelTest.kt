package com.jin.test

import com.jin.codegenmodel.HelloServiceClient
import com.realtime.grpc.protocode.StreamRequest
import com.jin.grpc.GrpcClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.*
import java.util.concurrent.TimeUnit


fun main() {

    println("test start")
//    val url = HttpUrl.Builder().scheme("http").host("10.0.0.49").port(8899).build()//home ip
    val url = HttpUrl.Builder().scheme("http").host("100.64.228.163").port(8899).build()//office ip
    val protocolList = ArrayList<Protocol>()
    protocolList.add(Protocol.H2_PRIOR_KNOWLEDGE)

    val grpcClient = GrpcClient.Builder().client(
        OkHttpClient.Builder().protocols(protocolList).readTimeout(40,TimeUnit.SECONDS).build()
    ).baseUrl(url).build()

    val client = grpcClient.create(HelloServiceClient::class.java.kotlin)

    val (sendChannel, receiveChannel) = client.streamingTalkChannel()

    GlobalScope.async {
        val request =
            StreamRequest.newBuilder().setRequestInfo("helloworld aaaaa").setRequestTime(System.currentTimeMillis())
                .setUUID("uuid-123-123-12345").build()
        sendChannel.send(request)
        System.out.println("message send")

        sendChannel.send(request)
        System.out.println("message send")

        sendChannel.send(request)
        System.out.println("message send")
    }

    runBlocking {
        var count = 0;
        while (true) {
            val response = receiveChannel.receive()
            System.out.println("message receive" + response)
            System.out.println("count" + count)
            System.out.println("finsih time = " + Date())//10s will timeout

        }
        sendChannel.close()
    }

}