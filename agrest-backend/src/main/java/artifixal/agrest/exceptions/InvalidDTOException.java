package artifixal.agrest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception related to malformed data sent in DTO.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDTOException extends RuntimeException {

    /**
     * @param msg What is wrong.
     */
    public InvalidDTOException(String msg) {
        super(msg);
    }
}