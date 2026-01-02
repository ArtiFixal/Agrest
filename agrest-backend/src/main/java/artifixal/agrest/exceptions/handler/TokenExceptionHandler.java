package artifixal.agrest.exceptions.handler;

import artifixal.agrest.dto.ErrorDTO;
import lombok.AllArgsConstructor;
import org.paseto4j.commons.PasetoException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles token auth exceptions.
 */
@Configuration
@Order(-2)
@AllArgsConstructor
public class TokenExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper mapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof PasetoException) {
            exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);
            ErrorDTO errorDto = new ErrorDTO(ex.getMessage());
            DataBuffer buff = exchange.getResponse()
                .bufferFactory()
                .wrap(mapper.writeValueAsBytes(errorDto));
            return exchange.getResponse()
                .writeWith(Mono.just(buff));
        }
        // Pass other errors
        return Mono.error(ex);
    }
}
