package com.realtime.grpc.protocode

import com.jin.grpcwire.squareup.wire.internal.RPCMethod
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.Service
import com.squareup.wire.WireRpc
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface HelloServiceClient : Service {
//    @WireRpc(
//            path = "/com.jin.proto.HelloSerivce/helloGRPCCall",
//            requestAdapter = "com.realtime.grpc.protocode.HelloRequest#ADAPTER",
//            responseAdapter = "com.realtime.grpc.protocode.HelloResponse#ADAPTER"
//    )
//    fun helloGRPCCall(request: HelloRequest): HelloResponse
//
//
//    @WireRpc(
//        path = "/routeguide.RouteGuide/ListFeatures",
//        requestAdapter = "routeguide.Rectangle#ADAPTER",
//        responseAdapter = "routeguide.Feature#ADAPTER"
//    )
//    fun ListFeatures(request: Rectangle): MessageSource<Feature>
//
//    @WireRpc(
//        path = "/routeguide.RouteGuide/RecordRoute",
//        requestAdapter = "routeguide.Point#ADAPTER",
//        responseAdapter = "routeguide.RouteSummary#ADAPTER"
//    )
//    fun RecordRoute(): Pair<MessageSink<Point>, MessageSource<RouteSummary>>
//
//    @WireRpc(
//        path = "/routeguide.RouteGuide/RouteChat",
//        requestAdapter = "routeguide.RouteNote#ADAPTER",
//        responseAdapter = "routeguide.RouteNote#ADAPTER"
//    )
//    fun RouteChat(): Pair<MessageSink<RouteNote>, MessageSource<RouteNote>>


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
}