package com.github.ljufa.sma.backend

import com.github.ljufa.sma.backend.api.MatchedRule
import com.github.ljufa.sma.backend.api.TopTweetsApiRequest
import com.github.ljufa.sma.backend.api.TopTweetsApiResponse
import com.github.ljufa.sma.backend.db.*
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

const val EXTERNAL_AUTH_ID = "1"

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
class ApiEndToEndTest(
    @Autowired val client: WebTestClient,
    @Autowired val userRepository: UserRepository,
    @Autowired val accountRepository: AccountRepository
) {


    companion object {
        @Container
        private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:latest")

        @DynamicPropertySource
        @JvmStatic
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { "r2dbc:postgresql://localhost:${postgreSQLContainer.getMappedPort(5432)}/${postgreSQLContainer.databaseName}" }
            registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername)
            registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword)
            registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl)
            registry.add("spring.flyway.username", postgreSQLContainer::getUsername)
            registry.add("spring.flyway.password", postgreSQLContainer::getPassword)
        }
    }

    @BeforeEach
    fun cleanDB() = runBlocking {
        accountRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @WithMockUser(EXTERNAL_AUTH_ID)
    fun `GET account for existing user return two accounts`() {
        runBlocking {
            val user = userRepository.save(User(authId = EXTERNAL_AUTH_ID))
            accountRepository.save(
                Account(
                    userId = user.id!!,
                    externalAccountType = ExternalAccountType.TWITTER,
                    externalAccountId = "tw|123"
                )
            )
            accountRepository.save(
                Account(
                    userId = user.id!!,
                    externalAccountType = ExternalAccountType.FACEBOOK,
                    externalAccountId = "fb|1"
                )
            )
            requestBodySpec().exchange().expectStatus().isOk.expectBodyList(Account::class.java).hasSize(2)
        }
    }

    @Test
    @WithMockUser("2")
    fun `GET account for non existing users return empty list`() {
        requestBodySpec().exchange().expectStatus().isOk.expectBody().json("[]")
    }

    @Test
    fun `GET account respond 401 for anonymous user`() {
        requestBodySpec().exchange().expectStatus().isUnauthorized
    }

    @Test
    @WithMockUser(EXTERNAL_AUTH_ID)
    fun `GET matched twitter rules`() {
        client.mutateWith(csrf()).get().uri("/api/tw/matchedrules")
            .accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk.expectBody<List<MatchedRule>>()
            .consumeWith {
                it.responseBody!! shouldHaveSize 3
            }
    }

    @Test
    @WithMockUser(EXTERNAL_AUTH_ID)
    fun `GET top tweets`() {
        client.mutateWith(csrf())
            .post()
            .uri("/api/tw/top")
            .bodyValue(TopTweetsApiRequest(daysInPast = 4, limit = 13))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody<List<TopTweetsApiResponse>>()
            .consumeWith {
                it.responseBody!! shouldHaveSize 13
            }
    }

    private fun requestBodySpec() = client
        .mutateWith(csrf())
        .get().uri("/api/account").accept(MediaType.APPLICATION_JSON)


}