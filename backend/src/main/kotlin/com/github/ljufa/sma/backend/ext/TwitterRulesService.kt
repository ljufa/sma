package com.github.ljufa.sma.backend.ext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange


@Component
class TwitterRulesService(val twitterApiClient: WebClient, private var existingRules: Rules? = null) {

    var log: Logger = LoggerFactory.getLogger(TwitterRulesService::class.java)

    suspend fun getExistingRules(): Rules {
        if (existingRules == null) {
            existingRules = twitterApiClient.get().awaitExchange { response -> response.awaitBody(Rules::class) }
        }
        return existingRules!!
    }

}
