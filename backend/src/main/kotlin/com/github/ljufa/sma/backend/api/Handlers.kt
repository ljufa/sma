package com.github.ljufa.sma.backend.api

import com.github.ljufa.sma.backend.db.AccountRepository
import com.github.ljufa.sma.backend.db.User
import com.github.ljufa.sma.backend.db.UserRepository
import com.github.ljufa.sma.tw.server.grpc.TopTweetsGrpcKt
import com.github.ljufa.sma.tw.server.grpc.TopTweetsRequest
import com.github.ljufa.sma.tw.server.grpc.TwitterApiGrpcKt
import com.google.protobuf.Empty
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
    val twitterDataStub: TwitterApiGrpcKt.TwitterApiCoroutineStub,
    val globalRules: com.github.ljufa.sma.backend.ext.Rules
) {
    suspend fun getMatchedRules(request: ServerRequest): ServerResponse {
        val matchedRules = twitterDataStub.getMatchedRules(Empty.getDefaultInstance())
        val apiRules = matchedRules.ruleList.map {
            MatchedRule(
                it.id,
                globalRules.data.find { gr -> gr.id == it.id }!!.tag,
                it.numberOfMatches
            )
        }
        return ServerResponse.ok().bodyValueAndAwait(apiRules)
    }

    suspend fun getTopTweets(request: ServerRequest): ServerResponse {
        val apiRequest = request.awaitBody<TopTweetsApiRequest>()
        val requestBuilder = TopTweetsRequest.newBuilder()
        requestBuilder.daysFromNow = apiRequest.daysInPast
        requestBuilder.limit = apiRequest.limit
        apiRequest.ruleIds?.forEach {
            requestBuilder.addIncludeRuleIds(it)
        }
        val grpcResponse = topTweetsSub.getTopTweets(requestBuilder.build())
        val apiResponse = grpcResponse.statsList.map { TopTweetsApiResponse(it.tweetId, it.numberOfRefs) }
        return ServerResponse.ok().bodyValueAndAwait(apiResponse)
    }
}