package com.kw.bulk.controller

import com.kw.bulk.constant.BulkApiStatus
import com.kw.bulk.dto.bulk.BulkOutputData
import com.kw.bulk.dto.bulk.BulkRequest
import com.kw.bulk.dto.bulk.BulkRerunRequest
import com.kw.bulk.service.bulk.BulkService
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.exception.AppException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.time.LocalDate

@WebMvcTest(BulkController::class)
class BulkControllerTest : ControllerTestBase() {

    @MockkBean lateinit var bulkService: BulkService

    @Nested inner class Run {
        private val endpoint = "/v1/bulk/run"

        @Test fun `bad request when invalid request`() {
            val input = """
                {
                    "current_date": 12
                }
            """.trimIndent()

            mockMvc.post(endpoint) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(input)
            }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.status.code") { value("BULK4000") }
                jsonPath("$.status.message") { value("Bad Request") }
                jsonPath("$.status.description") { value("Invalid Request Format") }
            }
        }

        @Test fun `when service throws exception`() {
            val input = BulkRequest(
                asOfDateTime = LocalDate.now().atTime((0..1).random(), (0..59).random()),
                productCode = BulkRequest.ProductCode.ALL.random(),
                processA = BulkRequest.ProcessA(),
                processB = BulkRequest.ProcessB(),
            )
            val expectedAppStatus = BulkApiStatus.BULK4090

            every {
                bulkService.process(input)
            } throws AppException(HttpStatus.CONFLICT, expectedAppStatus)

            mockMvc.post(endpoint) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(input)
            }.andExpect {
                status { isConflict() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.status.code") { value(expectedAppStatus.code) }
                jsonPath("$.status.message") { value(expectedAppStatus.message) }
                jsonPath("$.status.description") { value(expectedAppStatus.description) }
            }
        }

        @Test fun `ok when trigger successfully`() {
            val input = BulkRequest(
                asOfDateTime = LocalDate.now().atTime((0..1).random(), (0..59).random()),
                productCode = BulkRequest.ProductCode.ALL.random(),
                processA = BulkRequest.ProcessA(),
                processB = BulkRequest.ProcessB(),
            )
            val expectedOutput: ApiOutput<BulkOutputData> = ApiOutput
                .fromStatus(BulkApiStatus.BULK2000, data = BulkOutputData(999))

            every { bulkService.process(input) } returns expectedOutput

            mockMvc.post(endpoint) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(input)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(mapper.writeValueAsString(expectedOutput)) }
            }
        }
    }

    @Nested inner class Rerun {
        private val endpoint = "/v1/bulk/rerun"

        @Test fun `when service throws exception`() {
            val input = BulkRerunRequest(productCode = BulkRequest.ProductCode.ALL.random())
            val expectedAppStatus = BulkApiStatus.BULK4090

            every { bulkService.rerun(input) } throws AppException(HttpStatus.CONFLICT, expectedAppStatus)

            mockMvc.post(endpoint) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(input)
            }.andExpect {
                status { isConflict() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.status.code") { value(expectedAppStatus.code) }
                jsonPath("$.status.message") { value(expectedAppStatus.message) }
                jsonPath("$.status.description") { value(expectedAppStatus.description) }
            }
        }

        @Test fun `ok when rerun successfully`() {
            val input = BulkRerunRequest(productCode = BulkRequest.ProductCode.ALL.random(), fresh = true)
            val expectedOutput: ApiOutput<BulkOutputData> = ApiOutput
                .fromStatus(BulkApiStatus.BULK2000, data = BulkOutputData(999))

            every { bulkService.rerun(input) } returns expectedOutput

            mockMvc.post(endpoint) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(input)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(mapper.writeValueAsString(expectedOutput)) }
            }
        }
    }
}
