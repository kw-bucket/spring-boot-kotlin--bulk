package com.kw.bulk.dto.upstreamservice1.b

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ApplyBRequest(
    val transactionDatetime: String,
)
