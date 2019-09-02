/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jin.grpc

import com.google.protobuf.MessageLite
import com.jin.grpc.internal.RPCMethod
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toChannel
import okhttp3.Call
import okhttp3.RequestBody
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.net.ProtocolException
import kotlin.coroutines.Continuation
import com.jakewharton.rxrelay2.PublishRelay
import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.internal.*
import com.squareup.wire.internal.BlockingMessageSource
import com.squareup.wire.internal.genericParameterType
import com.squareup.wire.internal.invokeSuspending
import com.squareup.wire.internal.messageSink
import com.squareup.wire.internal.newDuplexRequestBody
import com.squareup.wire.internal.newRequestBody
import com.squareup.wire.internal.rawType
import com.squareup.wire.internal.readFromResponseBodyCallback
import com.squareup.wire.internal.unconfinedCoroutineScope
import com.squareup.wire.internal.writeToRequestBody
import io.reactivex.functions.Consumer
import kotlinx.coroutines.*
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException


internal sealed class GrpcMethod<S : MessageLite, R : MessageLite>(
    val path: String,
    val responseClass: Class<R>
) {
    /** Handle a dynamic proxy method call to this. */
    abstract fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any

    /**
     * Cancellation of the [Call] is always tied to the response. To cancel the call, either cancel
     * the response channel, deferred, or suspending function call.
     */
    private fun callWithChannels(grpcClient: GrpcClient): Pair<Channel<S>, Channel<R>> {
        val requestChannel = Channel<S>(1)
        val responseChannel = Channel<R>(1)

        val requestBody = newDuplexRequestBody()
        val call = grpcClient.newCall(path, requestBody)
        requestChannel.writeToRequestBody(requestBody)
        call.enqueue(responseChannel.readFromResponseBodyCallback(responseClass))

        responseChannel.invokeOnClose { cause ->
            call.cancel()
            requestChannel.cancel()
            responseChannel.cancel()
        }

        return requestChannel to responseChannel
    }


    private fun callWithRxStream(grpcClient: GrpcClient): Pair<PublishRelay<S>, PublishRelay<R>> {

        val requestBody = newDuplexRequestBody()

        val sendRelay = PublishRelay.create<S>()

        val receiveRelay = PublishRelay.create<R>();


        val messageSink = requestBody.messageSink<S>();

        //subscribe to send message
        sendRelay.subscribe(
            Consumer {
                messageSink.write(it)
            }
        )

        val call = grpcClient.newCall(path, requestBody)
        val callBack = object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                call.cancel()
            }

            override fun onResponse(call: Call, response: Response) {
                runBlocking {
                    response.use {
                        response.messageSource(responseClass).use { reader ->
                            while (true) {
                                val message = reader.read() ?: break
                                receiveRelay.accept(message)
                            }
                            call.cancel()
                        }
                    }
                }
            }
        }

        call.enqueue(callBack)

        return sendRelay to receiveRelay
    }


    private fun callBlocking(
        grpcClient: GrpcClient,
        requestBody: RequestBody,
        responseClass: Class<R>
    ): MessageSource<R> {
        val call = grpcClient.newCall(path, requestBody)
        val messageSource = BlockingMessageSource(call, responseClass)
        call.enqueue(messageSource.readFromResponseBodyCallback())
        return messageSource
    }

    /** Single request, single response. */
    class RequestResponse<S : MessageLite, R : MessageLite>(path: String, responseClass: Class<R>) :
        GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
            return (args.last() as Continuation<Any>).invokeSuspending {
                invoke(grpcClient, parameter = args[0])
            }
        }

        suspend fun invoke(
            grpcClient: GrpcClient,
            parameter: Any
        ): R {
            val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

            responseChannel.consume {
                requestChannel.send(parameter as S)
                requestChannel.close()
                return receive()
            }
        }
    }

    /** Single request, single response. */
    class BlockingRequestResponse<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
            System.out.println("mark 6")
            val requestBody = newRequestBody(args[0] as S)
            val messageSource = super.callBlocking(grpcClient, requestBody, responseClass)
            System.out.println("mark 7")
            messageSource.use {
                return messageSource.read() ?: throw ProtocolException("required message not sent")
            }
        }
    }

    /** Request is streaming, with one single response. */
    class StreamingRequest<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {

        override fun invoke(grpcClient: GrpcClient,args: Array<Any>): Pair<SendChannel<S>, Deferred<R>> {
            val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

            return Pair(
                requestChannel,
                // We use ATOMIC to guarantee the coroutine doesn't start after potential cancellations
                // happen.
                unconfinedCoroutineScope.async(start = CoroutineStart.ATOMIC) {
                    responseChannel.consume { receive() }
                }
            )
        }
    }


    /** rx streaming request. */
    class RxStreamingRequest<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Pair<PublishRelay<S>, PublishRelay<R>> {
            return super.callWithRxStream(grpcClient)
        }
    }


    /** Single request, and response is streaming. */
    class StreamingResponse<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
            val (requestChannel, responseChannel) = super.callWithChannels(grpcClient)

            // TODO(benoit) Remove the cancellation handling once this ships:
            //     https://github.com/Kotlin/kotlinx.coroutines/issues/845
            unconfinedCoroutineScope.coroutineContext[Job]!!.invokeOnCompletion {
                responseChannel.cancel()
            }
            return unconfinedCoroutineScope.produce<Any> {
                requestChannel.send(args[0] as S)
                requestChannel.close()
                responseChannel.toChannel(channel)
            }
        }
    }

    /** Single request, and response is streaming. */
    class BlockingStreamingResponse<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(grpcClient: GrpcClient, args: Array<Any>): Any {
            val requestBody = newRequestBody(args[0] as S)
            return super.callBlocking(grpcClient, requestBody, responseClass)
        }
    }

    /** Request and response are both streaming. */
    class FullDuplex<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(
            grpcClient: GrpcClient,
            args: Array<Any>
        ): Pair<SendChannel<S>, ReceiveChannel<R>> = super.callWithChannels(grpcClient)
    }

    /** Request and response are both streaming. */
    class BlockingFullDuplex<S : MessageLite, R : MessageLite>(
        path: String,
        responseClass: Class<R>
    ) : GrpcMethod<S, R>(path, responseClass) {
        override fun invoke(
            grpcClient: GrpcClient, args: Array<Any>
        ): Pair<MessageSink<S>, MessageSource<R>> {
            val requestBody = newDuplexRequestBody()
            val messageSink = requestBody.messageSink<S>()
            val messageSource = super.callBlocking(grpcClient, requestBody, responseClass)
            return messageSink to messageSource
        }
    }

    internal companion object {
        internal fun <S : MessageLite, R : MessageLite> Method.toGrpc(): GrpcMethod<S, R> {
            val rpcMethod = getAnnotation(RPCMethod::class.java)

            val responseClass: Class<R> = GrpcUtils.getClassByString(rpcMethod.responseClass) as Class<R>

            val parameterTypes = genericParameterTypes

            val returnType = genericReturnType

            if (parameterTypes.size == 2) {
                // Coroutines request-response.
                // Object methodName(RequestType, Continuation)
                if (parameterTypes[1].rawType() == Continuation::class.java) {
                    return RequestResponse(rpcMethod.path, responseClass)
                }

            } else if (parameterTypes.size == 1) {
                // Coroutines streaming response.
                // ReceiveChannel<ResponseType> methodName(RequestType)
                if (returnType.rawType() == ReceiveChannel::class.java) {
                    return StreamingResponse(rpcMethod.path, responseClass)
                }

                // Blocking streaming response.
                // MessageSource<ResponseType> methodName(RequestType)
                if (returnType.rawType() == MessageSource::class.java) {
                    return BlockingStreamingResponse(rpcMethod.path, responseClass)
                }
                // Blocking request-response.
                // ResponseType methodName(RequestType)
                return BlockingRequestResponse(rpcMethod.path, responseClass)
            } else if (parameterTypes.isEmpty()) {
                if (returnType.rawType() == Pair::class.java) {
                    val pairType = returnType as ParameterizedType
                    val requestType = pairType.genericParameterType(index = 0).rawType()
                    val responseType = pairType.genericParameterType(index = 1).rawType()

                    // Coroutines full duplex.
                    // Pair<SendChannel<RequestType>, ReceiveChannel<ResponseType>> methodName()
                    if (requestType == SendChannel::class.java && responseType == ReceiveChannel::class.java) {
                        return FullDuplex(rpcMethod.path, responseClass)
                    }

                    // Coroutines streaming request.
                    // Pair<SendChannel<RequestType>, Deferred<ResponseType>> methodName()
                    if (requestType == SendChannel::class.java && responseType == Deferred::class.java) {
                        return StreamingRequest(rpcMethod.path, responseClass)
                    }
                    if (requestType == PublishRelay::class.java && responseType == PublishRelay::class.java) {
                        return RxStreamingRequest(rpcMethod.path, responseClass)
                    }

                    // Blocking full duplex OR streaming request. (single response could be Future instead?)
                    // Pair<MessageSink<RequestType>, MessageSource<ResponseType>> methodName()
                    if (requestType == MessageSink::class.java
                        && responseType == MessageSource::class.java
                    ) {
                        return BlockingFullDuplex(rpcMethod.path, responseClass)
                    }
                }
            }

            error("unexpected gRPC method: $this")
        }
    }
}
