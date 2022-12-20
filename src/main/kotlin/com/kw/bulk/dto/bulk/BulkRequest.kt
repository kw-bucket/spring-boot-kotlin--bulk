package com.kw.bulk.dto.bulk

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.kw.bulk.validation.ValidBulkProductCode
import com.kw.bulk.validation.ValidProcessA
import java.time.LocalDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class BulkRequest(
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    val asOfDateTime: LocalDateTime,

    @field:ValidBulkProductCode
    val productCode: String,

    val triggerSource: String = TriggerSource.ADHOC,

    @field:ValidProcessA
    val processA: ProcessA = ProcessA(),
    val processB: ProcessB = ProcessB(),
) {
    object ProductCode {
        const val PAYLATER = "PAYLATER"
        const val PAYNEXTEXTRA = "PAYNEXTEXTRA"

        val ALL = listOf(PAYLATER, PAYNEXTEXTRA)
    }

    object TriggerSource {
        const val ADHOC = "ADHOC"

        const val MAMBUWEBHOOK = "MAMBUWEBHOOK"
        const val JENKINS = "JENKINS"

        val DAY: HashMap<String, Int> = hashMapOf(
            MAMBUWEBHOOK to 2,
            JENKINS to 5,
        )
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class ProcessA(
        val isA1Enabled: Boolean = false,
        val isA2Enabled: Boolean = false,
        val isAXEnabled: Boolean = false,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class ProcessB(
        val isBEnabled: Boolean = false,
    )
}
