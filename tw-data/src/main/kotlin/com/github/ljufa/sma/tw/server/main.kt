package com.github.ljufa.sma.tw.server

import com.github.ljufa.sma.tw.server.api.TopTweetsService
import com.github.ljufa.sma.tw.server.db.DataSource
import com.github.ljufa.sma.tw.server.db.DbRepository
import com.github.ljufa.sma.tw.server.incoming.MessageHandler
import io.grpc.ServerBuilder
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

private val log = LoggerFactory.getLogger("com.github.ljufa.sma.tw.server.MainKt")

fun main() {
    val config = createConfig()
    log.debug("Staring application with configuration: {}", config)
    val kafkaConsumer = createKafkaConsumer(config)
    val messageHandler = MessageHandler(kafkaConsumer)
    val dataSource = DataSource(config)
    val dbRepository = DbRepository(dataSource)

    thread(name = "writer-thread") {
        if (dataSource.getDbPurged()) {
            messageHandler.seekOffsetToBeginning()
        }
        messageHandler.listenBatches { consumerRecords ->
            dbRepository.persistBatch(consumerRecords)
        }
    }

    val grpcServer = ServerBuilder
        .forPort(config.server.port)
        .addService(TopTweetsService(dbRepository))
        .build()

    grpcServer.start()

    addShutdownHook {
        log.info("*** shutting down gRPC server since JVM is shutting down")
        grpcServer.shutdown()
        grpcServer.awaitTermination()
        log.info("*** server shut down")
    }

    addShutdownHook {
        log.info("*** shutting kafka message handler since JVM is shutting down")
        messageHandler.stop()
        log.info("*** message handler shut down")
    }

    grpcServer.awaitTermination()
}

fun addShutdownHook(hook: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            hook()
        }
    )
}
