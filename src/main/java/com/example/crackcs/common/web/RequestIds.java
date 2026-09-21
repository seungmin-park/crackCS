package com.example.crackcs.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIds {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "crackcs.requestId";
    public static final String MDC_KEY = "requestId";

    private RequestIds() {
    }

    public static String current(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        return value instanceof String requestId ? requestId : UUID.randomUUID().toString();
    }
}
