package com.kw.bulk.service.report

import com.kw.bulk.config.properties.AppProperties
import com.kw.bulk.entity.bulk.BulkJob
import com.kw.bulk.repository.bulk.ATaskRepository
import com.kw.bulk.repository.bulk.BTaskRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class CSVExporter(
    private val aTaskRepository: ATaskRepository,
    private val bTaskRepository: BTaskRepository,
    appProperties: AppProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val batchSize: Int = appProperties.query.batchSize.bulk

    private val collectionTaskHeaders = arrayOf(
        "Id", "Client Key", "Product Code", "Result Code", "Result Description",
    )

    private val applyInterestHeaders = arrayOf(
        "Id", "Loan Account Encoded Key", "Result Code", "Result Description",
    )

    private val directory = "tmp/"

    suspend fun exportFailedA1Tasks(bulkJob: BulkJob): File {
        val filePath = Paths.get(directory, "A1_Failure_${bulkJob.asOfDate.toLocalDate()}.csv")
        val csvPrinter = initCsvPrinter(filePath = filePath, headers = collectionTaskHeaders)

        csvPrinter.use { printer ->
            var paging: Pageable = PageRequest.of(0, batchSize)
            do {
                val tasks = aTaskRepository.findByProductBucketCompletedFalseAndJobId(paging, bulkJob.id)
                val records = tasks.content.map {
                    listOf(it.id, it.clientKey, it.productCode, it.a1ResultCode, it.a1ResultDescription)
                }

                printer.printRecords(records)

                if (!tasks.hasNext()) break

                paging = tasks.nextPageable()
            } while (true)

            return filePath.toFile().also { logger.info("Report - Failure A1 Tasks- Done!") }
        }
    }

    suspend fun exportFailedA2Tasks(bulkJob: BulkJob): File {
        val filePath = Paths.get(directory, "A1_Failure_${bulkJob.asOfDate.toLocalDate()}.csv")
        val csvPrinter = initCsvPrinter(filePath = filePath, headers = collectionTaskHeaders)

        csvPrinter.use { printer ->
            var paging: Pageable = PageRequest.of(0, batchSize)
            do {
                val tasks = aTaskRepository.findByCollectionFlagCompletedFalseAndJobId(paging, bulkJob.id)
                val records = tasks.content.map {
                    listOf(it.id, it.clientKey, it.productCode, it.a2ResultCode, it.a2ResultDescription)
                }

                printer.printRecords(records)

                if (!tasks.hasNext()) break

                paging = tasks.nextPageable()
            } while (true)

            return filePath.toFile().also { logger.info("Report - Failure A2 Tasks- Done!") }
        }
    }

    suspend fun exportFailedBTasks(bulkJob: BulkJob): File {
        val filePath = Paths.get(directory, "B_Failure_${bulkJob.asOfDate.toLocalDate()}.csv")
        val csvPrinter = initCsvPrinter(filePath = filePath, headers = applyInterestHeaders)

        csvPrinter.use { printer ->
            var paging: Pageable = PageRequest.of(0, batchSize)
            do {
                val tasks = bTaskRepository.findByCompletedFalseAndJobId(pageable = paging, jobId = bulkJob.id)
                val records = tasks.content.map {
                    listOf(it.id, it.accountKey, it.completed, it.resultCode, it.resultDescription)
                }

                printer.printRecords(records)

                if (!tasks.hasNext()) break

                paging = tasks.nextPageable()
            } while (true)

            return filePath.toFile().also { logger.info("Report - Failure B Tasks - Done!") }
        }
    }

    private fun initCsvPrinter(filePath: Path, headers: Array<String>): CSVPrinter {
        if (Files.notExists(filePath.parent)) Files.createDirectories(filePath.parent)

        val writer = File(filePath.toUri()).bufferedWriter()

        return CSVPrinter(
            writer,
            CSVFormat.DEFAULT.builder().apply { setHeader(*headers) }.build(),
        )
    }
}
