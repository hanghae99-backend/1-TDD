package io.hhplus.tdd.point.service

import io.hhplus.tdd.InvalidUserIdException
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.UserPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PointServiceUnitTest {

    @Mock
    private lateinit var userPointTable: UserPointTable

    @Mock
    private lateinit var pointHistoryTable: PointHistoryTable

    @InjectMocks
    private lateinit var pointService: PointService

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
}