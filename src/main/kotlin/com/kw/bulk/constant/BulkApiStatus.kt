package com.kw.bulk.constant

import com.kw.common.starter.constant.ApiOutputStatus

enum class BulkApiStatus(
    override val code: String,
    override val message: String,
    override val description: String
) : ApiOutputStatus {

    BULK2000("BULK2000", "Success", "Success"),

    BULK4000("BULK4000", "Bad Request", "The server cannot or will not process the request"),
    BULK4040("BULK4040", "Not Found", "Not Found"),
    BULK4090("BULK4090", "Conflict", "Conflict with current state of server"),
}
