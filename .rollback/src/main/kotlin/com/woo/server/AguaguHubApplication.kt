package com.woo.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AguaguHubApplication

fun main(args: Array<String>) {
    runApplication<AguaguHubApplication>(*args)
}
