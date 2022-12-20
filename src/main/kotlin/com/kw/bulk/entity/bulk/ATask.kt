package com.kw.bulk.entity.bulk

import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "a_task")
data class ATask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val jobId: Long,
    val clientKey: String,
    val productCode: String,

    val a1Completed: Boolean = false,
    val a1ResultCode: String? = null,
    val a1ResultDescription: String? = null,

    val a2Completed: Boolean = false,
    val a2ResultCode: String? = null,
    val a2ResultDescription: String? = null,

    @CreatedDate val createdAt: LocalDateTime = LocalDateTime.now(),
    @UpdateTimestamp val updatedAt: LocalDateTime = LocalDateTime.now(),
)
