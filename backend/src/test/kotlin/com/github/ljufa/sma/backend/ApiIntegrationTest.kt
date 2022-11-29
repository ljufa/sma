package com.github.ljufa.sma.backend

import com.github.ljufa.sma.backend.db.AccountRepository
import com.github.ljufa.sma.backend.db.UserRepository
import com.github.ljufa.sma.backend.ext.Rule
import com.github.ljufa.sma.backend.ext.Rules
import com.github.ljufa.sma.backend.ext.TwitterRulesService
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

const val EXTERNAL_AUTH_ID = "1"

@SpringBootTest
@AutoConfigureWebTestClient
abstract class ApiIntegrationTest {

    @Autowired
    lateinit var client: WebTestClient

}