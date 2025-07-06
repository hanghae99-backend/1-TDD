package io.hhplus.tdd.point.util

import io.hhplus.tdd.InvalidUserIdException

/**
 * 포인트 관련 검증 유틸리티
 */
object PointValidator {
    
    /**
     * 사용자 ID 검증
     */
    fun validateUserId(userId: Long) {
        if (userId < 0) {
            throw InvalidUserIdException(userId)
        }
    }
}