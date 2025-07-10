package io.hhplus.tdd.point.fixture

import io.hhplus.tdd.point.dto.UserPoint

object UserPointFixture {
    
    fun create(
        id: Long = 1L,
        point: Long = 1000L,
        updateMillis: Long = System.currentTimeMillis()
    ): UserPoint {
        return UserPoint(id, point, updateMillis)
    }
    
    fun createEmpty(id: Long = 1L): UserPoint = create(id = id, point = 0L)
}
