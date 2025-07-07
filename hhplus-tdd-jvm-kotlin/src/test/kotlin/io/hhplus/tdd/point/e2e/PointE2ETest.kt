package io.hhplus.tdd.point.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.random.Random

@SpringBootTest
@AutoConfigureMockMvc
class PointE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * 사용자 초기 상태 테스트
     */
    @Test
    fun `신규 사용자는 0포인트로 시작하고 히스토리가 비어있어야 한다`() {
        val newUserId = Random.nextLong(10000, 99999)

        // 포인트 조회
        mockMvc.perform(get("/point/{id}", newUserId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(newUserId))
            .andExpect(jsonPath("$.point").value(0L))
            .andExpect(jsonPath("$.updateMillis").isNumber)

        // 히스토리 조회
        mockMvc.perform(get("/point/{id}/histories", newUserId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    /**
     * 포인트 충전 및 히스토리 테스트
     */
    @Test
    fun `포인트 충전 후 히스토리에 정확히 기록되어야 한다`() {
        val userId = Random.nextLong(10000, 99999)
        val chargeAmount = 1000L

        // 포인트 충전
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeAmount))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(chargeAmount))

        // 히스토리 조회로 충전 내역 확인
        mockMvc.perform(get("/point/{id}/histories", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(userId))
            .andExpect(jsonPath("$[0].type").value("CHARGE"))
            .andExpect(jsonPath("$[0].amount").value(chargeAmount))
            .andExpect(jsonPath("$[0].timeMillis").isNumber)
    }

    @Test
    fun `포인트 충전과 사용을 반복해도 히스토리가 정확히 쌓여야 한다`() {
        val userId = Random.nextLong(10000, 99999)
        val chargeAmount1 = 1000L
        val chargeAmount2 = 500L
        val useAmount = 300L

        // 포인트 충전 (2회)
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeAmount1))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.point").value(chargeAmount1))

        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeAmount2))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.point").value(chargeAmount1 + chargeAmount2))

        // 포인트 사용
        mockMvc.perform(
            patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(useAmount))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.point").value(chargeAmount1 + chargeAmount2 - useAmount))

        // 히스토리 조회로 모든 거래 내역 확인
        mockMvc.perform(get("/point/{id}/histories", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            // 첫 번째 충전
            .andExpect(jsonPath("$[0].userId").value(userId))
            .andExpect(jsonPath("$[0].type").value("CHARGE"))
            .andExpect(jsonPath("$[0].amount").value(chargeAmount1))
            // 두 번째 충전
            .andExpect(jsonPath("$[1].userId").value(userId))
            .andExpect(jsonPath("$[1].type").value("CHARGE"))
            .andExpect(jsonPath("$[1].amount").value(chargeAmount2))
            // 사용
            .andExpect(jsonPath("$[2].userId").value(userId))
            .andExpect(jsonPath("$[2].type").value("USE"))
            .andExpect(jsonPath("$[2].amount").value(useAmount))
    }

    /**
     * 다중 사용자 테스트
     */
    @Test
    fun `여러 사용자가 동시에 사용해도 각자의 히스토리가 독립적이어야 한다`() {
        val userId1 = Random.nextLong(10000, 99999)
        val userId2 = Random.nextLong(10000, 99999)
        val amount1 = 1000L
        val amount2 = 2000L

        // 사용자1 포인트 충전
        mockMvc.perform(
            patch("/point/{id}/charge", userId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(amount1))
        )
            .andExpect(status().isOk)

        // 사용자2 포인트 충전
        mockMvc.perform(
            patch("/point/{id}/charge", userId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(amount2))
        )
            .andExpect(status().isOk)

        // 사용자1 히스토리 조회
        mockMvc.perform(get("/point/{id}/histories", userId1))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(userId1))
            .andExpect(jsonPath("$[0].amount").value(amount1))

        // 사용자2 히스토리 조회
        mockMvc.perform(get("/point/{id}/histories", userId2))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(userId2))
            .andExpect(jsonPath("$[0].amount").value(amount2))
    }

    /**
     * 에러 처리 테스트
     */
    @Test
    fun `잘못된 사용자 ID로 접근시 적절한 에러가 발생해야 한다`() {
        val invalidUserId = -1L

        // 포인트 조회는 에러
        mockMvc.perform(get("/point/{id}", invalidUserId))
            .andExpect(status().isBadRequest)

        // 히스토리 조회는 현재 검증하지 않음 (빈 리스트)
        mockMvc.perform(get("/point/{id}/histories", invalidUserId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    /**
     * 데이터 일관성 테스트
     */
    @Test
    fun `사용자의 현재 포인트와 히스토리 합계가 일치해야 한다`() {
        val userId = Random.nextLong(10000, 99999)
        val chargeAmount = 1500L
        val useAmount = 500L

        // 포인트 충전
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeAmount))
        )
            .andExpect(status().isOk)

        // 포인트 사용
        mockMvc.perform(
            patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(useAmount))
        )
            .andExpect(status().isOk)

        // 현재 포인트 조회
        val expectedCurrentPoint = chargeAmount - useAmount
        mockMvc.perform(get("/point/{id}", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(expectedCurrentPoint))

        // 히스토리 조회로 거래 내역 확인
        mockMvc.perform(get("/point/{id}/histories", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("CHARGE"))
            .andExpect(jsonPath("$[0].amount").value(chargeAmount))
            .andExpect(jsonPath("$[1].type").value("USE"))
            .andExpect(jsonPath("$[1].amount").value(useAmount))
    }
}
