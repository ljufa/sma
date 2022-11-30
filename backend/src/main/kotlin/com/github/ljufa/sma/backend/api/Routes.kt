package com.github.ljufa.sma.backend.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RoutesConfig(val accountHandler: AccountHandler, val twitterDataHandler: TwitterDataHandler) {

    @Bean
    fun routes() = coRouter {
        "api".nest {
            GET("account", accountHandler::getUserAccount)
            "tw".nest {
                GET("matchedrules", twitterDataHandler::getMatchedRules)
                POST("top", twitterDataHandler::getTopTweets)
            }
        }
    }

}