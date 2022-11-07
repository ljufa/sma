package com.github.ljufa.sma.tw.server.service

import com.github.ljufa.sma.tw.server.db.DbRepository
import com.github.ljufa.sma.tw.server.grpc.TopTweetsGrpcKt
import com.github.ljufa.sma.tw.server.grpc.TopTweetsRequest
import com.github.ljufa.sma.tw.server.grpc.TopTweetsResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TopTweetsService : TopTweetsGrpcKt.TopTweetsCoroutineImplBase() {
    val log: Logger = LoggerFactory.getLogger(TopTweetsService::class.java)

    override suspend fun getTopTweets(request: TopTweetsRequest): TopTweetsResponse {
        return DbRepository.getTopTweets(request)
    }


}

