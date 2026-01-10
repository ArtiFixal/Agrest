package artifixal.agrest.services;

import artifixal.agrest.config.PaginationProperties;
import artifixal.agrest.exceptions.page.PaginationException;
import artifixal.agrest.exceptions.page.UnavailablePageSizeException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service related to pagination.
 */
@Service
public class PageService {

    /**
     * Describes how many pages there are total.
     */
    public final static String TOTAL_PAGES = "X-Total-Pages";

    /**
     * Describes how many items are total in DB.
     */
    public final static String TOTAL_COUNT = "X-Total-Count";

    /**
     * On which page we are.
     */
    public final static String CURRENT_PAGE = "X-Current-Page";

    /**
     * How many elements will single page contain.
     */
    public final static String PAGE_SIZE = "X-Page-Size";

    private static List<Integer> availablePageSizes;

    public PageService(PaginationProperties paginationProperties) {
        availablePageSizes = paginationProperties.getSizes();
    }

    /**
     * @return Allowed page sizes for pagination.
     */
    public List<Integer> getAvailablePageSizes() {
        return availablePageSizes;
    }

    /**
     * Counts all items in repository and creates HTTP headers with pagination data for the given page.
     *
     * @param page Pagination parameter on which headers will be based.
     * @param repo From where to count items.
     *
     * @return Mono emiting headers with pagination data.
     */
    public Mono<HttpHeaders> createPaginationHeaders(Pageable page, R2dbcRepository repo) {
        if (!availablePageSizes.contains(page.getPageSize()))
            throw new UnavailablePageSizeException(page.getPageSize(), availablePageSizes);
        if (page.getPageNumber() < 0)
            throw new PaginationException("Page number can't be negative");
        return repo.count()
            .map((total) -> createPaginationHeadersFromCount(page, (Long) total));
    }

    /**
     * Creates HTTP headers with pagination data for the given page.
     *
     * @param page Pagination parameter on which headers will be based.
     * @param totalCount How many elements are in DB.
     *
     * @return Headers with pagination data.
     */
    public HttpHeaders createPaginationHeadersFromCount(Pageable page, Long totalCount) {
        long totalPages = (long) totalCount / page.getPageSize() + 1;
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOTAL_PAGES, String.valueOf(totalPages));
        headers.add(TOTAL_COUNT, String.valueOf(totalCount));
        headers.add(CURRENT_PAGE, String.valueOf(page.getPageNumber()));
        headers.add(PAGE_SIZE, String.valueOf(page.getPageSize()));
        return headers;
    }
}
