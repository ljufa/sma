package com.github.ljufa.sma.backend.ext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange


@Component
class TwitterRulesService(val twitterApiClient: WebClient) {

    var log: Logger = LoggerFactory.getLogger(TwitterRulesService::class.java)


    suspend fun getExistingRules(): Rules {
        return twitterApiClient.get().awaitExchange { it.awaitBody(Rules::class) }
    }

}
