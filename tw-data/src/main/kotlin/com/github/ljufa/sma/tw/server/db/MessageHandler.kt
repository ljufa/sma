package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.config
import com.github.ljufa.sma.tw.server.getCurrentLag
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.system.measureTimeMillis


class MessageHandler(val consumer: KafkaConsumer<String, String>) {
    init {
        consumer.subscribe(mutableListOf(config.kafka.topicName))
    }

    val log: Logger = LoggerFactory.getLogger(MessageHandler::class.java)

    private var listening = true

    fun listenBatches(
        consumeMessages: (ConsumerRecords<String, String>) -> Unit
    ) {
        while (listening) {
            val lag = consumer.getCurrentLag()
            if (lag < config.kafka.minBatchPersisSize) {
                log.debug("Lag: $lag < ${config.kafka.minBatchPersisSize} going to sleep")
                Thread.sleep(5000)
                continue
            }
            val consumerRecords = consumer.poll(Duration.ofMillis(3000))
            kotlin.runCatching {
                measureTimeMillis {
                    consumeMessages(consumerRecords)
                    consumer.commitSync()
                }.also { log.info("Batch processing finished in {}ms", it) }
            }.onFailure { log.error("Error in processing message batch", it) }
        }

    }

    fun seekOffsetToBeginning() {
        consumer.poll(Duration.ofSeconds(10))
        consumer.assignment().forEach {
            consumer.seek(it, 0).also { log.info("setting offset on topic-partition $it to 0") }
        }
    }

    fun stop() {
        listening = false
        consumer.unsubscribe()
    }
}