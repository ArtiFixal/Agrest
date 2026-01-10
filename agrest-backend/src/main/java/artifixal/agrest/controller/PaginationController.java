package artifixal.agrest.controller;

import artifixal.agrest.services.PageService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller providing available pagination options.
 */
@RequestMapping("/v1/pagination")
@RestController
@AllArgsConstructor
public class PaginationController {

    private final PageService pageService;

    @GetMapping()
    public Mono<List<Integer>> getAvailablePageSizes() {
        return Mono.just(pageService.getAvailablePageSizes());
    }
}
