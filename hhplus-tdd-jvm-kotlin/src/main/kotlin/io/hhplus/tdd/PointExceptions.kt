package io.hhplus.tdd

class InvalidUserIdException(userId: Long) : RuntimeException("유효하지 않은 사용자 ID입니다: $userId")
