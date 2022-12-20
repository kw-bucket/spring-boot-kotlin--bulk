package com.kw.bulk.service.bulk.b

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.dto.upstreamservice1.b.ApplyBRequest
import com.kw.bulk.entity.bulk.BTask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.service.api.BUpstreamApiService
import com.kw.bulk.service.bulk.task.TasksBuilder
import com.kw.common.starter.extension.apioutcome.peekOutputStatus
import com.kw.common.starter.manager.ThreadPoolManager
import com.kw.common.starter.service.api.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Service
class BService(
    private val tasksBuilder: TasksBuilder,
    private val interestTaskRepository: BTaskRepository,
    private val lmsAdapterService: BUpstreamApiService,
    private val appProperties: AppProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun process(bulkJob: BulkJob, coroutineScope: CoroutineScope, isRetry: Boolean) {
        if (!bulkJob.isBEnabled) return

        if (!isRetry) {
            measureTime { tasksBuilder.buildApplyInterestTasks(bulkJob = bulkJob, coroutineScope = coroutineScope) }
                .also { duration ->
                    logger.info(
                        "{} - Build Apply Interest Tasks - Done! in {}m {}s",
                        bulkJob.productCode, duration.inWholeMinutes, duration.inWholeSeconds % 60,
                    )
                }
        }

        measureTime { doApplyInterestTasks(bulkJob = bulkJob, coroutineScope = coroutineScope) }
            .also { duration ->
                logger.info(
                    "Bulk {} - Apply Interest Tasks All Done! in {}h {}m {}s",
                    bulkJob.productCode, duration.inWholeHours, duration.inWholeMinutes % 60, duration.inWholeSeconds % 60,
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun doApplyInterestTasks(bulkJob: BulkJob, coroutineScope: CoroutineScope) {
        logger.info("Bulk {} - Apply Interest Tasks Started!", bulkJob.productCode)

        val batchSize = appProperties.query.batchSize.bulk
        val paging: Pageable = PageRequest.of(0, batchSize)

        val concurrent = appProperties.bulk.concurrent.bTask
        val pool = ThreadPoolManager.initFixedThreadPoolTaskExecutor(
            nThreads = concurrent,
            threadNamePrefix = "Thd-${bulkJob.productCode}-ApplyInterest-",
        )
        val dispatcher = pool.asCoroutineDispatcher()
        val semaphore = Semaphore(concurrent)

        var lastId = 0L
        do {
            val tasks = interestTaskRepository.findByIdGreaterThanAndCompletedFalseAndJobId(
                pageable = paging,
                jobId = bulkJob.id,
                id = lastId,
            )

            measureTime {
                val deferred = tasks.content.map {
                    coroutineScope.async(dispatcher) {
                        semaphore.withPermit { applyInterest(it) }
                    }
                }.awaitAll()

                interestTaskRepository.saveAll(deferred)
            }.also { duration ->
                logger.info(
                    "Bulk {} - Apply Interest - From Id: {}, Size: {} - Done! in {}m {}s",
                    bulkJob.productCode, tasks.firstOrNull()?.id, tasks.numberOfElements,
                    duration.inWholeMinutes, duration.inWholeSeconds % 60,
                )
            }

            if (tasks.numberOfElements < batchSize) break

            lastId = tasks.last().id
        } while (true)

        pool.shutdown()
    }

    private suspend fun applyInterest(task: BTask): BTask {
        val applicationDate = LocalDateTime.now().toString()
        val request = ApplyBRequest(applicationDate)

        val response = lmsAdapterService.applyInterest(task.accountKey, request)
        val (httpStatus, body) = when (response) {
            is ApiResponse.Success,
            is ApiResponse.Failure -> Pair(response.httpStatus, response.body)
            is ApiResponse.Error -> Pair(response.httpStatus, null)
        }

        val statusCode = body?.status?.code ?: httpStatus.value().toString()
        val statusDescription = body?.status?.description ?: httpStatus.reasonPhrase

        val commpleted: Boolean = httpStatus.is2xxSuccessful || when (statusCode) {
            BUpstreamApiService.ApiStatus.Code.LA2000 -> true
            BUpstreamApiService.ApiStatus.Code.LA8100 -> {
                statusDescription.contains(
                    BUpstreamApiService.ApiStatus.Description.ACC_NO_ACCRUED_INT,
                    ignoreCase = true,
                )
            }
            else -> false
        }
        val result = response.peekOutputStatus()

        logger.debug("Bulk Apply Interest - Task Id: {}, Result: {}", task.id, result)

        return task.copy(completed = commpleted, resultCode = statusCode, resultDescription = statusDescription)
    }
}
