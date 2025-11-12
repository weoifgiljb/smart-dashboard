package com.selfdiscipline.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DuplicateKeyException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("重复键异常: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", "用户名或邮箱已存在");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        log.warn("运行时异常: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        Map<String, String> response = new HashMap<>();
        response.put("message", "验证失败");
        response.put("errors", errors.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        Map<String, String> error = new HashMap<>();
        error.put("message", "服务器内部错误");
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






