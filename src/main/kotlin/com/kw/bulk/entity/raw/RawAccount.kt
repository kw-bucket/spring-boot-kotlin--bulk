package com.kw.bulk.entity.raw

import com.kw.bulk.dto.bulk.BulkRequest
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "account")
data class RawAccount(

    @Id
    val encodedKey: String
) {
    object AccountState {
        const val active = "ACTIVE"
        const val activeInArrears = "ACTIVE_IN_ARREARS"

        val activeStates = listOf("ACTIVE", "ACTIVE_IN_ARREARS")
    }

    object Name {
        const val PAYNEXT_TL = "Pay Next Termloan"
        const val PAYNEXTEXTRA_TL = "Pay Next Extra Termloan"

        val byProductCode = mapOf(
            BulkRequest.ProductCode.PAYLATER to Name.PAYNEXT_TL,
            BulkRequest.ProductCode.PAYNEXTEXTRA to Name.PAYNEXTEXTRA_TL,
        )
    }
}
