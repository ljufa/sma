package com.github.ljufa.sma.backend.api

import com.github.ljufa.sma.backend.EXTERNAL_AUTH_ID
import com.github.ljufa.sma.backend.PostgresIntegrationTest
import com.github.ljufa.sma.backend.db.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.context.ActiveProfiles

class AccountApiTest : PostgresIntegrationTest() {
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
    fun `GET account respond 403 for anonymous user`() {
        requestBodySpec().exchange().expectStatus().isForbidden
    }

    private fun requestBodySpec() = client
        .mutateWith(SecurityMockServerConfigurers.csrf())
        .get().uri("/api/account").accept(MediaType.APPLICATION_JSON)
}