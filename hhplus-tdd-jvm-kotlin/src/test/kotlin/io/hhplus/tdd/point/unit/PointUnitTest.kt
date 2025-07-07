package io.hhplus.tdd.point.unit

import io.hhplus.tdd.InvalidUserIdException
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
     * 사용자 포인트 히스토리 조회
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