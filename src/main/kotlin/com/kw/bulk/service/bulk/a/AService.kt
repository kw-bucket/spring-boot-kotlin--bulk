package com.kw.bulk.service.bulk.a

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.dto.upstreamservice2.a1.CalculateA1Request
import com.kw.bulk.dto.upstreamservice2.a2.CalculateA2Request
import com.kw.bulk.entity.bulk.ATask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.service.api.AUpstreamApiService
import com.kw.bulk.service.bulk.task.TasksBuilder
import com.kw.common.starter.extension.apioutcome.peekOutputStatus
import com.kw.common.starter.manager.ThreadPoolManager
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
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Service
class AService(
    private val tasksBuilder: TasksBuilder,
    private val AUpstreamApiService: AUpstreamApiService,
    private val aTaskRepository: ATaskRepository,
    private val appProperties: AppProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun process(bulkJob: BulkJob, coroutineScope: CoroutineScope, isRetry: Boolean) {
        if (!bulkJob.isA1Enabled && !bulkJob.isA2Enabled) return

        if (!isRetry) {
            measureTime { tasksBuilder.buildCollectionTasks(bulkJob = bulkJob, coroutineScope = coroutineScope) }
                .also { duration ->
                    logger.info(
                        "{} - Build Collection Tasks - Done! in {}m {}s",
                        bulkJob.productCode, duration.inWholeMinutes, duration.inWholeSeconds % 60,
                    )
                }
        }

        measureTime { doCollectionTasks(bulkJob = bulkJob, coroutineScope = coroutineScope) }
            .also { duration ->
                logger.info(
                    "Bulk {} - Collection Tasks All Done! in {}h {}m {}s",
                    bulkJob.productCode, duration.inWholeHours, duration.inWholeMinutes % 60, duration.inWholeSeconds % 60,
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun doCollectionTasks(bulkJob: BulkJob, coroutineScope: CoroutineScope) {
        logger.info("Bulk {} - Collection Tasks Started!", bulkJob.productCode)

        val batchSize = appProperties.query.batchSize.bulk
        val page: Pageable = PageRequest.of(0, batchSize)

        val concurrent = appProperties.bulk.concurrent.aTask
        val pool = ThreadPoolManager.initFixedThreadPoolTaskExecutor(
            nThreads = concurrent,
            threadNamePrefix = "Thd-${bulkJob.productCode}-CollectionTask-",
        )
        val dispatcher = pool.asCoroutineDispatcher()
        val semaphore = Semaphore(concurrent)

        val bucketCalculationDate = if (bulkJob.asOfDate.dayOfMonth == 3)
            bulkJob.asOfDate.minusDays(1).toLocalDate()
        else
            bulkJob.asOfDate.toLocalDate()

        var lastId = 0L
        do {
            val tasks = aTaskRepository.findIncompletedTask(pageable = page, jobId = bulkJob.id, id = lastId)

            measureTime {
                val deferred = tasks.content.map { task ->
                    coroutineScope.async(dispatcher) {
                        semaphore.withPermit {
                            calculateProductBucket(
                                isEnabled = bulkJob.isA1Enabled,
                                task = task,
                                date = bucketCalculationDate,
                            ).let { task ->
                                val isEnabled = if (bulkJob.isA1Enabled && bulkJob.isA2Enabled)
                                    task.a1Completed
                                else
                                    bulkJob.isA2Enabled

                                calculateCollectionFlag(
                                    isEnabled = isEnabled,
                                    task = task,
                                )
                            }
                        }
                    }
                }.awaitAll()

                aTaskRepository.saveAll(deferred)
            }.also { duration ->
                logger.info(
                    "Bulk {} - Calculate Product Bucket - From Id: {}, Size: {} - Done! in {}m {}s",
                    bulkJob.productCode, tasks.firstOrNull()?.id, tasks.numberOfElements,
                    duration.inWholeMinutes, duration.inWholeSeconds % 60,
                )
            }

            if (tasks.numberOfElements < batchSize) break

            lastId = tasks.last().id
        } while (true)

        pool.shutdown()
    }

    private suspend fun calculateProductBucket(
        isEnabled: Boolean,
        task: ATask,
        date: LocalDate,
    ): ATask {
        if (!isEnabled || task.a1Completed)
            return task

        val request = CalculateA1Request(
            clientEncodedKey = task.clientKey, productCode = task.productCode,
            currentDate = date,
        )

        val apiResponse = AUpstreamApiService.calculateProductBucket(request)

        val outputStatusCode = apiResponse.body?.status?.code ?: apiResponse.httpStatus.value().toString()
        val outpustStatusDescription = apiResponse.body?.status?.description ?: apiResponse.httpStatus.reasonPhrase

        logger.debug(
            "Bulk - Product Bucket - {}, Task Id: {}, Result: {}",
            task.productCode, task.id, apiResponse.peekOutputStatus(),
        )

        return task.copy(
            a1Completed = apiResponse.httpStatus.is2xxSuccessful,
            a1ResultCode = outputStatusCode,
            a1ResultDescription = outpustStatusDescription,
        )
    }

    private suspend fun calculateCollectionFlag(isEnabled: Boolean = false, task: ATask): ATask {
        if (!isEnabled || task.a1Completed)
            return task

        val request = CalculateA2Request(clientEncodedKey = task.clientKey, productCode = task.productCode)

        val apiResponse = AUpstreamApiService.calculateCollectionFlag(request)

        val outputStatusCode = apiResponse.body?.status?.code ?: apiResponse.httpStatus.value().toString()
        val outpustStatusDescription = apiResponse.body?.status?.description ?: apiResponse.httpStatus.reasonPhrase

        logger.debug(
            "Bulk - Collection Flag - {}, Task Id: {}, Result: {}",
            task.productCode, task.id, apiResponse.peekOutputStatus(),
        )

        return task.copy(
            a1Completed = apiResponse.httpStatus.is2xxSuccessful,
            a1ResultCode = outputStatusCode,
            a1ResultDescription = outpustStatusDescription,
        )
    }
}
