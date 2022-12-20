package com.kw.bulk.dto.bulk

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.kw.bulk.validation.ValidBulkProductCode

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class BulkRerunRequest(
    val fresh: Boolean = false,

    @field:ValidBulkProductCode
    val productCode: String,
)
