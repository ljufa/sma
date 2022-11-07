package com.github.ljufa.sma.tw.server

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.EnvironmentVariablesPropertySource
import com.sksamuel.hoplite.PropertySource

data class Kafka(
    val bootstrapServers: String,
    val groupId: String,
    val topicName: String,
    val maxPollRecords: Int,
    val maxPollIntervalMs: Int,
    val minBatchPersisSize: Int
)

data class Database(
    val rootDirPath: String,
    val purgeOnBootToken: String?
)

data class Server(val port: Int, val isSsl: Boolean)
data class Config(val database: Database, val server: Server, val kafka: Kafka)


val config = ConfigLoader.Builder()
    .addPropertySource(EnvironmentVariablesPropertySource(useUnderscoresAsSeparator = true, allowUppercaseNames = true))
    .addSource(PropertySource.resource("/application-local.yaml", optional = true))
    .addSource(PropertySource.resource("/application.yaml"))
    .build()
    .loadConfigOrThrow<Config>()
