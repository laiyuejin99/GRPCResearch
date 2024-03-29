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

import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.squareup.wire.MessageSource
import okio.Buffer
import okio.BufferedSource
import java.io.Closeable
import java.net.ProtocolException

/**
 * Reads an HTTP/2 stream as a sequence of gRPC messages.
 *
 * @param source the HTTP/2 stream body.
 * @param messageAdapter a proto adapter for each message.
 * @param grpcEncoding the "grpc-encoding" header, or null if it is absent.
 */
internal class GrpcMessageSource<T : MessageLite>(
    private val source: BufferedSource,
    private val responseClass: Class<T>,
    private val grpcEncoding: String? = null
) : MessageSource<T>, Closeable by source {
    override fun read(): T? {
        if (source.exhausted()) return null

        // Length-Prefixed-Message → Compressed-Flag Message-Length Message
        //         Compressed-Flag → 0 / 1 # encoded as 1 byte unsigned integer
        //          Message-Length → {length of Message} # encoded as 4 byte unsigned integer
        //                 Message → *{binary octet}

        val compressedFlag = source.readByte()
        val messageDecoding: GrpcDecoder = when {
            compressedFlag.toInt() == 0 -> GrpcDecoder.IdentityGrpcDecoder
            compressedFlag.toInt() == 1 -> {
                grpcEncoding?.toGrpcDecoding() ?: throw ProtocolException(
                    "message is encoded but message-encoding header was omitted"
                )
            }
            else -> throw ProtocolException("unexpected compressed-flag: $compressedFlag")
        }

        val encodedLength = source.readInt().toLong() and 0xffffffffL

        val encodedMessage = Buffer()
        encodedMessage.write(source, encodedLength)
        val ByteString = ByteString.readFrom(encodedMessage.inputStream())

        return  GrpcUtils.parseFromByteString(ByteString,responseClass)

//        return messageAdapter.decode(messageDecoding.decode(encodedMessage).buffer())
    }
}
