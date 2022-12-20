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
@Table(name = "b_task")
data class BTask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val jobId: Long,
    val accountKey: String,
    val completed: Boolean = false,
    val resultCode: String? = null,
    val resultDescription: String? = null,

    @CreatedDate val createdAt: LocalDateTime = LocalDateTime.now(),
    @UpdateTimestamp val updatedAt: LocalDateTime = LocalDateTime.now(),
)
