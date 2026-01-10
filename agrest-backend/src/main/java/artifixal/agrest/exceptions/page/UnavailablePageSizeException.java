package artifixal.agrest.exceptions.page;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Exception thrown when user requested page size is not available.
 */
@Getter
public class UnavailablePageSizeException extends PaginationException {

    private final int pageSize;

    public UnavailablePageSizeException(int pageSize, List<Integer> availablePageSizes) {
        super("Unavailable page size. Available page sizes: " +
            availablePageSizes.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        this.pageSize = pageSize;
    }
}
