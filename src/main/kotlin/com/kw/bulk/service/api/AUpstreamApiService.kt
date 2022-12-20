package com.kw.bulk.service.api

import com.kw.bulk.config.properties.ApiProperties
import com.kw.bulk.dto.upstreamservice2.a1.CalculateA1OutputData
import com.kw.bulk.dto.upstreamservice2.a1.CalculateA1Request
import com.kw.bulk.dto.upstreamservice2.a2.CalculateA2Request
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
class AUpstreamApiService(
    restTemplate: RestTemplate,
    apiProperties: ApiProperties,
) : ApiService(restTemplate) {

    private val upstreamService1 = apiProperties.upstreamService1

    suspend fun calculateProductBucket(request: CalculateA1Request): ApiResponse<ApiOutput<CalculateA1OutputData>> {
        val path = "/v2/bucket/calculator"

        val headers = setHeader()
        val uriComponents: UriComponents = UriComponentsBuilder
            .fromHttpUrl(upstreamService1.baseUrl + path)
            .build()

        val responseType = object : ParameterizedTypeReference<ApiOutput<CalculateA1OutputData>>() {}

        return execute(HttpMethod.POST, uriComponents, HttpEntity(request, headers), responseType)
    }

    suspend fun calculateCollectionFlag(request: CalculateA2Request): ApiResponse<ApiOutput<Nothing>> {
        val path = "/v1/collection-service-flag"

        val headers = setHeader()
        val uriComponents: UriComponents = UriComponentsBuilder
            .fromHttpUrl(upstreamService1.baseUrl + path)
            .build()

        val responseType = object : ParameterizedTypeReference<ApiOutput<Nothing>>() {}

        return execute(HttpMethod.POST, uriComponents, HttpEntity(request, headers), responseType)
    }

    private fun setHeader(): HttpHeaders {
        val suffix = UUID.randomUUID().toString().take(6)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(Constant.Headers.X_ACCESS_KEY, upstreamService1.accessKey)
        headers.set(Constant.Headers.X_CORRELATION_ID, "${MDC.get(Constant.Context.CORRELATION_ID)}-$suffix")

        return headers
    }
}
