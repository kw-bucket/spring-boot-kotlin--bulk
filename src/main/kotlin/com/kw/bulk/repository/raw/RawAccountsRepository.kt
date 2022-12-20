package com.kw.bulk.repository.raw

import com.kw.bulk.entity.raw.RawAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RawAccountsRepository : JpaRepository<RawAccount, Long> {

    @Query(
        value = """
            SELECT
                l.account_holder_key
            FROM loan_accounts l
            JOIN loan_accounts__custom_fields lf
                ON lf._sdc_source_key_id = l.id
                AND lf.field_set_id = '_lendingAccount' AND lf.id = 'lpProductCode' AND lf.value = :productCode
            WHERE
                l.account_state = '${RawAccount.AccountState.activeInArrears}'
            OR (
                l.account_state = '${RawAccount.AccountState.active}'
                AND CAST(l.balances__interest_balance AS DECIMAL) + 
                    CAST(l.balances__interest_from_arrears_balance AS DECIMAL) + 
                    CAST(l.balances__fees_balance AS DECIMAL) > 0
               )
            GROUP BY l.account_holder_key
            LIMIT :size OFFSET :offset
        """,
        nativeQuery = true,
    )
    fun findClientKeys(productCode: String, offset: Int, size: Int): List<String>

    @Query(
        value = """
            SELECT
                l.account_holder_key
            FROM loan_accounts l
            JOIN loan_accounts__custom_fields lf
                ON lf._sdc_source_key_id = l.id
                AND lf.field_set_id = '_lendingAccount' AND lf.id = 'lpProductCode' AND lf.value = :productCode
            WHERE
                l.account_state = :accountState
            GROUP BY l.account_holder_key
            LIMIT :size OFFSET :offset
        """,
        nativeQuery = true,
    )
    fun findClientKeys(productCode: String, accountState: String, offset: Int, size: Int): List<String>

    @Query(
        value = """
            SELECT
                l.account_holder_key
            FROM loan_accounts l
            JOIN loan_accounts__custom_fields lfp
                ON lfp._sdc_source_key_id = l.id 
                AND lfp.field_set_id = '_lendingAccount' AND lfp.id = 'lpProductCode' AND lfp.value = :productCode
            JOIN loan_accounts__custom_fields lfb
                ON lfb._sdc_source_key_id = l.id
                AND lfb.field_set_id = '_lendingAccount' AND lfb.id = 'loanBucket' AND lfb.value = :bucket
            WHERE l.account_state IN :accountStates
            GROUP BY l.account_holder_key
            LIMIT :size OFFSET :offset
        """,
        nativeQuery = true,
    )
    fun findClientKeys(
        productCode: String,
        accountStates: List<String>,
        bucket: String,
        offset: Int,
        size: Int,
    ): List<String>

    @Query(
        value = """
            SELECT
                la.encoded_key
            FROM loan_accounts la
            JOIN loan_accounts__custom_fields lacf
                ON la.id = lacf._sdc_source_key_id
                AND lacf.field_set_id = '_lendingAccount'
                AND lacf.id = 'lpProductCode'
                AND lacf.value = :productCode
            WHERE la.loan_name = :loanName
                AND la.account_state IN :accountStates
            LIMIT :size OFFSET :offset
        """,
        nativeQuery = true,
    )
    fun findRawAccountKeys(
        productCode: String,
        loanName: String,
        accountStates: List<String>,
        offset: Int,
        size: Int,
    ): List<String>
}
