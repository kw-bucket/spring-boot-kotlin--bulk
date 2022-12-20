package com.kw.bulk.service.bulk

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.worker.DeleteOldDataWorker
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.minutes

@Service
class BulkWatcher(
    @Lazy private val bulkProcessor: BulkProcessor,
    private val bulkSummary: BulkSummary,
    private val deleteOldDataWorker: DeleteOldDataWorker,
    appProperties: AppProperties,
) {

    private val maxAutoRetries: Int = appProperties.bulk.autoRetry.max
    private val retryIntervalMinute: Int = appProperties.bulk.autoRetry.intervalMinutes
    private val emailConfig = appProperties.email

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun inspect(bulkJobId: Long) {
        val summarizedBulkJob = bulkSummary.summarize(bulkJobId)

        val isSuccess = summarizedBulkJob.status == BulkJob.Status.SUCCESS
        val isRetryExceeded = summarizedBulkJob.retryCount >= maxAutoRetries
        val needAutoRerun = !isSuccess && !isRetryExceeded

//        val emailProperties = if (isSuccess) emailConfig.success else emailConfig.failure
        val emailProperties = emailConfig.summary

        deleteOldDataWorker.runAsync(summarizedBulkJob)

        bulkSummary.sendSummaryNotification(
            bulkJob = summarizedBulkJob,
            emailProp = emailProperties,
            attachResults = isSuccess || isRetryExceeded,
        )

        if (needAutoRerun) {
            logger.info("Bulk {} - Perform Auto Rerun in {} minutes", summarizedBulkJob.productCode, retryIntervalMinute)

            delay(retryIntervalMinute.minutes)

            bulkProcessor.run(_bulkJob = summarizedBulkJob, isRetry = true)
        }
    }
}
