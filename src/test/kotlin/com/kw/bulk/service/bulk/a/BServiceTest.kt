package com.kw.bulk.service.bulk.a

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.BTask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.mock.MockApi
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.service.api.BUpstreamApiService
import com.kw.bulk.service.bulk.b.BService
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyArray
import org.springframework.data.domain.SliceImpl
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BServiceTest {

    private val tasksBuilder: TasksBuilder = mockk(relaxed = true)
    private val interestTaskRepository: BTaskRepository = mockk(relaxed = true)
    private val lmsAdapterService: BUpstreamApiService = mockk(relaxed = true)
    private val appProperties: AppProperties = mockk(relaxed = true)

    @InjectMockKs
    lateinit var service: BService

    @AfterEach
    fun afterEach() { clearAllMocks() }

    private fun stubSuccess() {
        coEvery { tasksBuilder.buildApplyInterestTasks(any(), any()) } returns Unit

        coEvery { appProperties.bulk.concurrent.bTask } returns 2
        coEvery { appProperties.query.batchSize.bulk } returns 1

        coEvery {
            interestTaskRepository.findByIdGreaterThanAndCompletedFalseAndJobId(any(), any(), any())
        } answers {
            SliceImpl(listOf(BTask(id = 1, jobId = 1, accountKey = "acc-1")))
        } andThenAnswer {
            SliceImpl(listOf(BTask(id = 2, jobId = 1, accountKey = "acc-2")))
        } andThenAnswer {
            SliceImpl(emptyList())
        }

        coEvery { interestTaskRepository.saveAll(anyArray<BTask>().toList()) } returnsArgument 0

        coEvery { lmsAdapterService.applyInterest(any(), any()) } returns MockApi.BUpstreamApi.applyBOk
    }

    @Nested
    inner class ApplyProcessBDay {
        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `perform new`() = runTest {
            val job = BulkJob(
                id = 1, asOfDate = LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
                triggerSource = "somewhere", productCode = "PAYNEXTEXTRA",
                isBEnabled = true,
            )

            stubSuccess()

            service.process(bulkJob = job, coroutineScope = this, isRetry = false)

            coVerify(exactly = 1) { tasksBuilder.buildApplyInterestTasks(any(), any()) }
            coVerify(exactly = 3) {
                interestTaskRepository.findByIdGreaterThanAndCompletedFalseAndJobId(any(), any(), any())
            }
            coVerify(exactly = 2) { lmsAdapterService.applyInterest(any(), any()) }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `perform retry`() = runTest {
            val job = BulkJob(
                id = 1, asOfDate = LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
                triggerSource = "somewhere", productCode = "PAYNEXTEXTRA",
                isBEnabled = true,
            )

            stubSuccess()

            service.process(bulkJob = job, coroutineScope = this, isRetry = true)

            coVerify(exactly = 0) { tasksBuilder.buildApplyInterestTasks(any(), any()) }
            coVerify(exactly = 3) {
                interestTaskRepository.findByIdGreaterThanAndCompletedFalseAndJobId(any(), any(), any())
            }
            coVerify(exactly = 2) { lmsAdapterService.applyInterest(any(), any()) }
        }
    }

    @Nested
    inner class NotApplyProcessBDay {
        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `perform new or retry`() = runTest {
            val job = BulkJob(id = 1, asOfDate = LocalDate.parse("2022-05-05").atTime(LocalTime.MIN), triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA")

            stubSuccess()

            listOf(true, false).forEach { retry ->
                service.process(bulkJob = job, coroutineScope = this, isRetry = retry)

                coVerify(exactly = 0) { tasksBuilder.buildApplyInterestTasks(any(), any()) }
                coVerify(exactly = 0) {
                    interestTaskRepository.findByIdGreaterThanAndCompletedFalseAndJobId(any(), any(), any())
                }
                coVerify(exactly = 0) { lmsAdapterService.applyInterest(any(), any()) }
            }
        }
    }
}
