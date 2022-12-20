package com.kw.bulk.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class ControllerTestBase {
    @Autowired lateinit var mockMvc: MockMvc

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
}
