package com.github.ljufa.sma.tw.server.service

import com.github.ljufa.sma.tw.server.db.DbRepository
import com.github.ljufa.sma.tw.server.grpc.*
import com.google.protobuf.Empty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TwitterApiService : TwitterApiGrpcKt.TwitterApiCoroutineImplBase() {
    val log: Logger = LoggerFactory.getLogger(TwitterApiService::class.java)

    override suspend fun getMatchedRules(request: Empty): MatchedRules {
        return DbRepository.findAllMatchedRules()
    }

    override suspend fun getLanguages(request: ByRuleRequest): Languages {
        return DbRepository.findAllLanguages()
    }

    override suspend fun getHashTags(request: ByRuleRequest): HashTags {
        return DbRepository.findAllHashTags()
    }

    override suspend fun getUserMentions(request: ByRuleRequest): UserMentions {
        return DbRepository.findAllUserMentions()
    }

}
