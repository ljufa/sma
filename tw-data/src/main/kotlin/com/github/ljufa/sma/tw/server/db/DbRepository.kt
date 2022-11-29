package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.*
import com.github.ljufa.sma.tw.server.api.*
import com.google.protobuf.ProtocolStringList
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.lmdbjava.Txn
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.measureTimeMillis


class DbRepository(private val dataSource: DataSource) {

    private val log: Logger = LoggerFactory.getLogger(DbRepository::class.java)
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    fun persistBatch(consumerRecords: ConsumerRecords<String, String>) {
        if (consumerRecords.isEmpty)
            return
        dataSource.writeTxn().let { txnWrite ->
            consumerRecords.forEach {
                runCatching {
                    val tweetRecord = jsonToGrpcObject(it)
                    if (tweetRecord != null) {
                        persistReactions(tweetRecord, txnWrite)
                        persistDateIndex(tweetRecord.tweetId, tweetRecord.createdAt, txnWrite)
                        persistPossiblySensitive(tweetRecord.tweetId, tweetRecord.possiblySensitive, txnWrite)
                        persistLangIndex(tweetRecord.tweetId, tweetRecord.lang, txnWrite)
                        persistMatchedRuleIndex(tweetRecord.tweetId, tweetRecord, txnWrite)
                        persistHashTags(tweetRecord, txnWrite)
                        persistUserMentions(tweetRecord, txnWrite)
                        persistUrls(tweetRecord, txnWrite)
                        persistPublicMetrics(tweetRecord, txnWrite)
                    }
                }.onFailure {
                    log.error("Error", it)
                }
            }
            txnWrite.commit()
        }
    }

    fun getTopTweets(request: TopTweetsRequest): TopTweetsResponse {
        val time = getRelativeDate(request.daysFromNow)
        val builder = TopTweetsResponse.newBuilder()
        runCatching {
            measureTimeMillis {
                val pq = PriorityQueue<TweetStatComparable>(request.limit)
                dataSource.readTxn().let { txn ->
                    dataSource.reactions.iterate(txn).stream()
                        .filter { matchDate(it.key(), time, txn) }
                        .filter { matchRule(it.key(), request.includeRuleIdsList, txn) }
                        .filter { excludePossiblySensitive(it.key(), request.excludePossiblySensitive, txn) }
                        .forEach { reaction ->
                            val twid = reaction.key().str()
                            val numberOfReactions = reaction.`val`().long
                            if (pq.size < request.limit) {
                                pq.add(TweetStatComparable(twid, numberOfReactions))
                            } else if (numberOfReactions > pq.peek().numberOfReactions) {
                                pq.poll()
                                pq.add(TweetStatComparable(twid, numberOfReactions))
                            }
                        }
                }
                builder.addAllStats(pq.sortedByDescending { it.numberOfReactions }
                    .map { TweetStat.newBuilder().setNumberOfRefs(it.numberOfReactions).setTweetId(it.twid).build() }
                    .toList())
            }.also { log.info("Query finished in {}", it) }
        }.onFailure { log.error("err", it) }
        return builder.build()
    }


    fun findAllMatchedRules(): MatchedRules {
        val builder = MatchedRules.newBuilder()
        runCatching {
            measureTimeMillis {
                dataSource.readTxn().let { txn ->
                    dataSource.matchedRulesIndex.iterate(txn)
                        .stream()
                        .map { it.`val`().str() }
                        .toList()
                        .groupingBy { it }
                        .eachCount()
                        .forEach {
                            builder.addRule(Rule.newBuilder().setId(it.key).setTag(it.key).setNumberOfMatches(it.value))
                        }
                }
            }.also { log.debug("matched rules fetch finished in {}ms", it) }
        }.onFailure {
            log.error("Error", it)
            throw Exception(it)
        }
        return builder.build()
    }


    private fun persistUrls(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.urlsList?.forEach { url ->
            dataSource.urls.put(txnWrite, tweetRecord.tweetId.bb(), url.url.bb())
        }
    }

