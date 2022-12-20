package com.kw.bulk.dto.upstreamservice2.a2

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CalculateA2Request(
    val clientEncodedKey: String,
    val productCode: String,
)
