package io.hhplus.tdd.point.unit

import io.hhplus.tdd.InvalidUserIdException
import io.hhplus.tdd.InsufficientPointException
import io.hhplus.tdd.InvalidAmountException
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.dto.TransactionType
import io.hhplus.tdd.point.service.PointService
import io.hhplus.tdd.point.fixture.UserPointFixture
import io.hhplus.tdd.point.fixture.PointHistoryFixture
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
        // given
        val expectedUserPoint = UserPointFixture.create(id = 1L, point = 1000L)

        every { userPointTable.selectById(1L) } returns expectedUserPoint

        // when
        val result = pointService.getUserPoint(1L)

        // then
        assertThat(result).isEqualTo(expectedUserPoint)
        verify(exactly = 1) { userPointTable.selectById(1L) }
    }

    @Test
    fun `존재하지 않는 사용자 조회 시 0포인트로 자동 생성`() {
        // given
        val expectedUserPoint = UserPointFixture.createEmpty(id = 999L)

        every { userPointTable.selectById(999L) } returns expectedUserPoint

        // when
        val result = pointService.getUserPoint(999L)

        // then
        assertThat(result).isEqualTo(expectedUserPoint)
        verify(exactly = 1) { userPointTable.selectById(999L) }
    }

    @Test
    fun `음수 사용자 ID로 조회 시 InvalidUserIdException 발생`() {
        // when & then
        assertThatThrownBy { pointService.getUserPoint(-1L) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: -1")
        
        verify(exactly = 0) { userPointTable.selectById(any()) }
    }

    /**
     * 포인트 충전 테스트
     */
    @Test
    fun `포인트 충전 성공`() {
        // given
        val userId = 1L
        val chargeAmount = 1000L
        val currentUserPoint = UserPointFixture.create(id = userId, point = 500L)
        val expectedUserPoint = UserPointFixture.create(id = userId, point = 1500L)
        val expectedHistory = PointHistoryFixture.createCharge(userId = userId, amount = chargeAmount)

        every { userPointTable.selectById(userId) } returns currentUserPoint
        every { userPointTable.insertOrUpdate(userId, 1500L) } returns expectedUserPoint
        every { pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = any()
        ) } returns expectedHistory

        // when
        val result = pointService.chargeUserPoint(userId, chargeAmount)

        // then
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
        // when & then
        assertThatThrownBy { pointService.chargeUserPoint(1L, 0L) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 충전 금액(0)보다 작은 금액이 유효하지 않습니다")

        assertThatThrownBy { pointService.chargeUserPoint(1L, -100L) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 충전 금액(0)보다 작은 금액이 유효하지 않습니다")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.CHARGE, any()) }
    }

    @Test
    fun `포인트 충전 시 음수 사용자 ID로 예외 발생`() {
        // when & then
        assertThatThrownBy { pointService.chargeUserPoint(-1L, 1000L) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: -1")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.CHARGE, any()) }
    }

    /**
     * 포인트 사용 테스트
     */
    @Test
    fun `포인트 사용 성공`() {
        // given
        val userId = 1L
        val useAmount = 300L
        val currentUserPoint = UserPointFixture.createWithSufficientPoint(id = userId)
        val expectedUserPoint = UserPointFixture.create(id = userId, point = 9700L)
        val expectedHistory = PointHistoryFixture.createUse(userId = userId, amount = useAmount)

        every { userPointTable.selectById(userId) } returns currentUserPoint
        every { userPointTable.insertOrUpdate(userId, 9700L) } returns expectedUserPoint
        every { pointHistoryTable.insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = any()
        ) } returns expectedHistory

        // when
        val result = pointService.useUserPoint(userId, useAmount)

        // then
        assertThat(result.point).isEqualTo(9700L)
        verify(exactly = 1) { userPointTable.selectById(userId) }
        verify(exactly = 1) { userPointTable.insertOrUpdate(userId, 9700L) }
        verify(exactly = 1) { pointHistoryTable.insert(
            id = userId,
            amount = useAmount,
            transactionType = TransactionType.USE,
            updateMillis = any()
        ) }
    }

    @Test
    fun `포인트 사용 시 잔액 부족으로 예외 발생`() {
        // given
        val userId = 1L
        val useAmount = 1500L
        val currentUserPoint = UserPointFixture.createWithInsufficientPoint(id = userId)

        every { userPointTable.selectById(userId) } returns currentUserPoint

        // when & then
        assertThatThrownBy { pointService.useUserPoint(userId, useAmount) }
            .isInstanceOf(InsufficientPointException::class.java)
            .hasMessageContaining("잔액이 부족합니다. 현재 포인트: 100, 사용 요청: 1500")

        verify(exactly = 1) { userPointTable.selectById(userId) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    @Test
    fun `포인트 사용 시 0이하 금액으로 예외 발생`() {
        // when & then
        assertThatThrownBy { pointService.useUserPoint(1L, 0L) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 사용 금액(0)보다 작은 금액이 유효하지 않습니다")

        assertThatThrownBy { pointService.useUserPoint(1L, -100L) }
            .isInstanceOf(InvalidAmountException::class.java)
            .hasMessageContaining("최소 사용 금액(0)보다 작은 금액이 유효하지 않습니다")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    @Test
    fun `포인트 사용 시 음수 사용자 ID로 예외 발생`() {
        // when & then
        assertThatThrownBy { pointService.useUserPoint(-1L, 500L) }
            .isInstanceOf(InvalidUserIdException::class.java)
            .hasMessageContaining("유효하지 않은 사용자 ID입니다: -1")

        verify(exactly = 0) { userPointTable.selectById(any()) }
        verify(exactly = 0) { userPointTable.insertOrUpdate(any(), any()) }
        verify(exactly = 0) { pointHistoryTable.insert(any(), any(), TransactionType.USE, any()) }
    }

    /**
     * 사용자 포인트 히스토리 조회 테스트
     */
    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 빈 리스트`() {
        // given
        val userId = 1L
        val expectedHistories = emptyList<io.hhplus.tdd.point.dto.PointHistory>()

        every { pointHistoryTable.selectAllByUserId(userId) } returns expectedHistories

        // when
        val result = pointService.getUserPointHistory(userId)

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
    }

    @Test
    fun `사용자 포인트 히스토리 조회 성공 - 충전 및 사용 내역 포함`() {
        // given
        val userId = 1L
        val expectedHistories = listOf(
            PointHistoryFixture.createCharge(userId = userId, amount = 1000L, id = 1L),
            PointHistoryFixture.createUse(userId = userId, amount = 500L, id = 2L),
            PointHistoryFixture.createCharge(userId = userId, amount = 2000L, id = 3L)
        )

        every { pointHistoryTable.selectAllByUserId(userId) } returns expectedHistories

        // when
        val result = pointService.getUserPointHistory(userId)

        // then
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
        // given
        val userId = 999L
        val expectedHistories = emptyList<io.hhplus.tdd.point.dto.PointHistory>()

        every { pointHistoryTable.selectAllByUserId(userId) } returns expectedHistories

        // when
        val result = pointService.getUserPointHistory(userId)

        // then
        assertThat(result).isEmpty()
        verify(exactly = 1) { pointHistoryTable.selectAllByUserId(userId) }
    }
}
