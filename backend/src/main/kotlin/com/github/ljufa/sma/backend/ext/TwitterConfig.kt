package com.github.ljufa.sma.backend.ext

import com.github.ljufa.sma.tw.server.api.TopTweetsGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class TwitterConfig {

    @ConstructorBinding
    @ConfigurationProperties("tw")
    data class TwProperties(
        val backendServerUrl: String,
        val authToken: String,
        val twApiBaseUrl: String

    )

    @Bean
    fun topTweetsStub(config: TwProperties): TopTweetsGrpcKt.TopTweetsCoroutineStub {
        val channel: ManagedChannel = ManagedChannelBuilder.forTarget(config.backendServerUrl)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()
        return TopTweetsGrpcKt.TopTweetsCoroutineStub(channel)
    }

    @Bean
    fun getTwitterWebClient(twProperties: TwProperties): WebClient {
        return WebClient.builder()
            .baseUrl(twProperties.twApiBaseUrl)
            .defaultHeaders {
                it.contentType = MediaType.APPLICATION_JSON
                it.setBearerAuth(twProperties.authToken)
            }
            .build()
    }

}

