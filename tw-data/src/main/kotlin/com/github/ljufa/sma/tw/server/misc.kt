package com.github.ljufa.sma.tw.server

import com.github.ljufa.sma.tw.server.db.*
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.lmdbjava.CursorIterable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Stream

private val jsonConf: Configuration = Configuration.defaultConfiguration()
    .addOptions(Option.SUPPRESS_EXCEPTIONS)

fun getRelativeDate(daysFromNow: Int): Long {
    val localDate = LocalDateTime.now().minusDays(daysFromNow.toLong())
    return localDate.toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun jsonToGrpcObject(consumerRecord: ConsumerRecord<String, String>): TweetRecord? {
    val value = consumerRecord.value()
    val context = runCatching { JsonPath.using(jsonConf).parse(value) }.getOrNull() ?: return null
    val twId = context.read<String>("$.data.id") ?: return null
    val kafkaMessageId = String(consumerRecord.headers().lastHeader("message_id").value())
    val builder = TweetRecord.newBuilder()
        .setMessageId(kafkaMessageId)
        .setTweetId(twId)
        .setAuthor(context.read("$.data.author_id"))
        .setCreatedAt(context.read("$.data.created_at"))
        .setLang(context.read("$.data.lang"))
        .setText(context.read("$.data.text"))


    val possiblySensitive: Boolean? = context.read("$.data.possibly_sensitive")
    if (possiblySensitive != null) {
        builder.possiblySensitive = possiblySensitive.toString()
    } else {
        builder.possiblySensitive = "unknown"
    }

    val publicMetrics: Map<String, Long>? = context.read("$.data.public_metrics")
    val publicMetricsBuilder = PublicMetricsVO.newBuilder()
    publicMetrics?.map {
        when (it.key) {
            "like_count" -> publicMetricsBuilder.likeCount = it.value
            "reply_count" -> publicMetricsBuilder.replyCount = it.value
            "quote_count" -> publicMetricsBuilder.quoteCount = it.value
            "retweet_count" -> publicMetricsBuilder.quoteCount = it.value
        }
    }
    builder.setPublicMetrics(publicMetricsBuilder)
    val refTws: List<Map<String, String>>? = context.read("$.data.referenced_tweets")
    refTws?.forEach { ref ->
        builder.addRef(
            TweetReferenceVO.newBuilder()
                .setId(twId)
                .setRefId(ref["id"].toString())
                .setType(ref["type"].toString())
        )
    }
    val matchedRules: List<Map<Any, Any>> = context.read("$.matching_rules")
    matchedRules.forEach { mr ->
        builder.addMatchedRule(
            MatchedRuleVO.newBuilder()
                .setId(mr["id"].toString())
                .setTag(mr["tag"].toString())
        )
    }
    val hashTags: List<Map<String, String>>? = context.read("$.data.entities.hashtags")
    hashTags?.forEach { ht ->
        builder.addHashtags(HashtagVO.newBuilder().setId(twId).setTag(ht["tag"]))
    }
    val mentions: List<Map<String, String>>? = context.read("$.data.entities.mentions")
    mentions?.forEach { mt ->
        builder.addUserMentions(UserMentionVO.newBuilder().setId(twId).setUsername(mt["username"]))
    }
    val urls: List<Map<String, String>>? = context.read("$.data.entities.urls")
    urls?.forEach { ur ->
        builder.addUrls(
            UrlVO.newBuilder().setId(twId)
                .setDisplayUrl(ur["display_url"] ?: "")
                .setExpandedUrl(ur["expanded_url"] ?: "")
                .setTitle(ur["title"] ?: "")
                .setUrl(ur["url"])
        )
    }
    return builder.build()
}


fun String.bb(): ByteBuffer {
    val bytes = this.toByteArray(StandardCharsets.UTF_8)
    val allocate = ByteBuffer.allocateDirect(bytes.size)
    return allocate.put(bytes).flip()
}

fun Long.bb(): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(8)
    return byteBuffer.putLong(this).flip()
}

fun ByteBuffer.str(): String =
    StandardCharsets.UTF_8.decode(this).toString()

fun CursorIterable<ByteBuffer>.stream(): Stream<CursorIterable.KeyVal<ByteBuffer>> =
    com.google.common.collect.Streams.stream(this)


fun createKafkaConsumer(config: Config): KafkaConsumer<String, String> {
    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = config.kafka.bootstrapServers
    props[ConsumerConfig.GROUP_ID_CONFIG] = config.kafka.groupId
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
    props[ConsumerConfig.FETCH_MAX_BYTES_CONFIG] = 8048576
    props[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = 8048576
    props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = config.kafka.maxPollRecords
    props[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = config.kafka.maxPollIntervalMs
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    val kafkaConsumer = KafkaConsumer<String, String>(props)
    kafkaConsumer.subscribe(mutableListOf(config.kafka.topicName))
    return kafkaConsumer
}
