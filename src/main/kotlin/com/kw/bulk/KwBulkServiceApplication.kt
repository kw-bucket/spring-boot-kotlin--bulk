package com.kw.bulk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@ConfigurationPropertiesScan
@SpringBootApplication(
    scanBasePackages = [
        "com.kw.bulk",
        "com.kw.common.starter",
        "com.kw.common.mail",
    ]
)
@EnableAsync(proxyTargetClass = true)
class KwBulkServiceApplication

fun main(args: Array<String>) {
    try {
        runApplication<KwBulkServiceApplication>(*args)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
