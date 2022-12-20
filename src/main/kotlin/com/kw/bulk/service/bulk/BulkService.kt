package com.kw.bulk.service.bulk

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.constant.BulkApiStatus
import com.kw.bulk.dto.bulk.BulkOutputData
import com.kw.bulk.dto.bulk.BulkRequest
import com.kw.bulk.dto.bulk.BulkRerunRequest
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.common.mail.extension.emailproperties.toRequest
import com.kw.common.mail.service.notification.EmailNotificationService
import com.kw.common.starter.constant.Constant
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.exception.AppException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BulkService(
    private val emailNotificationService: EmailNotificationService,
    private val jobRepository: JobRepository,
    private val bulkProcessor: BulkProcessor,
    private val templateEngine: SpringTemplateEngine,
    private val appProperties: AppProperties,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val emailConfig = appProperties.email

    fun process(request: BulkRequest): ApiOutput<BulkOutputData> {
        val requestDay: Int = request.asOfDateTime.dayOfMonth
        val triggerDay: Int = BulkRequest.TriggerSource.DAY[request.triggerSource] ?: 0

        val isTriggerable = requestDay == triggerDay || request.triggerSource == BulkRequest.TriggerSource.ADHOC
        if (!isTriggerable) {
            throw AppException(
                httpStatus = HttpStatus.BAD_REQUEST, apiOutputStatus = BulkApiStatus.BULK4000,
                description = "${request.triggerSource} cannot trigger on day $requestDay"
            )
        }

        val job = initiateBulkJob(request)

        sendHeartbeatNotification(request)
        bulkProcessor.run(job)

        return ApiOutput.fromStatus(apiOutputStatus = BulkApiStatus.BULK2000, data = BulkOutputData(jobId = job.id))
    }

    fun rerun(request: BulkRerunRequest): ApiOutput<BulkOutputData> {
        val latestJob = findLatestBulkJob(request.productCode)

        val bulkJob = if (request.fresh) {
            jobRepository.save(
                BulkJob(
                    asOfDate = latestJob.asOfDate, triggerSource = latestJob.triggerSource,
                    productCode = latestJob.productCode,
                    isA1Enabled = latestJob.isA1Enabled,
                    isA2Enabled = latestJob.isA2Enabled,
                    isBEnabled = latestJob.isBEnabled,
                )
            )
        } else latestJob

        val retryable = bulkJob.retryCount >= appProperties.bulk.autoRetry.max
        if (!retryable)
            throw AppException(HttpStatus.CONFLICT, BulkApiStatus.BULK4090, "Max retries don't exceeded")

        bulkProcessor.run(bulkJob, isRetry = !request.fresh)

        return ApiOutput.fromStatus(apiOutputStatus = BulkApiStatus.BULK2000, data = BulkOutputData(jobId = bulkJob.id))
    }

    private fun initiateBulkJob(request: BulkRequest): BulkJob {
        val latestBulkJob: BulkJob? = jobRepository.findFirstByProductCodeOrderByIdDesc(productCode = request.productCode)

        latestBulkJob?.also { job ->
            val requestDate = request.asOfDateTime.toLocalDate()
            val latestJobDate = job.asOfDate.toLocalDate()

            if (requestDate <= latestJobDate || latestBulkJob.status == BulkJob.Status.PENDING) {
                logger.error("Bulk {} - Latest job date is {} [{}]!", job.productCode, latestJobDate, latestBulkJob.status)

                throw AppException(
                    httpStatus = HttpStatus.CONFLICT,
                    apiOutputStatus = BulkApiStatus.BULK4090,
                    description = "Invalid request date or latest job is pending",
                )
            }
        }

        return jobRepository.save(
            BulkJob(
                asOfDate = request.asOfDateTime, triggerSource = request.triggerSource, productCode = request.productCode,
                isA1Enabled = request.processA.isA1Enabled,
                isA2Enabled = request.processA.isA2Enabled,
                isBEnabled = request.processB.isBEnabled,
            )
        )
    }

    private fun findLatestBulkJob(productCode: String): BulkJob {
        val latestBulkJob: BulkJob? = jobRepository.findFirstByProductCodeOrderByIdDesc(productCode = productCode)

        latestBulkJob ?: throw AppException(HttpStatus.NOT_FOUND, BulkApiStatus.BULK4040)

        if (latestBulkJob.status != BulkJob.Status.FAILED) {
            throw AppException(HttpStatus.CONFLICT, BulkApiStatus.BULK4090, "Latest job don't fail")
        }

        return latestBulkJob
    }

    private fun sendHeartbeatNotification(bulkRequest: BulkRequest) {
        val params = mapOf(
            "job_triggered" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "job_name" to "Bulk",
            "product_code" to bulkRequest.productCode,
            "correlation_id" to MDC.get(Constant.Context.CORRELATION_ID),
        )
        val subject = emailConfig.heartbeat.subject?.replace("{product_code}", bulkRequest.productCode)
        val body = templateEngine.process(
            emailConfig.heartbeat.bodyTemplate,
            Context().apply { setVariables(params) }
        )
        val request = emailConfig.heartbeat.toRequest(
            subject = subject,
            body = body,
        )

        emailNotificationService.sendEmailAsync(request)
    }
}
