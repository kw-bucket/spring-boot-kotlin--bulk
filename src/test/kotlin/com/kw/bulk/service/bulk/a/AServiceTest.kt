package com.kw.bulk.service.bulk.a

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.ATask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.mock.MockApi
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.service.api.AUpstreamApiService
import com.kw.bulk.service.bulk.task.TasksBuilder
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyArray
import org.springframework.data.domain.SliceImpl
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AServiceTest {

    private val tasksBuilder: TasksBuilder = mockk(relaxed = true)
    private val collectionApiService: AUpstreamApiService = mockk(relaxed = true)
    private val collectionTaskRepository: ATaskRepository = mockk(relaxed = true)
    private val appProperties: AppProperties = mockk(relaxed = true)

    @InjectMockKs
    lateinit var service: AService

    @AfterEach
    fun afterEach() { clearAllMocks() }

    private fun stubSuccess() {
        coEvery { tasksBuilder.buildCollectionTasks(any(), any()) } returns Unit

        coEvery { appProperties.bulk.concurrent.aTask } returns 2
        coEvery { appProperties.query.batchSize.bulk } returns 1

        coEvery {
            collectionTaskRepository.findIncompletedTask(any(), any(), any())
        } answers {
            SliceImpl(listOf(ATask(id = 1, jobId = 1, clientKey = "cli-1", productCode = "p")))
        } andThenAnswer {
            SliceImpl(listOf(ATask(id = 2, jobId = 1, clientKey = "cli-2", productCode = "p")))
        } andThenAnswer {
            SliceImpl(emptyList())
        }

        coEvery {
            collectionTaskRepository.saveAll(anyArray<ATask>().toList())
        } returnsArgument 0

        coEvery {
            collectionApiService.calculateProductBucket(request = any())
        } returns MockApi.AUpstreamApi.calculateA1Ok

        coEvery {
            collectionApiService.calculateCollectionFlag(request = any())
        } returns MockApi.AUpstreamApi.calculateA2Ok
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `perform new`() = runTest {
        val job = BulkJob(
            id = 1, asOfDate = LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
            triggerSource = "somewhere", productCode = "PAYNEXTEXTRA",
            isA1Enabled = true, isA2Enabled = true,
        )

        stubSuccess()

        service.process(bulkJob = job, coroutineScope = this, isRetry = false)

        coVerify(exactly = 1) { tasksBuilder.buildCollectionTasks(any(), any()) }
        coVerify(exactly = 3) { collectionTaskRepository.findIncompletedTask(any(), any(), any()) }
        coVerify(exactly = 2) { collectionApiService.calculateProductBucket(any()) }
        coVerify(exactly = 2) { collectionApiService.calculateCollectionFlag(any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `perform retry`() = runTest {
        val job = BulkJob(
            id = 1, asOfDate = LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
            triggerSource = "somewhere", productCode = "PAYNEXTEXTRA",
            isA1Enabled = true, isA2Enabled = true,
        )

        stubSuccess()

        service.process(bulkJob = job, coroutineScope = this, isRetry = true)

        coVerify(exactly = 0) { tasksBuilder.buildCollectionTasks(any(), any()) }
        coVerify(exactly = 3) { collectionTaskRepository.findIncompletedTask(any(), any(), any()) }
        coVerify(exactly = 2) { collectionApiService.calculateProductBucket(any()) }
        coVerify(exactly = 2) { collectionApiService.calculateCollectionFlag(any()) }
    }
}
