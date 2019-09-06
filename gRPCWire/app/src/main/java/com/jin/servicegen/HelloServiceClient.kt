package com.jin.servicegen

import com.jakewharton.rxrelay2.PublishRelay
import com.jin.grpc.internal.RPCMethod
import com.realtime.grpc.protocode.HelloRequest
import com.realtime.grpc.protocode.HelloResponse
import com.realtime.grpc.protocode.StreamRequest
import com.realtime.grpc.protocode.StreamResponse
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.Service
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface HelloServiceClient : Service {

    @RPCMethod(
        path = "/com.jin.proto.HelloSerivce/helloGRPCCall",
        responseClass = "com.realtime.grpc.protocode.HelloResponse"
    )
    fun helloGRPCCall(request: HelloRequest): HelloResponse

    @RPCMethod(
        path = "/com.jin.proto.StudentSerivce/StreamToStreamTalk",
        responseClass = "com.realtime.grpc.protocode.StreamResponse"
    )
    fun streamingTalk(): Pair<MessageSink<StreamRequest>, MessageSource<StreamResponse>>

    @RPCMethod(
        path = "/com.jin.proto.StudentSerivce/StreamToStreamTalk",
        responseClass = "com.realtime.grpc.protocode.StreamResponse"
    )
    fun streamingTalkChannel(): Pair<SendChannel<StreamRequest>, ReceiveChannel<StreamResponse>>

    @RPCMethod(
        path = "/com.jin.proto.StudentSerivce/StreamToStreamTalk",
        responseClass = "com.realtime.grpc.protocode.StreamResponse"
    )
    fun streamingTalkRx(): Pair<PublishRelay<StreamRequest>, PublishRelay<StreamResponse>>

}