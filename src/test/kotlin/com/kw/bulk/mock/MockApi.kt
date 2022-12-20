package com.kw.bulk.mock

import com.kw.bulk.dto.upstreamservice2.a1.CalculateA1OutputData
import com.kw.common.starter.dto.ApiOutput
import com.kw.common.starter.extension.string.asApiResponseSuccess
import com.kw.common.starter.extension.string.asResource

object MockApi {

    object AUpstreamApi {
        val calculateA1Ok = "/a-upstream-api/calculate-a1-ok.json".asResource()
            .asApiResponseSuccess<ApiOutput<CalculateA1OutputData>>()

        val calculateA2Ok = "/a-upstream-api/calculate-a2-ok.json".asResource()
            .asApiResponseSuccess<ApiOutput<Nothing>>()
    }

    object BUpstreamApi {
        val applyBOk = "/a-upstream-api/apply-b-ok.json".asResource()
            .asApiResponseSuccess<ApiOutput<Nothing>>()
    }
}
