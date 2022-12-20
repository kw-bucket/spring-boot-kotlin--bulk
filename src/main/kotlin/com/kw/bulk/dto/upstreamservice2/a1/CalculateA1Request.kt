package com.kw.bulk.dto.upstreamservice2.a1

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDate

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CalculateA1Request(
    val clientEncodedKey: String,
    val productCode: String,
    val currentDate: LocalDate,
    val channel: String = "BULK_CHANNEL",
)
