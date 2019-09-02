package com.jin.grpcwire.squareup.wire.internal


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCMethod(
    val path: String,
    val responseClass: String
)
