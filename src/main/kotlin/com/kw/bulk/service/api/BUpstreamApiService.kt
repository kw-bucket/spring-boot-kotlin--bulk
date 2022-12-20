package com.kw.bulk.service.api

import com.kw.bulk.config.properties.ApiProperties
import com.kw.bulk.dto.upstreamservice1.b.ApplyBRequest
import com.kw.common.starter.constant.Constant
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.service.api.ApiResponse
import com.kw.common.starter.service.api.ApiService
import org.slf4j.MDC
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@Service
class BUpstreamApiService(
    restTemplate: RestTemplate,
    apiProperties: ApiProperties,
) : ApiService(restTemplate) {

    object ApiStatus {
        object Code {
            const val LA2000 = "LA2000"
            const val LA8100 = "LA8100"
            const val LA4040 = "LA4040"
        }

        object Description {
            const val ACC_NO_ACCRUED_INT = "ACCOUNT_HAS_NO_ACCRUED_INTEREST"
            const val INVALID_ACCOUNT_STATE = "INVALID_ACCOUNT_STATE"
        }
    }


    private val upstreamService2 = apiProperties.upstreamService2

    suspend fun applyInterest(accountId: String, body: ApplyBRequest): ApiResponse<ApiOutput<Nothing>> {
        val path = "/v1/accounts/{account_id}/interest"

        val headers = setHeader()
        val uriVariables = mapOf("account_id" to accountId)
        val uriComponents: UriComponents = UriComponentsBuilder
            .fromHttpUrl(upstreamService2.baseUrl + path)
            .uriVariables(uriVariables)
            .build()

        val responseType = object : ParameterizedTypeReference<ApiOutput<Nothing>>() {}

        return execute(HttpMethod.POST, uriComponents, HttpEntity(body, headers), responseType)
    }

    private fun setHeader(): HttpHeaders {
        val suffix = UUID.randomUUID().toString().take(6)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(Constant.Headers.X_ACCESS_KEY, upstreamService2.accessKey)
        headers.set(Constant.Headers.X_CORRELATION_ID, "${MDC.get(Constant.Context.CORRELATION_ID)}-$suffix")

        return headers
    }
}
