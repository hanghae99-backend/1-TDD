package io.hhplus.tdd.point.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PointServiceIntegrationTest {

    private val userPointTable = UserPointTable()
    private val pointHistoryTable = PointHistoryTable()
    private val pointService = PointService(userPointTable, pointHistoryTable)

    @Test
    fun `존재하지 않는 사용자 조회 시 0포인트로 자동 생성`() {
        val nonExistentUserId = Random.nextLong(10000, 99999)
        
        val result = pointService.getUserPoint(nonExistentUserId)
        
        assertThat(result.id).isEqualTo(nonExistentUserId)
        assertThat(result.point).isEqualTo(0L)
        assertThat(result.updateMillis).isGreaterThan(0L)
    }

    @Test
    fun `기존 사용자 조회 시 저장된 포인트 반환`() {
        val userId = Random.nextLong(10000, 99999)
        val expectedPoint = 1500L
        
        userPointTable.insertOrUpdate(userId, expectedPoint)
        
        val result = pointService.getUserPoint(userId)
        
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(expectedPoint)
    }
}