package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.ErrorResponse;
import com.vanmors.ndbx.service.exception.RegistrationException;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        final String field = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getField)
                .orElse("unknown");

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid " + field + " field"));
    }

    @ExceptionHandler({RegistrationException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(final RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("invalid credentials"));
    }
}
