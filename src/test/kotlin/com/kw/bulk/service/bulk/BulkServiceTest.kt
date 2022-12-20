package com.kw.bulk.service.bulk

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.constant.BulkApiStatus
import com.kw.bulk.dto.bulk.BulkRequest
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.mock.MockAppProperties
import com.kw.bulk.repository.bulk.JobRepository
import com.kw.common.mail.service.notification.EmailNotificationService
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.exception.AppException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.thymeleaf.spring5.SpringTemplateEngine
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkServiceTest {

    private val emailNotificationService: EmailNotificationService = mockk(relaxed = true)
    private val jobRepository: JobRepository = mockk(relaxed = true)
    private val bulkProcessor: BulkProcessor = mockk(relaxed = true)
    private val templateEngine: SpringTemplateEngine = mockk(relaxed = true)
    private val appProperties: AppProperties = mockk(relaxed = true)

    @InjectMockKs
    private lateinit var service: BulkService

    @AfterEach
    fun afterEach() { clearAllMocks() }

    @Nested
    inner class TriggerSourceTest {

        private fun stubHearbeat() {
            every { appProperties.email } returns MockAppProperties.emailConfig
            every { templateEngine.process("heartbeat.html", any()) } returns "email body"
            every { emailNotificationService.sendEmailAsync(any()) } returns Unit
        }

        private fun stubProcessor() {
            every { jobRepository.save(any()) } returnsArgument 0
            every { jobRepository.findFirstByProductCodeOrderByIdDesc(any()) } returns null
            every { bulkProcessor.run(any()) } returns Unit
        }

        @Test
        fun `trigger from mambu webhook on 2nd should process`() {
            stubHearbeat()
            stubProcessor()

            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 2, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
                triggerSource = BulkRequest.TriggerSource.MAMBUWEBHOOK,
            )

            val response = service.process(request)

            verify(exactly = 1) { bulkProcessor.run(_bulkJob = any(), isRetry = false) }

            response.status shouldBe ApiOutput.Status(
                code = BulkApiStatus.BULK2000.code,
                message = BulkApiStatus.BULK2000.message,
                description = BulkApiStatus.BULK2000.description,
            )
        }

        @Test
        fun `trigger from mambu webhook on not-2nd should not process`() {
            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 3, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
                triggerSource = BulkRequest.TriggerSource.MAMBUWEBHOOK,
            )

            val exception = shouldThrow<AppException> { service.process(request) }

            exception.apiOutputStatus shouldBe BulkApiStatus.BULK4000
            exception.description shouldBe "MAMBUWEBHOOK cannot trigger on day 3"
        }

        @Test
        fun `trigger from jenkins on 5th should process`() {
            stubHearbeat()
            stubProcessor()

            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 5, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
                triggerSource = BulkRequest.TriggerSource.JENKINS,
            )

            val response = service.process(request)

            verify(exactly = 1) { bulkProcessor.run(_bulkJob = any(), isRetry = false) }

            response.status shouldBe ApiOutput.Status(
                code = BulkApiStatus.BULK2000.code,
                message = BulkApiStatus.BULK2000.message,
                description = BulkApiStatus.BULK2000.description,
            )
        }

        @Test
        fun `trigger from jenkins on not-5th should not process`() {
            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 6, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
                triggerSource = BulkRequest.TriggerSource.JENKINS,
            )

            val exception = shouldThrow<AppException> { service.process(request) }

            exception.apiOutputStatus shouldBe BulkApiStatus.BULK4000
            exception.description shouldBe "JENKINS cannot trigger on day 6"
        }

        @Test
        fun `trigger from unknown source should not process`() {
            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 1, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
                triggerSource = "UP-TO-ME",
            )

            val exception = shouldThrow<AppException> { service.process(request) }

            exception.apiOutputStatus shouldBe BulkApiStatus.BULK4000
            exception.description shouldBe "UP-TO-ME cannot trigger on day 1"
        }

        @Test
        fun `ad-hoc trigger should process`() {
            stubHearbeat()
            stubProcessor()

            val request = BulkRequest(
                asOfDateTime = LocalDateTime.of(2022, 8, 1, 0, 0),
                productCode = BulkRequest.ProductCode.ALL.random(),
            )

            val response = service.process(request)

            response.status shouldBe ApiOutput.Status(
                code = BulkApiStatus.BULK2000.code,
                message = BulkApiStatus.BULK2000.message,
                description = BulkApiStatus.BULK2000.description,
            )
        }
    }

    @Nested
    inner class InitiateBulkJobTest {

        private val today = LocalDateTime.now()

        private fun stubLatestJob(date: LocalDateTime, status: String = BulkJob.Status.SUCCESS) {
            every {
                jobRepository.findFirstByProductCodeOrderByIdDesc(productCode = any())
            } returns BulkJob(id = 1, asOfDate = date, status = status, triggerSource = "Bulk", productCode = "PAYNEXTEXTRA")
        }

        @Test
        fun `request date is less than or equal latest job date should reject`() {
            stubLatestJob(date = today)

            val request = BulkRequest(asOfDateTime = today.minusDays(1), productCode = "PAYNEXTEXTRA")

            val exception = shouldThrow<AppException> { service.process(request) }

            exception.apiOutputStatus shouldBe BulkApiStatus.BULK4090
            exception.description shouldBe "Invalid request date or latest job is pending"
        }

        @Test
        fun `latest job is still pending should reject`() {
            stubLatestJob(date = today.minusDays(3), status = BulkJob.Status.PENDING)

            val request = BulkRequest(asOfDateTime = today.minusDays(1), productCode = "PAYNEXTEXTRA")

            val exception = shouldThrow<AppException> { service.process(request) }

            exception.apiOutputStatus shouldBe BulkApiStatus.BULK4090
            exception.description shouldBe "Invalid request date or latest job is pending"
        }
    }
}
