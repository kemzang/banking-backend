package com.banking.transaction_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TransactionNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiError> handleInvalidTransaction(
            InvalidTransactionException exception,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(TransactionLimitExceededException.class)
    public ResponseEntity<ApiError> handleLimitExceeded(
            TransactionLimitExceededException exception,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({
            SoldeInsuffisantException.class,
            DeviseIncompatibleException.class
    })
    public ResponseEntity<ApiError> handleBusinessConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AccountServiceException.class)
    public ResponseEntity<ApiError> handleAccountService(
            AccountServiceException exception,
            HttpServletRequest request
    ) {
        return buildError(exception.getStatusCode(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildError(
                HttpStatus.BAD_REQUEST,
                "La requete contient des donnees invalides",
                request,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return buildError(
                HttpStatus.BAD_REQUEST,
                "La requete contient des donnees invalides",
                request,
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Erreur interne non gérée sur {} : {}", request.getRequestURI(), exception.getMessage(), exception);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiError> buildError(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        String reason = resolvedStatus != null ? resolvedStatus.getReasonPhrase() : "HTTP Error";
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                reason,
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(error);
    }
}
