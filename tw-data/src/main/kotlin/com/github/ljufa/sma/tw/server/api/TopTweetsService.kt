package com.github.ljufa.sma.tw.server.api

import com.github.ljufa.sma.tw.server.db.DbRepository
import com.google.protobuf.Empty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TopTweetsService(private val dbRepository: DbRepository) : TopTweetsGrpcKt.TopTweetsCoroutineImplBase() {
    val log: Logger = LoggerFactory.getLogger(TopTweetsService::class.java)

    override suspend fun getTopTweets(request: TopTweetsRequest): TopTweetsResponse {
        return dbRepository.getTopTweets(request)
    }
    override suspend fun getMatchedRules(request: Empty): MatchedRules {
        return dbRepository.findAllMatchedRules()
    }
}

