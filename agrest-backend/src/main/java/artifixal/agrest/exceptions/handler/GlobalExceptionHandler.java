package artifixal.agrest.exceptions.handler;

import artifixal.agrest.dto.ErrorDTO;
import artifixal.agrest.exceptions.EntityNotFoundException;
import artifixal.agrest.exceptions.JsonPatchException;
import artifixal.agrest.exceptions.page.PaginationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.vault.core.SecretNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Exception handler which handles common exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    protected Mono<ResponseEntity<ErrorDTO>> handleAsUnauthorized(ErrorDTO dto) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(dto));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ErrorDTO>> handleAuthorizationException(AuthenticationException ex,
        ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO(ex.getMessage());
        return handleAsUnauthorized(dto);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public Mono<ResponseEntity<ErrorDTO>> handleEntityNotFound(EntityNotFoundException ex, ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO(ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(dto));
    }

    @ExceptionHandler(SecretNotFoundException.class)
    public Mono<ResponseEntity<ErrorDTO>> handleSecretNotFound(SecretNotFoundException ex, ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO("Server is missing some essential data");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(dto));
    }

    @ExceptionHandler(JsonPatchException.class)
    public Mono<ResponseEntity<ErrorDTO>> handleJsonPatchException(JsonPatchException ex, ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO(ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(dto));
    }

    @ExceptionHandler(PaginationException.class)
    public Mono<ResponseEntity<ErrorDTO>> handlePaginationException(
        PaginationException ex,
        ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO(ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(dto));
    }
}
