package org.egov.dx.upyogdata.controller;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @RestControllerAdvice is auto-detected by Spring - no registration needed, It will intercept
 * MethodArgumentNotValidException, HttpMessageNotReadableException, RuntimeException, FeignException
 * globally across all controllers,
 * */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @NotBlank, @NotNull, @NotEmpty on request fields
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(Map.of("message", errors));
    }

    // Wrong Date format (e.g. voucherDate not in dd/MM/yyyy)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body: " + ex.getMostSpecificCause().getMessage()));
    }

    // Kafka publish failure — thrown from PFMSServiceImpl when producer.push().get() fails
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        log.error("Internal error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", ex.getMessage()));
    }

    // PFMS Auth API or Data API unreachable / returns error — thrown from PFMSApiClient via Feign
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignException(FeignException ex) {
        log.error("PFMS API error | status={} message={}", ex.status(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "PFMS API error: " + ex.getMessage()));
    }
}
