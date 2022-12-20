package com.kw.bulk.config.threadpool

import com.kw.common.starter.manager.ThreadPoolManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class ThreadPoolConfig {

    @Bean("BulkThreadPoolTaskExecutor")
    fun bulkThreadPoolTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolManager.initFixedThreadPoolTaskExecutor(
            nThreads = 20,
            capacity = 1000,
            threadNamePrefix = "Thd-Async-",
        )
}
