package artifixal.agrest.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Validator checking URL properness.
 */
public class StrictUrlValidator implements ConstraintValidator<ValidURL, String> {

    private String[] allowedProtocols;

    @Override
    public void initialize(ValidURL constraintAnnotation) {
        allowedProtocols = constraintAnnotation.protocols();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext cvc) {
        if (value == null || value.isBlank())
            return false;
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null)
                return false;
            boolean validProtocol = Arrays.asList(allowedProtocols)
                .contains(uri.getScheme().toLowerCase());
            if (!validProtocol)
                return false;
            if (uri.getHost() == null || uri.getHost().isBlank())
                return false;
            uri.toURL();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
