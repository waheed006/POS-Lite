package com.gembyte.poslite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.gembyte.poslite.data.local.entity.CustomerLedgerEntity
import com.gembyte.poslite.data.local.entity.CustomerLedgerItemEntity
import com.gembyte.poslite.data.model.CustomerBalance
import com.gembyte.poslite.data.model.LedgerWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Insert
    suspend fun insertLedger(ledger: CustomerLedgerEntity): Long

    @Insert
    suspend fun insertItems(items: List<CustomerLedgerItemEntity>)

    @Transaction
    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY date DESC")
    fun getCustomerLedger(customerId: Long): Flow<List<LedgerWithItems>>

    @Query(
        """
    SELECT
    c.id as customerId,
    c.name as customerName,
    COALESCE(
        SUM(
            CASE
                WHEN l.type = 'CREDIT'
                THEN l.amount
                WHEN l.type = 'PAYMENT'
                THEN -l.amount
                ELSE 0
            END

        ),

        0
    ) as balance
    FROM customers c
    LEFT JOIN customer_ledger l
    ON c.id = l.customerId
    GROUP BY c.id
    HAVING balance > 0
    ORDER BY c.name
    """
    )
    fun getCustomersWithBalance(): Flow<List<CustomerBalance>>

    @Query(
        """
    SELECT * FROM customer_ledger
    WHERE customerId = :customerId
    ORDER BY date DESC
    """
    )
    suspend fun getCustomerLedgerOnce(
        customerId: Long
    ): List<CustomerLedgerEntity>

    @Query("""
    SELECT
    c.id as customerId,
    c.name as customerName,
    COALESCE(
        SUM(
            CASE
                WHEN l.type='CREDIT'
                THEN l.amount
                ELSE -l.amount
            END
        ),0
    ) as balance
    FROM customers c
    LEFT JOIN customer_ledger l
    ON c.id=l.customerId
    GROUP BY c.id
    HAVING balance > 0
    ORDER BY c.name
    """)
    fun getOutstandingCustomers(): Flow<List<CustomerBalance>>

    @Query(
        """
    SELECT COALESCE(SUM(amount),0)
    FROM customer_ledger
    WHERE type = 'PAYMENT'
    """
    )
    fun getTotalReceivedPayments(): Flow<Double>

    @Query(
        """
    SELECT
    COALESCE(
        SUM(
            CASE
                WHEN type='CREDIT' THEN amount
                WHEN type='PAYMENT' THEN -amount
            END
        ),0
    )
    FROM customer_ledger
    """
    )
    fun getOutstandingReceivable(): Flow<Double>
}