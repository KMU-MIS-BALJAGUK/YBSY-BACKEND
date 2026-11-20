package com.baljaguk.ybsy.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 모든 오류 응답의 형식.
 * <pre>
 * { "code": "INVALID_INPUT", "message": "입력값이 올바르지 않습니다.",
 *   "fieldErrors": [ { "field": "title", "reason": "1~30자여야 합니다." } ] }
 * </pre>
 * 성공 응답은 감싸지 않고 DTO 를 그대로 돌려준다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }
}
