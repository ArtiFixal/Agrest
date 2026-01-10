package artifixal.agrest.exceptions;

import io.swagger.parser.SwaggerException;
import java.util.List;
import lombok.Getter;

/**
 * Exception related to parsing of swagger file.
 */
@Getter
public class SwaggerParseException extends SwaggerException {

    private List<String> errors;

    public SwaggerParseException(String msg, List<String> errors) {
        super(msg);
        this.errors = errors;
    }
}
