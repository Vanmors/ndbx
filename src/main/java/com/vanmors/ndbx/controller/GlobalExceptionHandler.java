package com.vanmors.ndbx.controller;

import com.vanmors.ndbx.controller.response.ErrorResponse;
import com.vanmors.ndbx.service.exception.RegistrationException;
import com.vanmors.ndbx.service.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final ConstraintViolationException ex) {
        final String field = ex.getConstraintViolations().iterator().next().getPropertyPath().toString();
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid \"" + field + "\" field"));
    }

    @ExceptionHandler({RegistrationException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(final RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
