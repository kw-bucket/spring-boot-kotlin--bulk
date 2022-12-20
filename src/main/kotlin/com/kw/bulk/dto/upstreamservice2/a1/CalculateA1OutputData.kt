package com.kw.bulk.dto.upstreamservice2.a1

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CalculateA1OutputData(
    val clientEncodedKey: String,
    val creditArrangementBlockCode: String,
)
