package com.kw.bulk.service.bulk

import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.bulk.service.bulk.a.AService
import com.kw.bulk.service.bulk.b.BService
import com.kw.common.starter.manager.ThreadPoolManager
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BulkProcessor(
    private val aService: AService,
    private val bService: BService,
    private val bulkWatcher: BulkWatcher,
    private val jobRepository: JobRepository,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Async("BulkThreadPoolTaskExcutor")
    fun run(_bulkJob: BulkJob, isRetry: Boolean = false) {
        val bulkJob = _bulkJob.copy(
            lastStartedAt = LocalDateTime.now(),
            retryCount = if (isRetry) _bulkJob.retryCount.inc() else _bulkJob.retryCount,
            status = BulkJob.Status.PENDING,
        ).let {
            jobRepository.save(it)
        }.also {
            logger.info(
                "Bulk {} Started! - JobId: {}, AsOfDateTime: {}, Attempt: {}",
                it.productCode, it.id, it.asOfDate, it.attempts(),
            )
        }

        val mdcContext = MDCContext()
        val pool = ThreadPoolManager.initFixedThreadPoolTaskExecutor(
            nThreads = 2,
            threadNamePrefix = "Thd-${bulkJob.productCode}-Bulk-",
        )
        val dispatcher = pool.asCoroutineDispatcher()

        runBlocking(mdcContext) {
            listOf(
                launch(dispatcher) { aService.process(bulkJob = bulkJob, coroutineScope = this, isRetry = isRetry) },
                launch(dispatcher) { bService.process(bulkJob = bulkJob, coroutineScope = this, isRetry = isRetry) },
            ).joinAll()

            bulkWatcher.inspect(bulkJobId = bulkJob.id).also {
                logger.info("Bulk {} - Attempt: {} Done!", bulkJob.productCode, bulkJob.attempts())
            }
        }.also { pool.shutdown() }
    }
}
