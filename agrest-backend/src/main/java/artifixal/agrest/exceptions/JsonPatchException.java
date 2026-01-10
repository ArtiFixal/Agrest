package artifixal.agrest.exceptions;

/**
 * Exception thrown on JsonPatch errors.
 */
public class JsonPatchException extends RuntimeException {

    public JsonPatchException(String msg) {
        super(msg);
    }
}
