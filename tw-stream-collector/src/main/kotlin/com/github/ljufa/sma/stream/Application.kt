package com.github.ljufa.sma.stream


fun main() {
    val messageSender = MessageSender()
    val twitterApi = FilteredSearchApi()
    twitterApi.stream { streamRecord -> messageSender.publishToKafka(streamRecord) }
}

