package com.kw.bulk.service.job

import com.kw.bulk.constant.BulkApiStatus
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.bulk.service.bulk.BulkSummary
import com.kw.common.mail.config.properties.EmailProperties
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.exception.AppException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val bulkSummary: BulkSummary,
) {

    fun getRecentJobs(): ApiOutput<List<BulkJob>> {
        return ApiOutput.fromStatus(
            BulkApiStatus.BULK2000,
            data = jobRepository.findTop15ByOrderByIdDesc(),
        )
    }

    fun getJob(id: Long): ApiOutput<BulkJob> {
        val job = findJob(id)

        return ApiOutput.fromStatus(BulkApiStatus.BULK2000, data = job)
    }

    fun updateJob(id: Long, status: String, retryCount: Int?): ApiOutput<Void> {
        val job = findJob(id)
        jobRepository.save(job.copy(status = status, retryCount = retryCount ?: job.retryCount))

        return ApiOutput.fromStatus(BulkApiStatus.BULK2000)
    }

    fun generateReport(id: Long, emailTo: String): ApiOutput<Void> {
        val job = findJob(id)
        val emailProperties = EmailProperties(
            from = "kw-bulk@service.com",
            to = emailTo,
            subject = "[${job.triggerSource.uppercase()} Summary] - Attempt {job_attempt} - {job_as_of_date}",
            bodyTemplate = "summary.html",
        )
        val attachResults = true

        bulkSummary.sendSummaryNotification(job, emailProperties, attachResults)

        return ApiOutput.fromStatus(BulkApiStatus.BULK2000)
    }

    private fun findJob(id: Long): BulkJob {
        val job = jobRepository.findByIdOrNull(id)
        job ?: throw AppException(HttpStatus.NOT_FOUND, BulkApiStatus.BULK4040)

        return job
    }
}
