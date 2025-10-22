package com.webmini.miniweb.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,Object> handleNotFound(NotFoundException ex) {
        return Map.of("timestamp", Instant.now(), "status", 404, "error", "Not Found", "message", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return Map.of(
                "timestamp", Instant.now(),
                "status", 409,
                "error", "Conflict",
                "message", "Operation violates data integrity (likely foreign key constraint)."
        );
    }
}