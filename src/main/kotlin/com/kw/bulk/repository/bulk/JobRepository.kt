package com.kw.bulk.repository.bulk

import com.kw.bulk.entity.bulk.BulkJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface JobRepository : JpaRepository<BulkJob, Long> {

    fun findTop15ByOrderByIdDesc(): List<BulkJob>

    fun findFirstByProductCodeOrderByIdDesc(productCode: String): BulkJob?

    fun findByProductCodeAndAsOfDateBefore(productCode: String, dateTime: LocalDateTime): List<BulkJob>

    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM job WHERE id IN :ids",
        nativeQuery = true
    )
    fun deleteByIdIn(ids: List<Long>)
}
