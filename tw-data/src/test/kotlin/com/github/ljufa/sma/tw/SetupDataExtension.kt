package com.github.ljufa.sma.tw

import com.github.ljufa.sma.tw.server.db.DbRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.*


class SetupDataExtension : BeforeAllCallback,
    ExtensionContext.Store.CloseableResource {

    override fun beforeAll(context: ExtensionContext) {
        if (!created) {
            DbRepository.persistBatch(createConsumerRecords())
            created = true
        }
    }

    override fun close() {
    }

    private fun createConsumerRecords(): ConsumerRecords<String, String> {
        val list = mutableListOf<ConsumerRecord<String, String>>()
        val element = Json.parseToJsonElement(File("src/test/resources/mock_kafka_data.json").readText())
        element.jsonArray.forEachIndexed { index, json ->
            list.add(createRecord(index.toLong(), json.toString()))
        }

        return ConsumerRecords(mutableMapOf(TopicPartition("topic", 0) to list))
    }

    private fun createRecord(offset: Long, json: String): ConsumerRecord<String, String> {
        val consumerRecord = ConsumerRecord("topic", 0, offset, "", json)
        consumerRecord.headers().add("message_id", UUID.randomUUID().toString().toByteArray())
        return consumerRecord
    }

    companion object {
        private var created = false
    }


}