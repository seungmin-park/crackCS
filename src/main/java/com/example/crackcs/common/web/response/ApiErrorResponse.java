package com.example.crackcs.common.web.response;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors,
        String requestId
) {

    public record FieldErrorResponse(String field, String reason) {
    }
}
