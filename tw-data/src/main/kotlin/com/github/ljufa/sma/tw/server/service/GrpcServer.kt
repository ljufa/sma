package com.github.ljufa.sma.tw.server.service

import com.github.ljufa.sma.tw.server.config
import io.grpc.Server
import io.grpc.ServerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GrpcServer {
    val log: Logger = LoggerFactory.getLogger(GrpcServer::class.java)

    val server: Server = ServerBuilder
        .forPort(config.server.port)
        .addService(TopTweetsService())
        .addService(TwitterApiService())
        .build()

    fun start() {
        server.start()
        log.info("Server started, listening on 1977")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("*** shutting down gRPC server since JVM is shutting down")
                stop()
                log.info("*** server shut down")
            }
        )
        server.awaitTermination()
    }

    private fun stop() {
        server.shutdown()
    }

}