package com.github.ljufa.sma.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

const val EXTERNAL_AUTH_ID = "1"

@SpringBootTest
@AutoConfigureWebTestClient
abstract class ApiIntegrationTest {

    @Autowired
    lateinit var client: WebTestClient

}