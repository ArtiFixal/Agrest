package artifixal.agrest.exceptions.page;

/**
 * Exception thrown on pagination errors.
 */
public class PaginationException extends RuntimeException {

    public PaginationException(String msg) {
        super(msg);
    }
}
