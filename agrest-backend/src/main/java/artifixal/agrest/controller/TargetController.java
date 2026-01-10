package artifixal.agrest.controller;

import artifixal.agrest.dto.TargetEntryDTO;
import artifixal.agrest.dto.target.TargetDTO;
import artifixal.agrest.dto.target.TargetDetailsDTO;
import artifixal.agrest.services.TargetService;
import artifixal.agrest.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller related to {@code Target} manipulation.
 */
@Slf4j
@RequestMapping("/v1/targets")
@RestController
@AllArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Long> addTarget(@RequestPart("target") @Valid TargetDTO newTargetData,
        @RequestPart(required = false) FilePart swagger) {
        return targetService.addTarget(newTargetData, Optional.ofNullable(swagger))
            .doOnSuccess((id) -> {
                UserService.getCurrentUserID()
                    .doOnSuccess((userID) -> log.info("New target {} created by {}", id, userID));
            });
    }

    @PatchMapping(path = "/{targetID}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> editTarget(@PathVariable @Min(0) Long targetID,
        @RequestPart("target") String jsonPatch,
        @RequestPart(required = false) FilePart swagger) {
        return targetService.editTarget(targetID, jsonPatch, Optional.ofNullable(swagger))
            .doOnSuccess((result) -> {
                UserService.getCurrentUserID()
                    .doOnSuccess((userID) -> log.info("Target {} updated by {}", targetID, userID));
            });
    }

    @GetMapping("/{targetID}/dto")
    public Mono<TargetDTO> getTargetDto(@PathVariable @Min(0) Long targetID) {
        return targetService.getUpdateTargetDTO(targetID);
    }

    @GetMapping("/{targetID}")
    public Mono<TargetDetailsDTO> getTarget(@PathVariable @Min(0) Long targetID) {
        return targetService.getTargetDTO(targetID);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TargetEntryDTO>> getTargetPage(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size,
        ServerHttpResponse response) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return targetService.getTargetPage(Optional.ofNullable(query), pageRequest)
            .flatMapMany((tuple) -> {
                response.getHeaders().addAll(tuple.getT1());
                return tuple.getT2();
            })
            .map((target) -> ServerSentEvent.builder(target)
                .event("targets")
                .id(String.valueOf(target.id()))
                .retry(Duration.ofSeconds(3))
                .build());
    }
}
