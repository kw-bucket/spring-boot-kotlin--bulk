package com.kw.bulk.controller

import com.kw.bulk.dto.bulk.BulkOutputData
import com.kw.bulk.dto.bulk.BulkRequest
import com.kw.bulk.dto.bulk.BulkRerunRequest
import com.kw.bulk.service.bulk.BulkService
import com.kw.common.starter.dto.ApiOutput
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/v1/bulk")
class BulkController(
    private val bulkService: BulkService
) {

    @PostMapping("/run")
    fun activateBulkProcess(
        @RequestBody @Valid requestBody: BulkRequest
    ): ResponseEntity<ApiOutput<BulkOutputData>> {
        return ResponseEntity.ok().body(bulkService.process(requestBody))
    }

    @PostMapping("/rerun")
    fun rerunBulkProcess(
        @RequestBody @Valid requestBody: BulkRerunRequest
    ): ResponseEntity<ApiOutput<BulkOutputData>> {
        return ResponseEntity.ok().body(bulkService.rerun(requestBody))
    }
}
