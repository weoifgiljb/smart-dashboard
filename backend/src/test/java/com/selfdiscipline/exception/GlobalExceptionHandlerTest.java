package com.selfdiscipline.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionKeepsStatus() {
        ResponseEntity<Map<String, String>> res = handler.handleApiException(ApiException.notFound("任务不存在"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertEquals("任务不存在", res.getBody().get("message"));
        assertEquals("NOT_FOUND", res.getBody().get("code"));
    }

    @Test
    void unexpectedExceptionIsInternal() {
        ResponseEntity<Map<String, String>> res = handler.handleException(new IllegalStateException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("服务器内部错误", res.getBody().get("message"));
    }

    @Test
    void illegalArgumentIsBadRequest() {
        ResponseEntity<Map<String, String>> res = handler.handleBadRequest(new IllegalArgumentException("无效的日期时间"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }
}
