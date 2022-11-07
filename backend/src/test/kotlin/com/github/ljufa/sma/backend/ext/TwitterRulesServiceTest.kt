package com.github.ljufa.sma.backend.ext

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TwitterRulesServiceTest {

    @Autowired
    lateinit var service: TwitterRulesService

    @Test
    fun getTwitterApiClient() {
        runBlocking {
            val rules = service.getExistingRules()
            println(rules)
        }
    }
}