package io.hhplus.tdd.point.unit

import io.hhplus.tdd.InvalidUserIdException
import io.hhplus.tdd.InsufficientPointException
import io.hhplus.tdd.InvalidAmountException
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.PointHistory
import io.hhplus.tdd.point.dto.TransactionType
import io.hhplus.tdd.point.dto.UserPoint
import io.hhplus.tdd.point.service.PointService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import io.mockk.*

class PointUnitTest {

    private val userPointTable = mockk<UserPointTable>()
    private val pointHistoryTable = mockk<PointHistoryTable>()
    private val pointService = PointService(userPointTable, pointHistoryTable)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    /**
     * 사용자 포인트 조회
     */
    @Test
    fun `존재하는 사용자의 포인트 조회 성공`() {
        val expectedUserPoint = UserPoint(id = 1L, point = 1000L, updateMillis = 1234567890L)

        every { userPointTable.selectById(1L) } returns expectedUserPoint

        val result = pointService.getUserPoint(1L)

        assertThat(result).isEqualTo(expectedUserPoint)
        verify(exactly = 1) { userPointTable.selectById(1L) }
    }

    @Test
    fun `존재하지 않는 사용자 조회 시 0포인트로 자동 생성`() {
        val expectedUserPoint = UserPoint(id = 999L, point = 0L, updateMillis = 1234567890L)

        every { userPointTable.selectById(999L) } returns expectedUserPoint

        val result = pointService.getUserPoint(999L)

        assertThat(result).isEqualTo(expectedUserPoint)
        verify(exactly = 1) { userPointTable.selectById(999L) }
    }

