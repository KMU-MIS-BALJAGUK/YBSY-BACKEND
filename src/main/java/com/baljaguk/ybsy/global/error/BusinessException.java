package com.baljaguk.ybsy.global.error;

import lombok.Getter;

/**
 * 서비스 규칙 위반을 알리는 예외. {@link GlobalExceptionHandler} 가 {@link ErrorResponse} 로 바꿔 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
