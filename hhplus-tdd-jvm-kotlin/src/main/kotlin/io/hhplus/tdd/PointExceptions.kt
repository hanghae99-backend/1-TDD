package io.hhplus.tdd

class InvalidUserIdException(userId: Long) : RuntimeException("유효하지 않은 사용자 ID입니다: $userId")

class InsufficientPointException(currentPoint: Long, requestAmount: Long) : 
    RuntimeException("잔액이 부족합니다. 현재 포인트: $currentPoint, 사용 요청: $requestAmount")

class InvalidAmountException(amount: Long, operation: String) : 
    RuntimeException("$operation 금액이 유효하지 않습니다: $amount")
