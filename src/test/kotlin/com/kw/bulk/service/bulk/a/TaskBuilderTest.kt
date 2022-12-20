package com.kw.bulk.service.bulk.a

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.ATask
import com.kw.bulk.entity.bulk.BTask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.entity.raw.RawAccount
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.repository.raw.RawAccountsRepository
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
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskBuilderTest {

    private val loanAccountsRepository: RawAccountsRepository = mockk(relaxed = true)
    private val collectionTaskRepository: ATaskRepository = mockk(relaxed = true)
    private val interestTaskRepository: BTaskRepository = mockk(relaxed = true)
    private val appProperties: AppProperties = mockk(relaxed = true)

    @InjectMockKs
    lateinit var service: TasksBuilder

    @AfterEach
    fun afterEach() { clearAllMocks() }

    private fun stubSuccess() {
        coEvery { appProperties.query.batchSize.bulk } returns 1
        coEvery { appProperties.query.batchSize.buildTask } returns 1

        coEvery {
            loanAccountsRepository.findClientKeys(productCode = any(), offset = any(), size = any())
        } answers { listOf("cli@2-1") } andThenAnswer { listOf("cli@2-2") } andThenAnswer { listOf() }

        coEvery {
            loanAccountsRepository.findClientKeys(
                productCode = any(), accountState = any(),
                offset = any(), size = any(),
            )
        } answers { listOf("cli@3-1") } andThenAnswer { listOf("cli@3-2") } andThenAnswer { listOf() }

        coEvery {
            loanAccountsRepository.findClientKeys(
                productCode = any(), accountStates = any(), bucket = "XDAYS",
                offset = any(), size = any(),
            )
        } answers { listOf("cli@5-1") } andThenAnswer { listOf("cli@5-2") } andThenAnswer { listOf() }

        coEvery {
            loanAccountsRepository.findRawAccountKeys(any(), any(), any(), any(), any())
        } answers { listOf("acc-1") } andThenAnswer { listOf("acc-2") } andThenAnswer { listOf() }

        coEvery {
            collectionTaskRepository.saveAll(anyArray<ATask>().toList())
        } returnsArgument 0

        coEvery {
            interestTaskRepository.saveAll(anyArray<BTask>().toList())
        } returnsArgument 0
    }

    @Nested
    inner class BuildATask {
        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `build collection task on day 5`() = runTest {
            val job = BulkJob(
                id = 1, asOfDate = LocalDate.parse("2022-05-05").atTime(LocalTime.MIN),
                triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA",
            )

            stubSuccess()

            service.buildCollectionTasks(bulkJob = job, coroutineScope = this)

            coVerify(exactly = 3) {
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountStates = any(), bucket = "XDAYS",
                    offset = any(), size = any(),
                )
            }
            coVerify(exactly = 0) {
                loanAccountsRepository.findClientKeys(productCode = any(), offset = any(), size = any())
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountState = any(),
                    offset = any(), size = any(),
                )
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `build collection task on day 3`() = runTest {
            val bulkJob = BulkJob(
                id = 1, asOfDate = LocalDate.parse("2022-05-03").atTime(LocalTime.MIN),
                triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA",
            )

            stubSuccess()

            service.buildCollectionTasks(bulkJob = bulkJob, coroutineScope = this)

            coVerify(exactly = 3) {
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountState = RawAccount.AccountState.activeInArrears,
                    offset = any(), size = any(),
                )
            }
            coVerify(exactly = 0) {
                loanAccountsRepository.findClientKeys(productCode = any(), offset = any(), size = any())
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountStates = any(), bucket = any(),
                    offset = any(), size = any(),
                )
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun `build collection task on not day 5,3`() = runTest {
            val bulkJob = BulkJob(
                id = 1, asOfDate = LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
                triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA",
            )

            stubSuccess()

            service.buildCollectionTasks(bulkJob = bulkJob, coroutineScope = this)

            coVerify(exactly = 3) {
                loanAccountsRepository.findClientKeys(productCode = any(), offset = any(), size = any())
            }
            coVerify(exactly = 0) {
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountStates = any(), bucket = any(),
                    offset = any(), size = any(),
                )
                loanAccountsRepository.findClientKeys(
                    productCode = any(), accountState = any(),
                    offset = any(), size = any(),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `build interest tasks`() = runTest {
        val bulkJob = BulkJob(
            id = 1, asOfDate = LocalDate.parse("2022-05-05").atTime(LocalTime.MIN),
            triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA",
        )

        stubSuccess()

        service.buildApplyInterestTasks(bulkJob = bulkJob, coroutineScope = this)

        coVerify(exactly = 3) { loanAccountsRepository.findRawAccountKeys(any(), any(), any(), any(), any()) }
    }
}
