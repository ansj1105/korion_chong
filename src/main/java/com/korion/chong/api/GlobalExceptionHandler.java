package com.korion.chong.api;

import com.korion.chong.leader.ForbiddenCountryScopeException;
import com.korion.chong.leader.LeaderNotFoundException;
import com.korion.chong.auth.AuthValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ForbiddenCountryScopeException.class)
    ResponseEntity<ApiErrorResponse> forbidden(ForbiddenCountryScopeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("FORBIDDEN_COUNTRY_SCOPE", exception.getMessage()));
    }

    @ExceptionHandler(LeaderNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(LeaderNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("LEADER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AuthValidationException.class)
    ResponseEntity<ApiErrorResponse> authValidation(AuthValidationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("BAD_REQUEST", exception.getMessage()));
    }
}
