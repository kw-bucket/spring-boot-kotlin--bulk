package com.kw.bulk.validation

import com.kw.bulk.dto.bulk.BulkRequest
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Constraint(validatedBy = [BulkProductCodeValidator::class])
annotation class ValidBulkProductCode(
    val message: String = "Invalid or not supported",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class BulkProductCodeValidator : ConstraintValidator<ValidBulkProductCode, String> {
    override fun isValid(value: String, context: ConstraintValidatorContext?): Boolean {
        return BulkRequest.ProductCode.ALL.contains(value)
    }
}

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Constraint(validatedBy = [BulkProcessAValidator::class])
annotation class ValidProcessA(
    val message: String = "Invalid A Process Request",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class BulkProcessAValidator : ConstraintValidator<ValidProcessA, BulkRequest.ProcessA> {
    override fun isValid(bulkProcessA: BulkRequest.ProcessA, context: ConstraintValidatorContext?): Boolean {
        return when (bulkProcessA.isAXEnabled) {
            true -> bulkProcessA.isA1Enabled
            false -> true
        }
    }
}
