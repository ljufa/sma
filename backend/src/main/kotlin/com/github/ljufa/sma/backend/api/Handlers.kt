package com.github.ljufa.sma.backend.api

import com.github.ljufa.sma.backend.db.AccountRepository
import com.github.ljufa.sma.backend.db.User
import com.github.ljufa.sma.backend.db.UserRepository
import com.github.ljufa.sma.backend.ext.TwitterRulesService
import com.github.ljufa.sma.tw.server.api.TopTweetsGrpcKt
import com.github.ljufa.sma.tw.server.api.TopTweetsRequest
import com.google.protobuf.Empty
import io.grpc.Metadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEmpty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*


@Component
class AccountHandler(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
) {
    suspend fun getUserAccount(request: ServerRequest): ServerResponse {
        val principal = request.awaitPrincipal() ?: return ServerResponse.status(403).buildAndAwait()
        val authId = principal.name
        val user =
            userRepository.findByAuthId(authId).onEmpty { emit(userRepository.save(User(authId = authId))) }.first()
        val accounts = accountRepository.findByUserId(user.id!!)
        return ServerResponse.ok().bodyAndAwait(accounts)
    }
}

@Component
class TwitterDataHandler(
    val topTweetsSub: TopTweetsGrpcKt.TopTweetsCoroutineStub,
    val twitterRulesService: TwitterRulesService
) {
    suspend fun getMatchedRules(request: ServerRequest): ServerResponse {
        val result = kotlin.runCatching {
            val matchedRules = topTweetsSub.getMatchedRules(request = Empty.getDefaultInstance(), headers = Metadata())
            val allRules = twitterRulesService.getExistingRules().data
            val apiRules = matchedRules.ruleList.map {
                MatchedRule(
                    it.id,
                    allRules.find { gr -> gr.id == it.id }!!.tag,
                    it.numberOfMatches
                )
            }
            ServerResponse.ok().bodyValueAndAwait(apiRules)
        }
            .onFailure {
                ServerResponse.status(500).buildAndAwait()
            }
        return result.getOrThrow()
    }

    suspend fun getTopTweets(request: ServerRequest): ServerResponse {
        val apiRequest = request.awaitBody<TopTweetsApiRequest>()
        val requestBuilder = TopTweetsRequest.newBuilder()
        requestBuilder.daysFromNow = apiRequest.daysInPast
        requestBuilder.limit = apiRequest.limit
        requestBuilder.excludePossiblySensitive = true
        apiRequest.ruleIds.forEach {
            requestBuilder.addIncludeRuleIds(it)
        }
        val grpcResponse = topTweetsSub.getTopTweets(requestBuilder.build())
        val apiResponse = grpcResponse.statsList.map { TopTweetsApiResponse(it.tweetId, it.numberOfRefs) }
        return ServerResponse.ok().bodyValueAndAwait(apiResponse)
    }
}