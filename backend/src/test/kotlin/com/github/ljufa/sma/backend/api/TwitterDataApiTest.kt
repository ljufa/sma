package com.github.ljufa.sma.backend.api

import com.github.ljufa.sma.backend.ApiIntegrationTest
import com.github.ljufa.sma.backend.EXTERNAL_AUTH_ID
import com.github.ljufa.sma.backend.db.*
import com.github.ljufa.sma.backend.ext.Rule
import com.github.ljufa.sma.backend.ext.Rules
import com.github.ljufa.sma.backend.ext.TwitterRulesService
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import io.mockk.*
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest
@ActiveProfiles("test")
class TwitterDataApiTest : ApiIntegrationTest() {


    @TestConfiguration
    class TestConfig {
        @Bean
        fun twitterRuleService(): TwitterRulesService {
            return TwitterRulesService(
                twitterApiClient = mockk(),
                existingRules = Rules(
                    listOf(
                        Rule(
                            id = "1",
                            tag = "berlin",
                            value = "berlin has media"
                        ), Rule(id = "2", tag = "paris", value = "paris has media")
                    )
                )
            )
        }
    }

    @Test
    @WithMockUser(EXTERNAL_AUTH_ID)
    fun `GET matched twitter rules`() {
        client.mutateWith(SecurityMockServerConfigurers.csrf()).get().uri("/api/tw/matchedrules")
            .accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk.expectBody<List<MatchedRule>>()
            .consumeWith {
                it.responseBody!! shouldHaveSize 3
            }
    }

    @Test
    @WithMockUser(EXTERNAL_AUTH_ID)
    fun `GET top tweets`() {
        client.mutateWith(SecurityMockServerConfigurers.csrf())
            .post()
            .uri("/api/tw/top")
            .bodyValue(TopTweetsApiRequest(daysInPast = 4, limit = 13, ruleIds = emptyList()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<List<TopTweetsApiResponse>>()
            .consumeWith {
                it.responseBody!! shouldHaveSize 13
            }
    }

}