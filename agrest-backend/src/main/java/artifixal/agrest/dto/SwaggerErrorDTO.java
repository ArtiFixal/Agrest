package artifixal.agrest.dto;

import java.util.List;

/**
 * DTO transporting swagger parsing error with its messages.
 */
public record SwaggerErrorDTO(String message, List<String> errors) {

}
