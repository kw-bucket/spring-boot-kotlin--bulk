package com.kw.bulk.service.bulk

import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.bulk.service.report.CSVExporter
import com.kw.common.mail.config.properties.EmailProperties
import com.kw.common.mail.extension.emailproperties.toRequest
import com.kw.common.mail.service.notification.EmailNotificationService
import com.kw.common.starter.constant.Constant
import com.kw.common.starter.extension.apioutcome.peekOutputStatus
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.File

@Service
class BulkSummary(
    private val emailNotificationService: EmailNotificationService,
    private val jobRepository: JobRepository,
    private val aTaskRepository: ATaskRepository,
    private val bTaskRepository: BTaskRepository,
    private val csvExporter: CSVExporter,
    private val templateEngine: SpringTemplateEngine,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun summarize(jobId: Long): BulkJob {
        val job = jobRepository.getById(jobId)

        val collectionBucketFailure =
            if (job.isA1Enabled) countProductBucketTask(jobId = job.id, completed = false) else 0
        val collectionFlagFailure =
            if (job.isA2Enabled) countCollectionFlagTask(jobId = job.id, completed = false) else 0
        val interestFailure =
            if (job.isBEnabled) countInterestTask(jobId = job.id, completed = false) else 0

        val totalFailure = collectionBucketFailure + collectionFlagFailure + interestFailure

        val status = if (totalFailure > 0) BulkJob.Status.FAILED else BulkJob.Status.SUCCESS

        return jobRepository.save(job.copy(status = status))
    }

    private fun countProductBucketTask(jobId: Long, completed: Boolean): Int =
        aTaskRepository.countByProductBucketCompletedAndJobId(jobId = jobId, completed = completed)

    private fun countCollectionFlagTask(jobId: Long, completed: Boolean): Int =
        aTaskRepository.countByCollectionFlagCompletedAndJobId(jobId = jobId, completed = completed)

    private fun countInterestTask(jobId: Long, completed: Boolean) =
        bTaskRepository.countByJobIdAndCompleted(jobId = jobId, completed = completed)

    fun sendSummaryNotification(
        bulkJob: BulkJob,
        emailProp: EmailProperties,
        attachResults: Boolean = false,
    ) {
        val attachments = if (attachResults) exportCsv(bulkJob) else null

        val result = try {
            val params: Map<String, Any> = mapOf(
                "correlation_id" to MDC.get(Constant.Context.CORRELATION_ID),
                "job" to bulkJob,

                "collection_task_count" to aTaskRepository.countByJobId(jobId = bulkJob.id),
                "collection_bucket_success_count" to
                    if (bulkJob.isA1Enabled) countProductBucketTask(jobId = bulkJob.id, completed = true)
                    else 0,
                "collection_bucket_failure_count" to
                    if (bulkJob.isA1Enabled) countProductBucketTask(jobId = bulkJob.id, completed = false)
                    else 0,
                "collection_flag_success_count" to
                    if (bulkJob.isA2Enabled) countCollectionFlagTask(jobId = bulkJob.id, completed = true)
                    else 0,
                "collection_flag_failure_count" to
                    if (bulkJob.isA2Enabled) countCollectionFlagTask(jobId = bulkJob.id, completed = false)
                    else 0,

                "interest_task_count" to
                    if (bulkJob.isBEnabled) bTaskRepository.countByJobId(jobId = bulkJob.id)
                    else 0,
                "interest_success_count" to countInterestTask(jobId = bulkJob.id, completed = true),
                "interest_failure_count" to countInterestTask(jobId = bulkJob.id, completed = false),
            )

            val emailSubject = emailProp.subject
                ?.replace(oldValue = "{product_code}", newValue = bulkJob.productCode)
                ?.replace(oldValue = "{job_attempt}", newValue = bulkJob.attempts().toString())
                ?.replace(oldValue = "{job_as_of_date}", newValue = bulkJob.asOfDate.toString())

            val emailBody = templateEngine.process(emailProp.bodyTemplate, Context().apply { setVariables(params) })

            val emailNotificationRequest = emailProp.toRequest(
                subject = emailSubject,
                body = emailBody,
                attachments = attachments,
            )

            emailNotificationService.sendEmail(emailNotificationRequest).peekOutputStatus()
                .also {
                    attachments?.forEach { it.delete() }
                }
        } catch (ex: Exception) {
            logger.error(ex.stackTraceToString())
            "Error"
        }

        logger.info(
            "Bulk {} - Send Bulk Summary Email Notification - Job Id: {}, Result: {}",
            bulkJob.productCode, bulkJob.id, result,
        )
    }

    private fun exportCsv(bulkJob: BulkJob): List<File> = runBlocking(MDCContext()) {
        val deferredInterestReport: Deferred<File?> = async(Dispatchers.IO) {
            if (bulkJob.isBEnabled) csvExporter.exportFailedBTasks(bulkJob) else null
        }
        val deferredBucketReport: Deferred<File?> = async(Dispatchers.IO) {
            if (bulkJob.isA1Enabled) csvExporter.exportFailedA1Tasks(bulkJob) else null
        }
        val deferredFlagReport: Deferred<File?> = async(Dispatchers.IO) {
            if (bulkJob.isA2Enabled) csvExporter.exportFailedA2Tasks(bulkJob) else null
        }

        listOfNotNull(
            deferredInterestReport.await(), deferredBucketReport.await(), deferredFlagReport.await()
        )
    }
}
