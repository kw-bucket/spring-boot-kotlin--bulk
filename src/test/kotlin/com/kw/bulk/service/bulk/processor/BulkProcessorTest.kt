package com.kw.bulk.service.bulk.processor

import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.bulk.service.bulk.BulkProcessor
import com.kw.bulk.service.bulk.BulkWatcher
import com.kw.bulk.service.bulk.a.AService
import com.kw.bulk.service.bulk.b.BService
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkProcessorTest {

    private val bulkWatcher: BulkWatcher = mockk(relaxed = true)
    private val jobRepository: JobRepository = mockk(relaxed = true)
    private val collectionService: AService = mockk(relaxed = true)
    private val interestService: BService = mockk(relaxed = true)

    @InjectMockKs
    lateinit var service: BulkProcessor

    @AfterEach
    fun afterEach() { clearAllMocks() }

    private fun stubSuccess() {
        every { jobRepository.save(any()) } returnsArgument 0

        coEvery { collectionService.process(any(), any(), any()) } returns Unit
        coEvery { interestService.process(any(), any(), any()) } returns Unit
        coEvery { bulkWatcher.inspect(any()) } returns Unit
    }

    @Test
    fun `perform new`() {
        val job = BulkJob(id = 1, asOfDate = LocalDateTime.now(), triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA")

        stubSuccess()

        service.run(_bulkJob = job)

        coVerify(exactly = 1) { collectionService.process(any(), any(), isRetry = false) }
        coVerify(exactly = 1) { interestService.process(any(), any(), isRetry = false) }
        coVerify(exactly = 1) { bulkWatcher.inspect(bulkJobId = job.id) }
    }

    @Test
    fun `perform retry`() {
        val job = BulkJob(id = 1, asOfDate = LocalDateTime.now(), triggerSource = "ADHOC", productCode = "PAYNEXTEXTRA")

        stubSuccess()

        service.run(_bulkJob = job, isRetry = true)

        coVerify(exactly = 1) { collectionService.process(any(), any(), isRetry = true) }
        coVerify(exactly = 1) { interestService.process(any(), any(), isRetry = true) }
        coVerify(exactly = 1) { bulkWatcher.inspect(bulkJobId = job.id) }
    }
}
