package io.hhplus.tdd.point.util

import io.hhplus.tdd.InvalidUserIdException
import io.hhplus.tdd.InvalidAmountException
import io.hhplus.tdd.InsufficientPointException
import io.hhplus.tdd.point.constant.PointPolicy

/**
 * 포인트 관련 검증 유틸리티
 */
object PointValidator {
    
    /**
     * 사용자 ID 검증
     */
    fun validateUserId(userId: Long) {
        if (userId <= 0) {
            throw InvalidUserIdException(userId)
        }
    }
    
    /**
     * 충전 금액 검증
     */
    fun validateChargeAmount(amount: Long) {
        if (amount <= PointPolicy.MIN_CHARGE_AMOUNT) {
            throw InvalidAmountException(amount, "최소 충전 금액(${PointPolicy.MIN_CHARGE_AMOUNT})보다 작은")
        }
    }
    
    /**
     * 사용 금액 검증
     */
    fun validateUseAmount(amount: Long) {
        if (amount <= PointPolicy.MIN_USE_AMOUNT) {
            throw InvalidAmountException(amount, "최소 사용 금액(${PointPolicy.MIN_USE_AMOUNT})보다 작은")
        }
    }
    
    /**
     * 사용 가능한 포인트 잔액 검증
     */
    fun validateSufficientPoint(currentPoint: Long, useAmount: Long) {
        if (currentPoint <= 0 || currentPoint < useAmount) {
            throw InsufficientPointException(currentPoint, useAmount)
        }
    }
}