    @Test
    fun `음수 사용자 ID로 조회 시 InvalidUserIdException 발생`() {
        val invalidUserId = -1L

        assertThatThrownBy { pointService.getUserPoint(invalidUserId) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")
        
        verify(exactly = 0) { userPointTable.selectById(any()) }
    }

    /**
     * 포인트 충전 테스트
     */
    @Test
    fun `포인트 충전 성공`() {
        val userId = 1L
        val chargeAmount = 1000L
        val currentUserPoint = UserPoint(id = userId, point = 500L, updateMillis = System.currentTimeMillis())
        val expectedUserPoint = UserPoint(id = userId, point = 1500L, updateMillis = System.currentTimeMillis())
        val expectedHistory = PointHistory(id = 1L, userId = userId, type = TransactionType.CHARGE, amount = chargeAmount, timeMillis = System.currentTimeMillis())

        every { userPointTable.selectById(userId) } returns currentUserPoint
        every { userPointTable.insertOrUpdate(userId, 1500L) } returns expectedUserPoint
        every { pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = any()
        ) } returns expectedHistory

        val result = pointService.chargeUserPoint(userId, chargeAmount)

        assertThat(result.point).isEqualTo(1500L)
        verify(exactly = 1) { userPointTable.selectById(userId) }
        verify(exactly = 1) { userPointTable.insertOrUpdate(userId, 1500L) }
        verify(exactly = 1) { pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = any()
        ) }
    }

    @Test
    fun `포인트 충전 시 0이하 금액으로 예외 발생`() {
        val userId = 1L
        val invalidAmountZero = 0L
        val invalidAmountNegative = -100L

        // 0원으로 충전 시도
        assertThatThrownBy { pointService.chargeUserPoint(userId, invalidAmountZero) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 충전 금액(0)보다 작은 금액이 유효하지 않습니다")

        // 음수로 충전 시도
        assertThatThrownBy { pointService.chargeUserPoint(userId, invalidAmountNegative) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 충전 금액(0)보다 작은 금액이 유효하지 않습니다")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.CHARGE, any()) }
    }

    @Test
    fun `포인트 충전 시 음수 사용자 ID로 예외 발생`() {
        val invalidUserId = -1L
        val chargeAmount = 1000L

        assertThatThrownBy { pointService.chargeUserPoint(invalidUserId, chargeAmount) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.CHARGE, any()) }
    }

    /**
     * 포인트 사용 테스트
     */
    @Test
    fun `포인트 사용 성공`() {
        val userId = 1L
        val useAmount = 300L
        val currentUserPoint = UserPoint(id = userId, point = 1000L, updateMillis = System.currentTimeMillis())
        val expectedUserPoint = UserPoint(id = userId, point = 700L, updateMillis = System.currentTimeMillis())
        val expectedHistory = PointHistory(id = 1L, userId = userId, type = TransactionType.USE, amount = useAmount, timeMillis = System.currentTimeMillis())

        every { userPointTable.selectById(userId) } returns currentUserPoint
        every { userPointTable.insertOrUpdate(userId, 700L) } returns expectedUserPoint
        every { pointHistoryTable.insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = any()
        ) } returns expectedHistory

        val result = pointService.useUserPoint(userId, useAmount)

        assertThat(result.point).isEqualTo(700L)
        verify(exactly = 1) { userPointTable.selectById(userId) }
        verify(exactly = 1) { userPointTable.insertOrUpdate(userId, 700L) }
        verify(exactly = 1) { pointHistoryTable.insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = any()
        ) }
    }

    @Test
    fun `포인트 사용 시 잔액 부족으로 예외 발생`() {
        val userId = 1L
        val useAmount = 1500L
        val currentUserPoint = UserPoint(id = userId, point = 1000L, updateMillis = System.currentTimeMillis())

        every { userPointTable.selectById(userId) } returns currentUserPoint

        assertThatThrownBy { pointService.useUserPoint(userId, useAmount) }
            .isInstanceOf(InsufficientPointException::class.java)
            .hasMessageContaining("잔액이 부족합니다. 현재 포인트: 1000, 사용 요청: 1500")

        verify(exactly = 1) { userPointTable.selectById(userId) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    @Test
    fun `포인트 사용 시 0이하 금액으로 예외 발생`() {
        val userId = 1L
        val invalidAmountZero = 0L
        val invalidAmountNegative = -100L

        // 0원으로 사용 시도
        assertThatThrownBy { pointService.useUserPoint(userId, invalidAmountZero) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 사용 금액(0)보다 작은 금액이 유효하지 않습니다")

        // 음수로 사용 시도
        assertThatThrownBy { pointService.useUserPoint(userId, invalidAmountNegative) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 사용 금액(0)보다 작은 금액이 유효하지 않습니다")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    @Test
    fun `포인트 사용 시 음수 사용자 ID로 예외 발생`() {
        val invalidUserId = -1L
        val useAmount = 500L

        assertThatThrownBy { pointService.useUserPoint(invalidUserId, useAmount) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    /**
     * 사용자 포인트 히스토리 조회 테스트
     */
    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 빈 리스트`() {
        val userId = 1L
        val expectedHistories = emptyList<PointHistory>()

        every { pointHistoryTable.selectAllByUserId(userId) } returns expectedHistories

        val result = pointService.getUserPointHistory(userId)

        assertThat(result).isEmpty()
        verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
    }

    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 충전 및 사용 내역 포함`() {
        val userId = 1L
        val expectedHistories = listOf(
            PointHistory(id = 1L, userId = userId, type = TransactionType.CHARGE, amount = 1000L, timeMillis = 1234567890L),
            PointHistory(id = 2L, userId = userId, type = TransactionType.USE, amount = 500L, timeMillis = 1234567900L),
            PointHistory(id = 3L, userId = userId, type = TransactionType.CHARGE, amount = 2000L, timeMillis = 1234567910L)
        )

        every { pointHistoryTable.selectAllByUserId(userId) } returns expectedHistories

        val result = pointService.getUserPointHistory(userId)

        assertThat(result).hasSize(3)
        assertThat(result).isEqualTo(expectedHistories)
        assertThat(result[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(result[0].amount).isEqualTo(1000L)
        assertThat(result[1].type).isEqualTo(TransactionType.USE)
        assertThat(result[1].amount).isEqualTo(500L)
        assertThat(result[2].type).isEqualTo(TransactionType.CHARGE)
        assertThat(result[2].amount).isEqualTo(2000L)
        verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
    }

    @Test
    fun `존재하지 않는 사용자의 포인트 히스토리 조회 - 빈 리스트 반환`() {
        val nonExistentUserId = 999L
        val expectedHistories = emptyList<PointHistory>()

        every { pointHistoryTable.selectAllByUserId(nonExistentUserId) } returns expectedHistories

        val result = pointService.getUserPointHistory(nonExistentUserId)

        assertThat(result).isEmpty()
        verify(exactly = 1) { pointHistoryTable.selectAllByUserId(nonExistentUserId) }
    }
}
