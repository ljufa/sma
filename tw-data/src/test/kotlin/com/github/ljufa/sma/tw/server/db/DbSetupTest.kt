package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.createConfig
import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.io.File
import java.util.*


open class DbSetupTest {

    val dbRepository: DbRepository

    init {
        dbRepository = DbRepository(dataSource = DataSource(config = createConfig()))
        dbRepository.persistBatch(createConsumerRecords())
    }

    private fun createConsumerRecords(): ConsumerRecords<String, String> {
        val list = mutableListOf<ConsumerRecord<String, String>>()
        val root = JsonPath.parse(File("src/test/resources/mock_kafka_data.json"))
        root.read<JSONArray>("*").forEachIndexed { index, json ->
            list.add(createRecord(index.toLong(), JSONObject(json as MutableMap<String, *>?)))
        }
        return ConsumerRecords(mutableMapOf(TopicPartition("topic", 0) to list))
    }

    private fun createRecord(offset: Long, json: JSONObject): ConsumerRecord<String, String> {
        val consumerRecord = ConsumerRecord("topic", 0, offset, "", json.toString())
        consumerRecord.headers().add("message_id", UUID.randomUUID().toString().toByteArray())
        return consumerRecord
    }


}