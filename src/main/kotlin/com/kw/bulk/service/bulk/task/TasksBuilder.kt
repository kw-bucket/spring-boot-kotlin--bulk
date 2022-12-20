package com.kw.bulk.service.bulk.task

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.ATask
import com.kw.bulk.entity.bulk.BTask
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.entity.raw.RawAccount
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.repository.bulk.BTaskRepository
import com.kw.bulk.repository.raw.RawAccountsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.stereotype.Service

@Service
class TasksBuilder(
    private val rawAccountsRepository: RawAccountsRepository,
    private val aTaskRepository: ATaskRepository,
    private val bTaskRepository: BTaskRepository,
    private val appProperties: AppProperties,
) {

    private val semaphorePermits: Int = 10
    private val chunkSize: Int = 100

    suspend fun buildCollectionTasks(bulkJob: BulkJob, coroutineScope: CoroutineScope) {
        val semaphore = Semaphore(permits = semaphorePermits)

        val size = appProperties.query.batchSize.buildTask
        var offset = 0

        do {
            val keys: List<String> = when (bulkJob.asOfDate.dayOfMonth) {
                5 -> rawAccountsRepository.findClientKeys(
                    productCode = bulkJob.productCode, accountStates = RawAccount.AccountState.activeStates,
                    bucket = "XDAYS",
                    offset = offset, size = size,
                )
                3 -> rawAccountsRepository.findClientKeys(
                    productCode = bulkJob.productCode, accountState = RawAccount.AccountState.activeInArrears,
                    offset = offset, size = size,
                )
                else -> rawAccountsRepository.findClientKeys(
                    productCode = bulkJob.productCode, offset = offset, size = size,
                )
            }

            keys.chunked(chunkSize).map { _keys ->
                coroutineScope.launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        composeATasks(jobId = bulkJob.id, productCode = bulkJob.productCode, clientKeys = _keys)
                    }
                }
            }.joinAll()

            if (keys.size < size) break

            offset += size
        } while (true)
    }

    suspend fun buildApplyInterestTasks(bulkJob: BulkJob, coroutineScope: CoroutineScope) {
        val semaphore = Semaphore(permits = semaphorePermits)

        val size = appProperties.query.batchSize.buildTask
        var offset = 0

        do {
            val keys: List<String> = rawAccountsRepository.findRawAccountKeys(
                productCode = bulkJob.productCode,
                loanName = RawAccount.Name.byProductCode[bulkJob.productCode].orEmpty(),
                accountStates = RawAccount.AccountState.activeStates,
                offset = offset,
                size = size,
            )

            keys.chunked(chunkSize).map { _keys ->
                coroutineScope.launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        composeBTasks(jobId = bulkJob.id, accountKeys = _keys)
                    }
                }
            }.joinAll()

            if (keys.size < size) break

            offset += size
        } while (true)
    }

    private suspend fun composeATasks(jobId: Long, productCode: String, clientKeys: List<String>) {
        if (clientKeys.isEmpty()) return

        val aTasks: List<ATask> = clientKeys.map {
            ATask(jobId = jobId, clientKey = it, productCode = productCode)
        }

        aTaskRepository.saveAll(aTasks)
    }

    private suspend fun composeBTasks(jobId: Long, accountKeys: List<String>) {
        if (accountKeys.isEmpty()) return

        val bTasks: List<BTask> = accountKeys.map {
            BTask(jobId = jobId, accountKey = it)
        }

        bTaskRepository.saveAll(bTasks)
    }
}
