package com.github.ljufa.sma.tw.server

import com.github.ljufa.sma.tw.server.db.DataSource
import com.github.ljufa.sma.tw.server.db.DbRepository
import com.github.ljufa.sma.tw.server.db.MessageHandler
import com.github.ljufa.sma.tw.server.service.GrpcServer
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

private val log = LoggerFactory.getLogger("main")

fun main() {
    log.debug("Staring application with configuration: {}", config)
    val messageHandler = MessageHandler(createKafkaConsumer())
    thread(name = "writer-thread") {
        if (DataSource.getDbPurged()) {
            messageHandler.seekOffsetToBeginning()
        }
        messageHandler.listenBatches { consumerRecords ->
            DbRepository.persistBatch(consumerRecords)
        }
    }
    GrpcServer().start()
    messageHandler.stop()
}
