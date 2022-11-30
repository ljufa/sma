package com.github.ljufa.sma.backend.api

import com.github.ljufa.sma.backend.db.*
import com.github.ljufa.sma.backend.ext.Rule
import com.github.ljufa.sma.backend.ext.Rules
import com.github.ljufa.sma.backend.ext.TwitterRulesService
import com.github.ljufa.sma.tw.server.api.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.server.EntityResponse
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono


@ActiveProfiles("test")
class TwitterDataHandlerTest {

    private val topTweetsStub = mockk<TopTweetsGrpcKt.TopTweetsCoroutineStub>(relaxed = true)

    private val twitterRulesService = mockk<TwitterRulesService>(relaxed = true)

    private var twitterDataHandler: TwitterDataHandler = TwitterDataHandler(topTweetsStub, twitterRulesService)

    init {
        coEvery { twitterRulesService.getExistingRules() } returns Rules(
            listOf(
                Rule(
                    id = "1",
                    tag = "berlin",
                    value = "berlin has media"
                ), Rule(id = "2", tag = "paris", value = "paris has media")
            )
        )
        coEvery { topTweetsStub.getMatchedRules(any(), any()) } returns MatchedRules.newBuilder()
            .addRule(com.github.ljufa.sma.tw.server.api.Rule.newBuilder().setId("1").setTag("Berlin"))
            .addRule(com.github.ljufa.sma.tw.server.api.Rule.newBuilder().setId("2").setTag("Paris"))
            .build()
    }

    @Test
    fun `should get matched twitter rules`(): Unit = runBlocking {
        val response = twitterDataHandler.getMatchedRules(mockServerRequest())
        val rules = (response as EntityResponse<List<Rules>>).entity()
        Assertions.assertThat(rules).hasSize(2)
    }


    @Test
    fun `should return top tweets`(): Unit = runBlocking {
        coEvery { topTweetsStub.getTopTweets(any(), any()) } returns TopTweetsResponse.newBuilder()
            .addStats(TweetStat.newBuilder().setTweetId("1").setNumberOfRefs(100))
            .addStats(TweetStat.newBuilder().setTweetId("2").setNumberOfRefs(10))
            .addStats(TweetStat.newBuilder().setTweetId("3").setNumberOfRefs(1))
            .build()
        val response = twitterDataHandler.getTopTweets(
            mockServerRequest(
                body = TopTweetsApiRequest(
                    daysInPast = 1,
                    ruleIds = listOf("1", "2"),
                    limit = 2
                )
            )
        )
        val topTweets = (response as EntityResponse<List<TopTweetsApiResponse>>).entity()
        topTweets shouldHaveSize 3
        topTweets[0].numberOfRefs shouldBe 100
        topTweets[1].numberOfRefs shouldBe 10
        topTweets[2].numberOfRefs shouldBe 1

    }

    private fun mockServerRequest(pathVariables: Map<String, String> = emptyMap(), body: Any? = null): ServerRequest {
        val httpRequest = MockServerHttpRequest.get("/dummy")
        val webExchange = MockServerWebExchange.builder(httpRequest).build()
        val requestBuilder = MockServerRequest.builder()
            .exchange(webExchange)
            .apply { pathVariables.forEach(this::pathVariable) }
        if (body != null)
            return requestBuilder.body(Mono.just(body))
        return requestBuilder.build()
    }

}