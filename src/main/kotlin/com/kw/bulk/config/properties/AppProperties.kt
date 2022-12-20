package com.kw.bulk.config.properties

import com.kw.common.mail.config.properties.EmailProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("config")
data class AppProperties(
    val dataRetentionDays: Long,
    val bulk: BulkConfig,
    val query: QueryProperties,
    val email: EmailConfig,
) {
    data class BulkConfig(
        val autoRetry: AutoRetry,
        val concurrent: Concurrent,
    ) {
        data class Concurrent(
            val aTask: Int = 10,
            val bTask: Int = 10,
        )
    }

    data class AutoRetry(
        val max: Int = 0,
        val intervalMinutes: Int = 0,
    )

    data class QueryProperties(val batchSize: BatchSize) {
        data class BatchSize(
            val bulk: Int = 500,
            val buildTask: Int = 500,
        )
    }

    data class EmailConfig(
        val heartbeat: EmailProperties,
        val summary: EmailProperties,
    )
}
