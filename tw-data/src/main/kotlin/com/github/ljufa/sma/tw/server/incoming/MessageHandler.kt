package com.github.ljufa.sma.tw.server.incoming

import com.github.ljufa.sma.tw.server.Config
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.system.measureTimeMillis


class MessageHandler(val consumer: KafkaConsumer<String, String>) {
    val log: Logger = LoggerFactory.getLogger(MessageHandler::class.java)
    private var listening = true
    private var running = true

    fun listenBatches(
        consumeMessages: (ConsumerRecords<String, String>) -> Unit
    ) {
        while (listening) {
            val consumerRecords = consumer.poll(Duration.ofMillis(2000))
            if (consumerRecords.isEmpty) {
                continue
            }
            runCatching {
                measureTimeMillis {
                    consumeMessages(consumerRecords)
                    consumer.commitSync()
                }.also { log.info("Batch size ${consumerRecords.count()} finished processing in {}ms", it) }
            }.onFailure { log.error("Error in processing message batch", it) }
        }
        consumer.unsubscribe()
        running = false
    }

    fun seekOffsetToBeginning() {
        consumer.poll(Duration.ofSeconds(10))
        consumer.assignment().forEach { topicPartition ->
            consumer.seek(topicPartition, 0).also { log.info("setting offset on topic-partition $it to 0") }
        }
    }

    fun stop() {
        listening = false
        var waitCounter = 0
        while (running && waitCounter++ < 10) {
            log.info("Waiting for consumer thread to finish...")
            Thread.sleep(300)
        }
    }
}