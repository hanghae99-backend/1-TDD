package io.hhplus.tdd.point.fixture

import io.hhplus.tdd.point.dto.PointHistory
import io.hhplus.tdd.point.dto.TransactionType

object PointHistoryFixture {
    
    fun create(
        id: Long = 1L,
        userId: Long = 1L,
        type: TransactionType = TransactionType.CHARGE,
        amount: Long = 1000L,
        timeMillis: Long = System.currentTimeMillis()
    ): PointHistory {
        return PointHistory(id, userId, type, amount, timeMillis)
    }
    
    fun createCharge(
        userId: Long = 1L,
        amount: Long = 1000L,
        id: Long = 1L
    ): PointHistory = create(id = id, userId = userId, type = TransactionType.CHARGE, amount = amount)
    
    fun createUse(
        userId: Long = 1L,
        amount: Long = 500L,
        id: Long = 1L
    ): PointHistory = create(id = id, userId = userId, type = TransactionType.USE, amount = amount)
}
