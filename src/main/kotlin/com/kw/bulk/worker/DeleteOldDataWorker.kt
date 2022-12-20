package com.kw.bulk.worker

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.common.starter.manager.ThreadPoolManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DeleteOldDataWorker(
    private val jobRepository: JobRepository,
    private val collectionTaskRepository: ATaskRepository,
    private val interestTaskRepository: BTaskRepository,
    appProperties: AppProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val retentionDays: Long = appProperties.dataRetentionDays

    fun runAsync(bulkJob: BulkJob) {
        val pool = ThreadPoolManager.initFixedThreadPoolTaskExecutor(nThreads = 1, threadNamePrefix = "Thd-Del-")

        pool.execute {
            val retentionPeriod = bulkJob.asOfDate.minusDays(retentionDays)
            val jobs = jobRepository.findByProductCodeAndAsOfDateBefore(bulkJob.productCode, retentionPeriod)

            if (jobs.isEmpty()) return@execute

            val jobIds = jobs.map { it.id }
            val result = try {
                collectionTaskRepository.deleteByJobIds(jobIds)
                interestTaskRepository.deleteByJobIds(jobIds)
                jobRepository.deleteByIdIn(jobIds)
                "Success"
            } catch (ex: Exception) {
                ex.printStackTrace()
                "Failed"
            }

            logger.info("Delete Old Jobs - Result: {}, Job Ids: {}", result, jobIds)
        }

        pool.shutdown()
    }
}
