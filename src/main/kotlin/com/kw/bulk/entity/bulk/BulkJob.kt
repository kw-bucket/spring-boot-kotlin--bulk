package com.kw.bulk.entity.bulk

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.boot.ansi.AnsiOutput.Enabled
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "job")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class BulkJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val status: String = Status.PENDING,
    val asOfDate: LocalDateTime,
    val triggerSource: String,
    val productCode: String,
    val retryCount: Int = 0,
    val isA1Enabled: Boolean = false,
    val isA2Enabled: Boolean = false,
    val isAXEnabled: Boolean = false,
    val isBEnabled: Boolean = false,
    val lastStartedAt: LocalDateTime? = null,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @UpdateTimestamp
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    object Status {
        const val PENDING = "PENDING"
        const val FAILED = "FAILED"
        const val SUCCESS = "SUCCESS"
    }

    fun attempts() = retryCount + 1
}
