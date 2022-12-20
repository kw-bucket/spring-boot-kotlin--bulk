package com.kw.bulk.repository.bulk

import com.kw.bulk.entity.bulk.ATask
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ATaskRepository : JpaRepository<ATask, Long> {

    fun findByProductBucketCompletedFalseAndJobId(pageable: Pageable, jobId: Long): Slice<ATask>

    fun findByCollectionFlagCompletedFalseAndJobId(pageable: Pageable, jobId: Long): Slice<ATask>

    @Query(
        value = """
            SELECT ct.* FROM collection_task ct JOIN job j ON ct.job_id = j.id 
            WHERE ct.id > :id
            AND ct.job_id = :jobId
            AND (ct.product_bucket_completed = 0 OR (ct.collection_flag_completed = 0 AND j.is_collection_flag_enabled = 1))
        """,
        nativeQuery = true,
    )
    fun findIncompletedTask(pageable: Pageable, jobId: Long, id: Long): Slice<ATask>

    fun countByJobId(jobId: Long): Int

    fun countByProductBucketCompletedAndJobId(completed: Boolean, jobId: Long): Int

    fun countByCollectionFlagCompletedAndJobId(completed: Boolean, jobId: Long): Int

    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM collection_task WHERE job_id IN :jobIds",
        nativeQuery = true,
    )
    fun deleteByJobIds(jobIds: List<Long>)
}
