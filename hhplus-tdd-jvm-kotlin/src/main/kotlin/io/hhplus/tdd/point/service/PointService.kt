package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.PointHistory
import io.hhplus.tdd.point.dto.TransactionType
import io.hhplus.tdd.point.dto.UserPoint
import io.hhplus.tdd.point.util.PointValidator
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable
) {
    
    fun getUserPoint(id: Long): UserPoint {
        PointValidator.validateUserId(id)
        return userPointTable.selectById(id)
    }

    fun getUserPointHistory(id: Long): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(id)
    }

    fun chargeUserPoint(id:Long, amount: Long): UserPoint {
        val currentPoint =  userPointTable.selectById(id).point
        val updatedPoint = currentPoint + amount

        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis())

        return userPointTable.insertOrUpdate(id, updatedPoint)
    }

    fun useUserPoint(id:Long, amount: Long): UserPoint {
        val currentPoint =  userPointTable.selectById(id).point
        val updatedPoint = currentPoint - amount

        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis())

        return userPointTable.insertOrUpdate(id, updatedPoint)
    }
}