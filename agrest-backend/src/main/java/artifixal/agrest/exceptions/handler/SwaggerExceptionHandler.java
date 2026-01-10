package artifixal.agrest.exceptions.handler;

import artifixal.agrest.dto.ErrorDTO;
import artifixal.agrest.dto.SwaggerErrorDTO;
import artifixal.agrest.exceptions.SwaggerParseException;
import io.swagger.parser.SwaggerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Exception handler for {@code SwaggerExceptions}
 */
@RestControllerAdvice
public class SwaggerExceptionHandler {

    @ExceptionHandler(SwaggerParseException.class)
    public Mono<ResponseEntity<SwaggerErrorDTO>> handleSwaggerParseException(SwaggerParseException ex,
        ServerWebExchange exchange) {
        SwaggerErrorDTO dto = new SwaggerErrorDTO(ex.getMessage(), ex.getErrors());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(dto));
    }

    @ExceptionHandler(SwaggerException.class)
    public Mono<ResponseEntity<ErrorDTO>> handleSwaggerException(SwaggerException ex,
        ServerWebExchange exchange) {
        ErrorDTO dto = new ErrorDTO(ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(dto));
    }
}
