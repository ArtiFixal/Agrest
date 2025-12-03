package artifixal.agrest.exceptions;

import org.springframework.security.web.server.csrf.CsrfException;

/**
 * Exception related to CSRF tokens errors.
 */
public class CsrfTokenException extends CsrfException{

    public CsrfTokenException(String msg){
        super(msg);
    }
}
