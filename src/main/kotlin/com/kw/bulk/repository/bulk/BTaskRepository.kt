package com.kw.bulk.repository.bulk

import com.kw.bulk.entity.bulk.BTask
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface BTaskRepository : JpaRepository<BTask, Long> {

    fun findByCompletedFalseAndJobId(pageable: Pageable, jobId: Long): Slice<BTask>

    fun findByIdGreaterThanAndCompletedFalseAndJobId(
        pageable: Pageable,
        id: Long,
        jobId: Long,
    ): Slice<BTask>

    fun countByJobId(jobId: Long): Int

    fun countByJobIdAndCompleted(jobId: Long, completed: Boolean): Int

    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM interest_task WHERE job_id IN :jobIds",
        nativeQuery = true
    )
    fun deleteByJobIds(jobIds: List<Long>)
}