    private fun persistPublicMetrics(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        val totalCount =
            tweetRecord.publicMetrics.retweetCount + tweetRecord.publicMetrics.likeCount + tweetRecord.publicMetrics.quoteCount + tweetRecord.publicMetrics.replyCount
        dataSource.publicMetricsIndex.put(txnWrite, tweetRecord.tweetId.bb(), totalCount.bb())
    }

    private fun persistUserMentions(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.userMentionsList?.forEach { um ->
            dataSource.userMentions.put(txnWrite, tweetRecord.tweetId.bb(), um.username.bb())
        }
    }

    private fun persistHashTags(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.hashtagsList?.forEach { ht ->
            dataSource.hashTags.put(txnWrite, tweetRecord.tweetId.bb(), ht.tag.bb())
        }
    }

    private fun persistLangIndex(twId: String, lang: String, txnWrite: Txn<ByteBuffer>) {
        dataSource.langIndex.put(txnWrite, twId.bb(), lang.bb())
    }


    private fun persistMatchedRuleIndex(twId: String, tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.matchedRuleList?.forEach { mr ->
            dataSource.matchedRulesIndex.put(txnWrite, twId.bb(), mr.id.bb())
        }
    }


    private fun persistDateIndex(twId: String, createdAt: String, txnWrite: Txn<ByteBuffer>) {
        dataSource.dateIndex.put(
            txnWrite,
            twId.bb(),
            formatter.parse(createdAt).toInstant().toEpochMilli().bb()
        )
    }

    private fun persistPossiblySensitive(tweetId: String, possiblySensitive: String, txnWrite: Txn<ByteBuffer>) {
        dataSource.possiblySensitiveIndex.put(
            txnWrite,
            tweetId.bb(),
            possiblySensitive.bb()
        )
    }

    private fun persistReactions(
        tweetRecord: TweetRecord,
        txnWrite: Txn<ByteBuffer>
    ) {
        val tweetId = tweetRecord.tweetId
        tweetRecord.refList?.forEach {
            val exists = incrementReactionIfExists(it, txnWrite)
            if (!exists) {
                dataSource.reactions.put(txnWrite, it.refId.bb(), 1L.bb())
            }
        }
        dataSource.reactions.put(txnWrite, tweetId.bb(), 0L.bb())
    }

    private fun incrementReactionIfExists(it: TweetReferenceVO, txn: Txn<ByteBuffer>): Boolean {
        val twId = it.refId.bb()
        val referencedExisting = dataSource.reactions.get(txn, twId)
        if (referencedExisting != null) {
            val cnt = referencedExisting.long + 1
            dataSource.reactions.put(txn, twId, cnt.bb())
            return true
        } else {
            return false
        }
    }


    private fun matchDate(twId: ByteBuffer?, afterTime: Long?, txn: Txn<ByteBuffer>): Boolean {
        if (afterTime == null) {
            return true
        }
        val get = dataSource.dateIndex.get(txn, twId)
        return get != null && get.long >= afterTime
    }

    private fun matchRule(twId: ByteBuffer, rule: ProtocolStringList?, txn: Txn<ByteBuffer>): Boolean {
        if (rule.isNullOrEmpty()) {
            return true
        }
        val get = dataSource.matchedRulesIndex.get(txn, twId)
        return get != null && rule.contains(get.str())

    }


    private fun excludePossiblySensitive(
        tweetId: ByteBuffer,
        possiblySensitiveOnly: Boolean,
        txn: Txn<ByteBuffer>
    ): Boolean {
        if (!possiblySensitiveOnly) {
            return true
        }
        val get = dataSource.possiblySensitiveIndex.get(txn, tweetId)
        val str = get.str()
        return str == "false"
    }

    class TweetStatComparable(val twid: String, val numberOfReactions: Long) : Comparable<TweetStatComparable> {
        override fun compareTo(other: TweetStatComparable): Int {
            return numberOfReactions.compareTo(other.numberOfReactions)
        }
    }

}