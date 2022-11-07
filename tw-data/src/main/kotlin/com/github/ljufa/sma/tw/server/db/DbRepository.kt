package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.*
import com.github.ljufa.sma.tw.server.grpc.*
import com.google.protobuf.ProtocolStringList
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.lmdbjava.Txn
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.streams.toList
import kotlin.system.measureTimeMillis


object DbRepository {

    private val log: Logger = LoggerFactory.getLogger(DbRepository::class.java)
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    fun persistBatch(consumerRecords: ConsumerRecords<String, String>) {
        DataSource.writeTxn().use { txnWrite ->
            consumerRecords.forEach {
                kotlin.runCatching {
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
                    }
                }.onFailure {
                    log.error("Error", it)
                }
            }
            txnWrite.commit()
        }
    }

    fun getTopTweets(request: TopTweetsRequest): TopTweetsResponse {
        log.debug("Requested ${request.excludePossiblySensitive}")
        val time = getRelativeDate(request.daysFromNow)
        val builder = TopTweetsResponse.newBuilder()
        kotlin.runCatching {
            measureTimeMillis {
                val pq = PriorityQueue<TweetStatComparable>(request.limit)
                DataSource.readTxn().use { txn ->
                    DataSource.reactions.iterate(txn).stream()
                        .filter { matchDate(it.key(), time, txn) }
                        .filter { matchLanguage(it.key(), request.includeLanguagesList, txn) }
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
        kotlin.runCatching {
            measureTimeMillis {
                DataSource.readTxn().use { txn ->
                    DataSource.matchedRulesIndex.iterate(txn)
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

    fun findAllLanguages(): Languages {
        val builder = Languages.newBuilder()
        kotlin.runCatching {
            measureTimeMillis {
                DataSource.readTxn().use { txn ->
                    DataSource.langIndex.iterate(txn)
                        .stream()
                        .map { it.`val`().str() }
                        .toList()
                        .groupingBy { it }
                        .eachCount()
                        .forEach {
                            builder.addLanguage(
                                Language.newBuilder().setId(it.key).setLabel(it.key).setNumberOfMatches(it.value)
                            )
                        }
                }
            }.also { log.debug("language fetch finished in {}ms", it) }
        }.onFailure { log.error("error", it) }
        return builder.build()
    }

    fun findAllHashTags(): HashTags {
        val builder = HashTags.newBuilder()
        kotlin.runCatching {
            measureTimeMillis {
                DataSource.readTxn().use { txn ->
                    DataSource.hashTags.iterate(txn)
                        .stream()
                        .map { it.`val`().str() }
                        .toList()
                        .groupingBy { it }
                        .eachCount()
                        .filter { it.value > 20 }
                        .forEach {
                            builder.addHashtag(
                                HashTag.newBuilder().setTag(it.key).setNumberOfMatches(it.value)
                            )
                        }
                }
            }.also { log.debug("hashtags fetch finished in {}ms", it) }
        }.onFailure { log.error("error", it) }
        return builder.build()
    }

    fun findAllUserMentions(): UserMentions {
        val builder = UserMentions.newBuilder()
        kotlin.runCatching {
            measureTimeMillis {
            }.also { log.debug("Usermentions fetch finished in {}ms", it) }
        }.onFailure { log.error("error", it) }
        return builder.build()
    }


    private fun persistUrls(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.urlsList?.forEach { url ->
            DataSource.urls.put(txnWrite, tweetRecord.tweetId.bb(), url.url.bb())
        }
    }

    private fun persistUserMentions(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.userMentionsList?.forEach { um ->
            DataSource.userMentions.put(txnWrite, tweetRecord.tweetId.bb(), um.username.bb())
        }
    }

    private fun persistHashTags(tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.hashtagsList?.forEach { ht ->
            DataSource.hashTags.put(txnWrite, tweetRecord.tweetId.bb(), ht.tag.bb())
        }
    }

    private fun persistLangIndex(twId: String, lang: String, txnWrite: Txn<ByteBuffer>) {
        DataSource.langIndex.put(txnWrite, twId.bb(), lang.bb())
    }


    private fun persistMatchedRuleIndex(twId: String, tweetRecord: TweetRecord, txnWrite: Txn<ByteBuffer>) {
        tweetRecord.matchedRuleList?.forEach { mr ->
            DataSource.matchedRulesIndex.put(txnWrite, twId.bb(), mr.id.bb())
        }
    }


    private fun persistDateIndex(twId: String, createdAt: String, txnWrite: Txn<ByteBuffer>) {
        DataSource.dateIndex.put(
            txnWrite,
            twId.bb(),
            formatter.parse(createdAt).toInstant().toEpochMilli().bb()
        )
    }
    private fun persistPossiblySensitive(tweetId: String, possiblySensitive: String, txnWrite: Txn<ByteBuffer>) {
        DataSource.possiblySensitiveIndex.put(
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
                DataSource.reactions.put(txnWrite, it.refId.bb(), 1L.bb())
            }
        }
        DataSource.reactions.put(txnWrite, tweetId.bb(), 0L.bb())
    }

    private fun incrementReactionIfExists(it: TweetReferenceVO, txn: Txn<ByteBuffer>): Boolean {
        val twId = it.refId.bb()
        val referencedExisting = DataSource.reactions.get(txn, twId)
        if (referencedExisting != null) {
            val cnt = referencedExisting.long + 1
            DataSource.reactions.put(txn, twId, cnt.bb())
            return true
        } else {
            return false
        }
    }


    private fun matchDate(twId: ByteBuffer?, afterTime: Long?, txn: Txn<ByteBuffer>): Boolean {
        if (afterTime == null) {
            return true
        }
        val get = DataSource.dateIndex.get(txn, twId)
        return get != null && get.long >= afterTime
    }

    private fun matchRule(twId: ByteBuffer, rule: ProtocolStringList?, txn: Txn<ByteBuffer>): Boolean {
        if (rule == null || rule.isEmpty()) {
            return true
        }
        val get = DataSource.matchedRulesIndex.get(txn, twId)
        return get != null && rule.contains(get.str())

    }

    private fun matchLanguage(tweetId: ByteBuffer, lang: ProtocolStringList?, txn: Txn<ByteBuffer>): Boolean {
        if (lang == null || lang.isEmpty())
            return true
        val get = DataSource.langIndex.get(txn, tweetId)
        return get != null && lang.contains(get.str())
    }

    private fun excludePossiblySensitive(tweetId: ByteBuffer, possiblySensitiveOnly: Boolean, txn: Txn<ByteBuffer>): Boolean {
        if(!possiblySensitiveOnly){
            return true
        }
        val get = DataSource.possiblySensitiveIndex.get(txn, tweetId)
        val str = get.str()
        return str == "false"
    }

    class TweetStatComparable(val twid: String, val numberOfReactions: Long) : Comparable<TweetStatComparable> {
        override fun compareTo(other: TweetStatComparable): Int {
            return numberOfReactions.compareTo(other.numberOfReactions)
        }
    }

}