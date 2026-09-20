package com.selfdiscipline.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException e) {
        if (e.getStatus().is5xxServerError()) {
            log.error("业务异常[{}]: {}", e.getCode(), e.getMessage());
        } else {
            log.warn("业务异常[{}]: {}", e.getCode(), e.getMessage());
        }
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("code", e.getCode());
        return ResponseEntity.status(e.getStatus()).body(error);
    }

    @ExceptionHandler(ImageGenerationException.class)
    public ResponseEntity<Map<String, String>> handleImageException(ImageGenerationException e) {
        HttpStatus status = mapImageErrorToStatus(e);
        if (status.is5xxServerError()) {
            log.error("图像生成失败[{}]: {}", e.getType(), e.getMessage());
        } else {
            log.warn("图像生成失败[{}]: {}", e.getType(), e.getMessage());
        }
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("type", e.getType().name());
        error.put("code", e.getType().name());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("重复键异常: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", "用户名或邮箱已存在");
        error.put("code", "CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage() == null ? "请求参数错误" : e.getMessage());
        error.put("code", "BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        String firstMessage = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("验证失败");
        Map<String, String> response = new HashMap<>();
        response.put("message", firstMessage);
        response.put("code", "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        Map<String, String> error = new HashMap<>();
        error.put("message", "服务器内部错误");
        error.put("code", "INTERNAL_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private HttpStatus mapImageErrorToStatus(ImageGenerationException e) {
        ImageGenerationException.ErrorType t = e.getType();
        return switch (t) {
            case CONFIG_MISSING -> HttpStatus.INTERNAL_SERVER_ERROR;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case UNAUTHORIZED -> HttpStatus.BAD_GATEWAY;
            case FORBIDDEN -> HttpStatus.BAD_GATEWAY;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UPSTREAM_4XX, PARSING -> HttpStatus.BAD_GATEWAY;
            case UPSTREAM_5XX -> HttpStatus.BAD_GATEWAY;
        };
    }
}
