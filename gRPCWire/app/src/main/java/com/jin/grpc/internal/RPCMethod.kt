package com.jin.grpc.internal


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCMethod(
    val path: String,
    val responseClass: String
)
