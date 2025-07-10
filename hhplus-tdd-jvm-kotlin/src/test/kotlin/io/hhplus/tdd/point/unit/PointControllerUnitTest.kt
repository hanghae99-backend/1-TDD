package io.hhplus.tdd.point.unit

import io.hhplus.tdd.point.controller.PointController
import io.hhplus.tdd.point.dto.PointHistory
import io.hhplus.tdd.point.dto.TransactionType
import io.hhplus.tdd.point.dto.UserPoint
import io.hhplus.tdd.point.service.PointService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PointControllerUnitTest {

    private val pointService = mockk<PointService>()
    private val pointController = PointController(pointService)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    /**
     * 사용자 포인트 조회
     */
    @Test
    fun `존재하는 사용자의 포인트 조회 성공`() {
        // given
        val userId = 1L
        val expectedUserPoint = UserPoint(
            id = userId,
            point = 1000L,
            updateMillis = System.currentTimeMillis()
        )
        
        every { pointService.getUserPoint(userId) } returns expectedUserPoint

        // when
        val result = pointController.point(userId)

        // then
        assertThat(result).isEqualTo(expectedUserPoint)
        verify(exactly = 1) { pointService.getUserPoint(userId) }
    }

    @Test
    fun `존재하지 않는 사용자의 포인트 조회시 새 사용자 생성 후 반환`() {
        // given
        val userId = 999L
        val newUserPoint = UserPoint(
            id = userId,
            point = 0L,
            updateMillis = System.currentTimeMillis()
        )
        
        every { pointService.getUserPoint(userId) } returns newUserPoint

        // when
        val result = pointController.point(userId)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(0L)
        verify(exactly = 1) { pointService.getUserPoint(userId) }
    }

    @Test
    fun `유효하지 않은 사용자 ID로 조회시 예외 발생`() {
        // given
        val invalidUserId = -1L
        val expectedMessage = "유효하지 않은 사용자 ID입니다."
        
        every { pointService.getUserPoint(invalidUserId) } throws IllegalArgumentException(expectedMessage)

        // when & then
        assertThatThrownBy { pointController.point(invalidUserId) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expectedMessage)
        
        verify(exactly = 1) { pointService.getUserPoint(invalidUserId) }
    }

    /**
     * 사용자 포인트 내역 조회
     */
    @Test
    fun `존재하는 사용자의 포인트 내역 조회 성공`() {
        // given
        val userId = 1L
        val expectedHistories = listOf(
            PointHistory(
                id = 1L,
                userId = userId,
                type = TransactionType.CHARGE,
                amount = 1000L,
                timeMillis = System.currentTimeMillis()
            ),
            PointHistory(
                id = 2L,
                userId = userId,
                type = TransactionType.USE,
                amount = 500L,
                timeMillis = System.currentTimeMillis()
            )
        )
        
        every { pointService.getUserPointHistory(userId) } returns expectedHistories

        // when
        val result = pointController.history(userId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(result[1].type).isEqualTo(TransactionType.USE)
        assertThat(result[0].amount).isEqualTo(1000L)
        assertThat(result[1].amount).isEqualTo(500L)
        verify(exactly = 1) { pointService.getUserPointHistory(userId) }
    }

    @Test
    fun `존재하지 않는 사용자의 포인트 내역 조회시 빈 리스트 반환`() {
        // given
        val userId = 999L
        val emptyHistories = emptyList<PointHistory>()
        
        every { pointService.getUserPointHistory(userId) } returns emptyHistories

        // when
        val result = pointController.history(userId)

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { pointService.getUserPointHistory(userId) }
    }

    /**
     * 사용자 포인트 충전
     */
    @Test
    fun `유효한 금액으로 포인트 충전 성공`() {
        // given
        val userId = 1L
        val chargeAmount = 1000L
        val expectedUserPoint = UserPoint(
            id = userId,
            point = 1000L,
            updateMillis = System.currentTimeMillis()
        )
        
        every { pointService.chargeUserPoint(userId, chargeAmount) } returns expectedUserPoint

        // when
        val result = pointController.charge(userId, chargeAmount)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(1000L)
        verify(exactly = 1) { pointService.chargeUserPoint(userId, chargeAmount) }
    }

    @Test
    fun `음수 금액으로 포인트 충전시 예외 발생`() {
        // given
        val userId = 1L
        val invalidAmount = -1000L
        val expectedMessage = "충전 금액은 0보다 커야 합니다."
        
        every { pointService.chargeUserPoint(userId, invalidAmount) } throws IllegalArgumentException(expectedMessage)

        // when & then
        assertThatThrownBy { pointController.charge(userId, invalidAmount) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expectedMessage)
        
        verify(exactly = 1) { pointService.chargeUserPoint(userId, invalidAmount) }
    }

    @Test
    fun `0원으로 포인트 충전시 예외 발생`() {
        // given
        val userId = 1L
        val zeroAmount = 0L
        val expectedMessage = "충전 금액은 0보다 커야 합니다."
        
        every { pointService.chargeUserPoint(userId, zeroAmount) } throws IllegalArgumentException(expectedMessage)

        // when & then
        assertThatThrownBy { pointController.charge(userId, zeroAmount) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expectedMessage)
        
        verify(exactly = 1) { pointService.chargeUserPoint(userId, zeroAmount) }
    }

    /**
     * 사용자 포인트 사용
     */
    @Test
    fun `유효한 금액으로 포인트 사용 성공`() {
        // given
        val userId = 1L
        val useAmount = 500L
        val expectedUserPoint = UserPoint(
            id = userId,
            point = 500L,
            updateMillis = System.currentTimeMillis()
        )
        
        every { pointService.useUserPoint(userId, useAmount) } returns expectedUserPoint

        // when
        val result = pointController.use(userId, useAmount)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(500L)
        verify(exactly = 1) { pointService.useUserPoint(userId, useAmount) }
    }

    @Test
    fun `음수 금액으로 포인트 사용시 예외 발생`() {
        // given
        val userId = 1L
        val invalidAmount = -500L
        val expectedMessage = "사용 금액은 0보다 커야 합니다."
        
        every { pointService.useUserPoint(userId, invalidAmount) } throws IllegalArgumentException(expectedMessage)

        // when & then
        assertThatThrownBy { pointController.use(userId, invalidAmount) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage(expectedMessage)
        
        verify(exactly = 1) { pointService.useUserPoint(userId, invalidAmount) }
    }

    @Test
    fun `보유 포인트보다 많은 금액 사용시 예외 발생`() {
        // given
        val userId = 1L
        val excessiveAmount = 10000L
        val expectedMessage = "보유 포인트가 부족합니다."
        
        every { pointService.useUserPoint(userId, excessiveAmount) } throws IllegalStateException(expectedMessage)

        // when & then
        assertThatThrownBy { pointController.use(userId, excessiveAmount) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage(expectedMessage)
        
        verify(exactly = 1) { pointService.useUserPoint(userId, excessiveAmount) }
    }
}
