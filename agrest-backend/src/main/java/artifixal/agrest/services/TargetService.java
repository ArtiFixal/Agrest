package artifixal.agrest.services;

import artifixal.agrest.dto.TargetEntryDTO;
import artifixal.agrest.dto.target.TargetDTO;
import artifixal.agrest.dto.target.TargetDetailsDTO;
import artifixal.agrest.entity.Tag;
import artifixal.agrest.entity.Target;
import artifixal.agrest.exceptions.EntityNotFoundException;
import artifixal.agrest.exceptions.JsonPatchException;
import artifixal.agrest.exceptions.SwaggerParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.SwaggerException;
import io.swagger.util.Json;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import artifixal.agrest.repository.TargetBasicRepository;
import artifixal.agrest.repository.TargetRepository;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;

/**
 * Service related to scan target logic.
 */
@Slf4j
@Service
@AllArgsConstructor
public class TargetService {

    private final TargetBasicRepository targetBasicRepository;
    private final TargetRepository targetRepository;
    private final PageService pageService;
    private final JsonPatchService jsonPatchService;
    private final TagService tagService;
    private final JsonMapper mapper;
    private final Validator validator;

    private Mono<String> readOpenApiFile(FilePart openApiSpec) {
        return DataBufferUtils.join(openApiSpec.content())
            .map((dataBuffer) -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return new String(bytes);
            });
    }

    private Mono<Optional<JsonNode>> handleSwaggerFile(Optional<FilePart> openApiSpec) {
        if (openApiSpec.isEmpty()) {
            log.debug("No swagger file");
            return Mono.just(Optional.empty());
        }
        return readOpenApiFile(openApiSpec.get())
            .map((content) -> {
                SwaggerParseResult result = new OpenAPIParser().readContents(content, null, null);
                if (result.getMessages() != null && !result.getMessages().isEmpty())
                    throw new SwaggerParseException("Swagger contains errors", result.getMessages());
                return result.getOpenAPI();
            })
            .map((openApi) -> {
                try {
                    // Jackson 2 internal swagger util mapper
                    String json = Json.mapper().writeValueAsString(openApi);
                    // Jackson 3 node
                    JsonNode node = mapper.readTree(json);
                    return Optional.of(node);
                } catch (JsonProcessingException e) {
                    throw new SwaggerException("Failed to convert swagger into JSON");
                }
            });
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public Mono<Long> addTarget(TargetDTO targetData, Optional<FilePart> openApiSpec) {
        return handleSwaggerFile(openApiSpec)
            .map((swaggerFile) -> new Target(
                targetData.name(),
                targetData.url(),
                targetData.description().orElse(""),
                swaggerFile.orElse(null),
                targetData.headers().orElse(Map.of()),
                targetData.cookies().orElse(Map.of())))
            .flatMap((toAdd) -> targetBasicRepository.save(toAdd))
            .flatMap((target) -> {
                if (targetData.tags().isPresent())
                    return tagService.addTagsToTarget(target.getId(), targetData.tags().get())
                        .then(Mono.just(target.getId()));
                return Mono.just(target.getId());
            });
    }

    /**
     * Updates the given target.
     *
     * @param targetID Which target to update.
     * @param updateData JsonPatch containing updates.
     * @param swagger Optional swagger file, if provided it always replace existing.
     *
     * @return Mono with update task to subscribe.
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public Mono<Void> editTarget(Long targetID, String updateData, Optional<FilePart> swagger) {
        return targetRepository.findEntityByID(targetID)
            .flatMap((originalTarget) -> handleSwaggerFile(swagger).flatMap((swaggerOptional) -> {
                TargetDTO originalDto = toDto(originalTarget);
                return jsonPatchService.applyPatch(updateData, originalDto, TargetDTO.class)
                    .flatMap((patchedDto) -> {
                        if (!validator.validate(patchedDto).isEmpty())
                            throw new JsonPatchException("Patch request malformed");
                        return Mono
                            .just(toEntity(targetID, patchedDto, swaggerOptional.orElse(originalTarget.getSwagger())))
                            // Fill missing audit fields
                            // Edit fields will be auto filled on update
                            .map((target) -> {
                                target.setCreatorID(originalTarget.getCreatorID());
                                target.setCreated(originalTarget.getCreated());
                                return target;
                            })
                            .flatMap((patchedTarget) -> tagService.editTargetTags(targetID, originalTarget.getTags(),
                                patchedDto.tags().orElse(List.of()))
                                .then(Mono.just(patchedTarget)));
                    })
                    .flatMap((patchedTarget) -> targetBasicRepository.save(patchedTarget))
                    .then();
            }));
    }

    public Mono<Target> getTargetEntity(Long targetID) {
        return targetRepository.findEntityByID(targetID)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("Target", targetID)));
    }

    public Mono<TargetDetailsDTO> getTargetDTO(Long targetID) {
        return targetRepository.findByID(targetID)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("Target", targetID)));
    }

    /**
     * Fetches target data for update form.
     *
     * @param targetID Which target to fetch.
     *
     * @return Mono emiting target data.
     */
    public Mono<TargetDTO> getUpdateTargetDTO(Long targetID) {
        return getTargetEntity(targetID)
            .map((target) -> toDto(target));
    }

    /**
     * Fetches a paginated list of targets matching the optional search query.
     * When a query is provided, targets are matched if the query appears in their
     * name, URL, or any associated tag name (case-insensitive). <p>
     *
     * If no query parameter fetches default page.
     *
     * @param query Optional search term to fetch items containing it.
     * @param page Which page to fetch.
     *
     * @return Zipped Mono with pagination headers and Flux to subscribe.
     */
    public Mono<Tuple2<HttpHeaders, Flux<TargetEntryDTO>>> getTargetPage(Optional<String> query, Pageable page) {
        if (query.isPresent()) {
            return targetRepository.countByQuery(query.get())
                .map((totalElements) -> pageService.createPaginationHeadersFromCount(page, totalElements))
                .zipWith(Mono.just(targetRepository.findAllBy(query.get(), page)));
        }
        return pageService.createPaginationHeaders(page, targetBasicRepository)
            .zipWith(Mono.just(targetRepository.findAllBy(page)));
    }

    private TargetDTO toDto(Target target) {
        List<String> tags = target.getTags()
            .stream()
            .map((tag) -> tag.getName())
            .collect(Collectors.toList());
        return new TargetDTO(target.getName(), target.getUrl(), Optional.ofNullable(target.getDescription()),
            Optional.of(tags), Optional.of(target.getHeaders()), Optional.of(target.getCookies()));
    }

    private Target toEntity(Long targetId, TargetDTO dto, JsonNode swagger) {
        List<Tag> tags = dto.tags()
            .orElse(List.of())
            .stream()
            .map((tag) -> new Tag(tag))
            .collect(Collectors.toList());
        Target target = new Target(targetId,
            dto.name(),
            dto.url(),
            dto.description().orElse(""),
            swagger,
            dto.headers().orElse(Map.of()),
            dto.cookies().orElse(Map.of()));
        target.setTags(tags);
        return target;
    }
}
