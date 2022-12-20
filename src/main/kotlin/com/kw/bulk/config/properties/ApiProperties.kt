package com.kw.bulk.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("api")
data class ApiProperties(
    val upstreamService1: ApiServiceProperties,
    val upstreamService2: ApiServiceProperties,
) {
    data class ApiServiceProperties(val baseUrl: String, val accessKey: String?)
}
