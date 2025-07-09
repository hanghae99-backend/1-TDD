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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PointUnitTest {

    @Mock
    private lateinit var userPointTable: UserPointTable

    @Mock
    private lateinit var pointHistoryTable: PointHistoryTable

    @InjectMocks
    private lateinit var pointService: PointService

    /**
     * 사용자 포인트 조회
     */
    @Test
    fun `존재하는 사용자의 포인트 조회 성공`() {
        val expectedUserPoint = UserPoint(id = 1L, point = 1000L, updateMillis = 1234567890L)

        `when`(userPointTable.selectById(1L)).thenReturn(expectedUserPoint)

        val result = pointService.getUserPoint(1L)

        assertThat(result).isEqualTo(expectedUserPoint)
        verify(userPointTable, times(1)).selectById(1L)
    }

    @Test
    fun `존재하지 않는 사용자 조회 시 0포인트로 자동 생성`() {
        val expectedUserPoint = UserPoint(id = 999L, point = 0L, updateMillis = 1234567890L)

        `when`(userPointTable.selectById(999L)).thenReturn(expectedUserPoint)

        val result = pointService.getUserPoint(999L)

        assertThat(result).isEqualTo(expectedUserPoint)
        verify(userPointTable, times(1)).selectById(999L)
    }

    @Test
    fun `음수 사용자 ID로 조회 시 InvalidUserIdException 발생`() {
        val invalidUserId = -1L

        assertThatThrownBy { pointService.getUserPoint(invalidUserId) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")
        
        verify(userPointTable, never()).selectById(anyLong())
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

        `when`(userPointTable.selectById(userId)).thenReturn(currentUserPoint)
        `when`(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(expectedUserPoint)
        `when`(pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = anyLong()
        )).thenReturn(expectedHistory)

        val result = pointService.chargeUserPoint(userId, chargeAmount)

        assertThat(result.point).isEqualTo(1500L)
        verify(userPointTable, times(1)).selectById(userId)
        verify(userPointTable, times(1)).insertOrUpdate(userId, 1500L)
        verify(pointHistoryTable, times(1)).insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = anyLong()
        )
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

        verify(userPointTable, never()).selectById(anyLong())
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong())
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong())
    }

    @Test
    fun `포인트 충전 시 음수 사용자 ID로 예외 발생`() {
        val invalidUserId = -1L
        val chargeAmount = 1000L

        assertThatThrownBy { pointService.chargeUserPoint(invalidUserId, chargeAmount) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")

        verify(userPointTable, never()).selectById(anyLong())
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong())
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong())
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

        `when`(userPointTable.selectById(userId)).thenReturn(currentUserPoint)
        `when`(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(expectedUserPoint)
        `when`(pointHistoryTable.insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = anyLong()
        )).thenReturn(expectedHistory)

        val result = pointService.useUserPoint(userId, useAmount)

        assertThat(result.point).isEqualTo(700L)
        verify(userPointTable, times(1)).selectById(userId)
        verify(userPointTable, times(1)).insertOrUpdate(userId, 700L)
        verify(pointHistoryTable, times(1)).insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = anyLong()
        )
    }

    @Test
    fun `포인트 사용 시 잔액 부족으로 예외 발생`() {
        val userId = 1L
        val useAmount = 1500L
        val currentUserPoint = UserPoint(id = userId, point = 1000L, updateMillis = System.currentTimeMillis())

        `when`(userPointTable.selectById(userId)).thenReturn(currentUserPoint)

        assertThatThrownBy { pointService.useUserPoint(userId, useAmount) }
            .isInstanceOf(InsufficientPointException::class.java)
            .hasMessageContaining("잔액이 부족합니다. 현재 포인트: 1000, 사용 요청: 1500")

        verify(userPointTable, times(1)).selectById(userId)
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong())
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong())
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

        verify(userPointTable, never()).selectById(anyLong())
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong())
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong())
    }

    @Test
    fun `포인트 사용 시 음수 사용자 ID로 예외 발생`() {
        val invalidUserId = -1L
        val useAmount = 500L

        assertThatThrownBy { pointService.useUserPoint(invalidUserId, useAmount) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: $invalidUserId")

        verify(userPointTable, never()).selectById(anyLong())
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong())
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong())
    }

    /**
     * 사용자 포인트 히스토리 조회 테스트
     */
    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 빈 리스트`() {
        val userId = 1L
        val expectedHistories = emptyList<PointHistory>()

        `when`(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistories)

        val result = pointService.getUserPointHistory(userId)

        assertThat(result).isEmpty()
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId)
    }

    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 충전 및 사용 내역 포함`() {
        val userId = 1L
        val expectedHistories = listOf(
            PointHistory(id = 1L, userId = userId, type = TransactionType.CHARGE, amount = 1000L, timeMillis = 1234567890L),
            PointHistory(id = 2L, userId = userId, type = TransactionType.USE, amount = 500L, timeMillis = 1234567900L),
            PointHistory(id = 3L, userId = userId, type = TransactionType.CHARGE, amount = 2000L, timeMillis = 1234567910L)
        )

        `when`(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistories)

        val result = pointService.getUserPointHistory(userId)

        assertThat(result).hasSize(3)
        assertThat(result).isEqualTo(expectedHistories)
        assertThat(result[0].type).isEqualTo(TransactionType.CHARGE)
        assertThat(result[0].amount).isEqualTo(1000L)
        assertThat(result[1].type).isEqualTo(TransactionType.USE)
        assertThat(result[1].amount).isEqualTo(500L)
        assertThat(result[2].type).isEqualTo(TransactionType.CHARGE)
        assertThat(result[2].amount).isEqualTo(2000L)
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId)
    }

    @Test
    fun `존재하지 않는 사용자의 포인트 히스토리 조회 - 빈 리스트 반환`() {
        val nonExistentUserId = 999L
        val expectedHistories = emptyList<PointHistory>()

        `when`(pointHistoryTable.selectAllByUserId(nonExistentUserId)).thenReturn(expectedHistories)

        val result = pointService.getUserPointHistory(nonExistentUserId)

        assertThat(result).isEmpty()
        verify(pointHistoryTable, times(1)).selectAllByUserId(nonExistentUserId)
    }
}