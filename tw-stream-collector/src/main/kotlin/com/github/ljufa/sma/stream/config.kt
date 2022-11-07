package com.github.ljufa.sma.stream

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.EnvironmentVariablesPropertySource
import com.sksamuel.hoplite.PropertySource

data class Twitter(val authToken: String)
data class Kafka(val bootstrapServers: String, val topicName: String)
data class Config(val kafka: Kafka, val twitter: Twitter)

val config = ConfigLoader.Builder()
    .addPropertySource(EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true))
    .addSource(PropertySource.resource("/application-local.yaml", optional = true))
    .addSource(PropertySource.resource("/application.yaml"))
    .build()
    .loadConfigOrThrow<Config>()
