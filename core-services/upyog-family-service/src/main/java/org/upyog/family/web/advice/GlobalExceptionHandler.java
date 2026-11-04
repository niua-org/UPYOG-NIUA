package org.upyog.family.web.advice;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.Error;
import org.egov.tracer.model.ErrorRes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Global exception interceptor translating exceptions into standard UPYOG ErrorRes payloads.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles business domain CustomExceptions thrown by UPYOG validators and services.
     *
     * @param ex the caught CustomException containing business error codes and descriptions
     * @return ResponseEntity with structured UPYOG ErrorRes contract and HTTP 400 Bad Request status
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorRes> handleCustomException(CustomException ex) {
        log.error("[UPYOG-FAMILY-SERVICE] CustomException caught: code={}, message={}", ex.getCode(), ex.getMessage(), ex);

        List<Error> errors = new ArrayList<>();
        if (ex.getErrors() != null && !ex.getErrors().isEmpty()) {
            for (Map.Entry<String, String> entry : ex.getErrors().entrySet()) {
                Error error = new Error();
                error.setCode(entry.getKey());
                error.setMessage(entry.getValue());
                errors.add(error);
            }
        } else {
            Error error = new Error();
            error.setCode(ex.getCode());
            error.setMessage(ex.getMessage());
            errors.add(error);
        }

        ErrorRes errorRes = new ErrorRes();
        errorRes.setResponseInfo(new ResponseInfo());
        errorRes.setErrors(errors);

        return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles Spring validation constraint violations triggered by {@code @Valid} request body envelopes in UPYOG.
     *
     * @param ex the caught MethodArgumentNotValidException with field binding errors
     * @return ResponseEntity with validation error details and HTTP 400 Bad Request status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRes> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("[UPYOG-FAMILY-SERVICE] Validation error: {}", ex.getMessage());

        List<Error> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Error error = new Error();
            error.setCode(fieldError.getField());
            error.setMessage(fieldError.getDefaultMessage());
            errors.add(error);
        }

        ErrorRes errorRes = new ErrorRes();
        errorRes.setResponseInfo(new ResponseInfo());
        errorRes.setErrors(errors);

        return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal argument exceptions resulting from invalid query filters or parameters in UPYOG.
     *
     * @param ex the caught IllegalArgumentException
     * @return ResponseEntity with error details and HTTP 400 Bad Request status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRes> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("[UPYOG-FAMILY-SERVICE] IllegalArgumentException: {}", ex.getMessage(), ex);

        Error error = new Error();
        error.setCode("INVALID_ARGUMENT");
        error.setMessage(ex.getMessage());

        ErrorRes errorRes = new ErrorRes();
        errorRes.setResponseInfo(new ResponseInfo());
        errorRes.setErrors(Collections.singletonList(error));

        return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback exception handler for unexpected runtime or system exceptions in UPYOG.
     *
     * @param ex the caught unhandled Exception
     * @return ResponseEntity with generic internal error details and HTTP 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleGeneralException(Exception ex) {
        log.error("[UPYOG-FAMILY-SERVICE] Internal unhandled exception: {}", ex.getMessage(), ex);

        Error error = new Error();
        error.setCode("INTERNAL_SERVER_ERROR");
        error.setMessage(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred while processing family request");

        ErrorRes errorRes = new ErrorRes();
        errorRes.setResponseInfo(new ResponseInfo());
        errorRes.setErrors(Collections.singletonList(error));

        return new ResponseEntity<>(errorRes, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
