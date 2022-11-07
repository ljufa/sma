package com.github.ljufa.sma.stream

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class FilteredSearchApi {

    private val log = LoggerFactory.getLogger("com.github.ljufa.toptweets.stream.FilteredSearchApi")

    fun stream(consume: (streamRecord: ByteArray) -> Unit) {
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitter.com/2/tweets/search/stream?tweet.fields=public_metrics,lang,created_at,author_id,referenced_tweets,entities,possibly_sensitive"))
                .header(
                    "Authorization",
                    "Bearer ${config.twitter.authToken}"
                )
                .GET()
                .build()
        HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofByteArrayConsumer { it.ifPresent { t -> consume(t) } })

    }
}
