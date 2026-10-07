package org.dreamabout.sw.frp.be.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import org.dreamabout.sw.frp.be.domain.exception.UserAlreadyExistsException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.connector.UnknownConnectorTypeException;
import org.dreamabout.sw.frp.be.module.common.model.dto.ErrorDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "429", description = "Too Many Requests", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(schema = @Schema(implementation = ErrorDto.class)))
})
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorDto errorDto = new ErrorDto("Validation Error", "Validation failed: " + errors, null);
        return ResponseEntity.badRequest().body(errorDto);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorDto> handleBindException(BindException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorDto errorDto = new ErrorDto("Bind Error", "Binding failed: " + errors, null);
        return ResponseEntity.badRequest().body(errorDto);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorDto errorDto = new ErrorDto("ConstraintViolation", ex.getMessage(), null);
        return ResponseEntity.badRequest().body(errorDto);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException ex) {
        ErrorDto errorDto = new ErrorDto("AuthenticationError", "Invalid email or password", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDto);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex) {
        ErrorDto errorDto = new ErrorDto("AccessDenied", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorDto errorDto = new ErrorDto("UserAlreadyExists", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    @ExceptionHandler(UnknownConnectorTypeException.class)
    public ResponseEntity<ErrorDto> handleUnknownConnectorType(UnknownConnectorTypeException ex) {
        return ResponseEntity.badRequest().body(errorOf(ex));
    }

    /**
     * The source rejected the credentials; the user has to fix them.
     */
    @ExceptionHandler(ConnectorAuthException.class)
    public ResponseEntity<ErrorDto> handleConnectorAuth(ConnectorAuthException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(errorOf(ex));
    }

    @ExceptionHandler(ConnectorRateLimitedException.class)
    public ResponseEntity<ErrorDto> handleConnectorRateLimited(ConnectorRateLimitedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfter().toSeconds()))
                .body(errorOf(ex));
    }

    /**
     * Any other failure of the external source (unavailable, not ready yet).
     */
    @ExceptionHandler(ConnectorException.class)
    public ResponseEntity<ErrorDto> handleConnector(ConnectorException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorOf(ex));
    }

    private static ErrorDto errorOf(Exception ex) {
        return new ErrorDto(ex.getClass().getSimpleName(), ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOtherExceptions(Exception ex) {
        String stackTrace = Arrays.stream(ex.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

        String msg = ex.getMessage() != null ? ex.getMessage() : "Unexpected error of type: " + ex.getClass().getSimpleName();
        ErrorDto errorDto = new ErrorDto(ex.getClass().getSimpleName(), msg, stackTrace);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}
