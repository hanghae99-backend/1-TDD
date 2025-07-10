package io.hhplus.tdd.point.integration

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.TransactionType
import io.hhplus.tdd.point.service.PointService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PointIntegrationTest {

    private val userPointTable = UserPointTable()
    private val pointHistoryTable = PointHistoryTable()
    private val pointService = PointService(userPointTable, pointHistoryTable)


    /**
     * 사용자 포인트 조회
     */
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

    /**
     * 포인트 충전 테스트
     */
    @Test
    fun `포인트 충전 성공`() {
        val userId = Random.nextLong(10000, 99999)
        val chargeAmount = 1000L
        
        val result = pointService.chargeUserPoint(userId, chargeAmount)
        
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(chargeAmount)
        assertThat(result.updateMillis).isGreaterThan(0L)
        
        // 히스토리 확인
        val histories = pointService.getUserPointHistory(userId)
        assertThat(histories).hasSize(1)
        assertThat(histories[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories[0].amount).isEqualTo(chargeAmount)
        assertThat(histories[0].userId).isEqualTo(userId)
    }

    @Test
    fun `기존 포인트에 충전 성공`() {
        val userId = Random.nextLong(10000, 99999)
        val initialAmount = 500L
        val chargeAmount = 1000L
        
        // 초기 포인트 설정
        userPointTable.insertOrUpdate(userId, initialAmount)
        
        val result = pointService.chargeUserPoint(userId, chargeAmount)
        
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(initialAmount + chargeAmount)
        
        // 히스토리 확인
        val histories = pointService.getUserPointHistory(userId)
        assertThat(histories).hasSize(1)
        assertThat(histories[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories[0].amount).isEqualTo(chargeAmount)
    }

    /**
     * 포인트 사용 테스트
     */
    @Test
    fun `포인트 사용 성공`() {
        val userId = Random.nextLong(10000, 99999)
        val initialAmount = 1500L
        val useAmount = 500L
        
        // 초기 포인트 설정
        userPointTable.insertOrUpdate(userId, initialAmount)
        
        val result = pointService.useUserPoint(userId, useAmount)
        
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(initialAmount - useAmount)
        assertThat(result.updateMillis).isGreaterThan(0L)
        
        // 히스토리 확인
        val histories = pointService.getUserPointHistory(userId)
        assertThat(histories).hasSize(1)
        assertThat(histories[0].type).isEqualTo(TransactionType.USE)
        assertThat(histories[0].amount).isEqualTo(useAmount)
        assertThat(histories[0].userId).isEqualTo(userId)
    }

    @Test
    fun `포인트 충전 후 사용 시나리오`() {
        val userId = Random.nextLong(10000, 99999)
        val chargeAmount = 2000L
        val useAmount = 800L
        
        // 포인트 충전
        val chargeResult = pointService.chargeUserPoint(userId, chargeAmount)
        assertThat(chargeResult.point).isEqualTo(chargeAmount)
        
        // 포인트 사용
        val useResult = pointService.useUserPoint(userId, useAmount)
        assertThat(useResult.point).isEqualTo(chargeAmount - useAmount)
        
        // 히스토리 확인
        val histories = pointService.getUserPointHistory(userId)
        assertThat(histories).hasSize(2)
        
        // 충전 히스토리
        assertThat(histories[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories[0].amount).isEqualTo(chargeAmount)
        
        // 사용 히스토리
        assertThat(histories[1].type).isEqualTo(TransactionType.USE)
        assertThat(histories[1].amount).isEqualTo(useAmount)
    }

    /**
     * 사용자 포인트 히스토리 조회 테스트
     */
    @Test
    fun `신규 사용자의 포인트 히스토리 조회 - 빈 리스트 반환`() {
        val newUserId = Random.nextLong(10000, 99999)
        
        val result = pointService.getUserPointHistory(newUserId)
        
        assertThat(result).isEmpty()
    }

    @Test
    fun `포인트 충전 및 사용 후 히스토리 조회`() {
        val userId = Random.nextLong(10000, 99999)

        // 포인트 충전
        pointService.chargeUserPoint(userId, 1000L)
        pointService.chargeUserPoint(userId, 500L)

        // 포인트 사용
        pointService.useUserPoint(userId, 300L)

        // 히스토리 조회
        val histories = pointService.getUserPointHistory(userId)

        assertThat(histories).hasSize(3)

        // 시간순으로 정렬되어 있는지 확인 (첫 번째가 가장 오래된 것)
        assertThat(histories[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories[0].amount).isEqualTo(1000L)
        assertThat(histories[0].userId).isEqualTo(userId)

        assertThat(histories[1].type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories[1].amount).isEqualTo(500L)
        assertThat(histories[1].userId).isEqualTo(userId)

        assertThat(histories[2].type).isEqualTo(TransactionType.USE)
        assertThat(histories[2].amount).isEqualTo(300L)
        assertThat(histories[2].userId).isEqualTo(userId)

        // 각 히스토리가 유효한 ID와 시간을 가지는지 확인
        histories.forEach { history ->
            assertThat(history.id).isGreaterThan(0L)
            assertThat(history.timeMillis).isGreaterThan(0L)
        }
    }

    @Test
    fun `여러 사용자의 히스토리가 섞이지 않는지 확인`() {
        val userId1 = Random.nextLong(10000, 99999)
        val userId2 = Random.nextLong(10000, 99999)
        
        // 사용자1의 거래
        pointService.chargeUserPoint(userId1, 1000L)
        pointService.useUserPoint(userId1, 500L)
        
        // 사용자2의 거래
        pointService.chargeUserPoint(userId2, 2000L)
        
        // 각 사용자의 히스토리 조회
        val user1Histories = pointService.getUserPointHistory(userId1)
        val user2Histories = pointService.getUserPointHistory(userId2)
        
        // 사용자1은 2개의 거래 내역
        assertThat(user1Histories).hasSize(2)
        assertThat(user1Histories).allMatch { it.userId == userId1 }
        
        // 사용자2는 1개의 거래 내역
        assertThat(user2Histories).hasSize(1)
        assertThat(user2Histories).allMatch { it.userId == userId2 }
        assertThat(user2Histories[0].amount).isEqualTo(2000L)
        assertThat(user2Histories[0].type).isEqualTo(TransactionType.CHARGE)
    }
}