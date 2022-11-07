package com.github.ljufa.sma.stream

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Future


class MessageSender {
    private val producer: KafkaProducer<String, String>
    private val log = LoggerFactory.getLogger("com.github.ljufa.toptweets.stream.MessageSender")

    init {
        val props = Properties()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        props[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = 4048576
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = config.kafka.bootstrapServers
        producer = KafkaProducer(props)
    }

    private fun sendMessage(topic: String, message: String): Future<RecordMetadata> {
        val producerRecord = ProducerRecord<String, String>(topic, message)
        producerRecord.headers().add("message_id", UUID.randomUUID().toString().toByteArray())
        return producer.send(producerRecord)
    }

    fun publishToKafka(streamRecord: ByteArray) {
        val message = String(streamRecord)
        if (message.trim().isNotBlank()) {
            log.debug("Message $message")
            sendMessage(config.kafka.topicName, message)
        }
    }

}