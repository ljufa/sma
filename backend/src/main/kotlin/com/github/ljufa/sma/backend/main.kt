package com.github.ljufa.sma.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableR2dbcRepositories
@ConfigurationPropertiesScan
class SmaControlApplication

fun main(args: Array<String>) {
    runApplication<SmaControlApplication>(*args)
}

