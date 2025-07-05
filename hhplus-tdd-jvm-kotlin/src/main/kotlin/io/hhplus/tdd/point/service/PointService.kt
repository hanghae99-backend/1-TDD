package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.UserPoint
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointTable: UserPointTable
) {
    fun getUserPoint(id: Long): UserPoint {
        return userPointTable.selectById(id)
    }
}