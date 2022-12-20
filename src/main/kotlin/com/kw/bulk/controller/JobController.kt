package com.kw.bulk.controller

import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.service.job.JobService
import com.kw.common.starter.dto.ApiOutput
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/jobs")
class JobController(
    private val jobService: JobService
) {
    @GetMapping("/recent")
    fun getRecentJobs(): ResponseEntity<ApiOutput<List<BulkJob>>> {
        return ResponseEntity.ok().body(jobService.getRecentJobs())
    }

    @GetMapping("/{id}")
    fun getJob(@PathVariable id: Long): ResponseEntity<ApiOutput<BulkJob>> {
        return ResponseEntity.ok().body(jobService.getJob(id))
    }

    @GetMapping("/{id}/report")
    fun generateReport(
        @PathVariable id: Long,
        @RequestParam to: String
    ): ResponseEntity<ApiOutput<Void>> {
        return ResponseEntity.ok().body(jobService.generateReport(id, to))
    }

    @PostMapping("/{id}")
    fun updateJob(
        @PathVariable id: Long,
        @RequestParam status: String,
        @RequestParam retryCount: Int?,
    ): ResponseEntity<ApiOutput<Void>> {
        return ResponseEntity.ok().body(jobService.updateJob(id, status, retryCount))
    }
}
