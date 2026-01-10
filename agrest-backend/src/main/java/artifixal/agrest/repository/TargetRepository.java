package artifixal.agrest.repository;

import artifixal.agrest.dto.TagDTO;
import artifixal.agrest.dto.TargetEntryDTO;
import artifixal.agrest.dto.target.TargetDetailsDTO;
import artifixal.agrest.entity.Tag;
import artifixal.agrest.entity.Target;
import artifixal.agrest.services.TagService;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * R2DBC Repository able to fetch entity with its relationships.
 */
@Repository
@AllArgsConstructor
public class TargetRepository {

    private final R2dbcEntityTemplate entityTemplate;
    private final ObjectMapper mapper;
    private final TagRepository tagRepository;
    private final TagService tagService;

    private interface TagRow {
        public Long tagID();

        public String tagName();
    }

    private record TargetDetailRow(Long targetID, String targetName, String url,
        String description, Map<String, String> headers, Map<String, String> cookies,
        LocalDateTime created, LocalDateTime edited, Long tagID, String tagName) implements TagRow {
    }

    public Mono<TargetDetailsDTO> findByID(Long targetID) {
        String select = """
            SELECT t.id,t.name,t.url,t.description,t.headers,t.cookies,t.created,t.edited,tg.id,tg.name
            FROM targets t
            LEFT JOIN targets_tags tt ON t.id=tt.target_id
            LEFT JOIN tags tg ON tg.id=tt.tag_id
            WHERE t.id = :id
            """;
        return entityTemplate.getDatabaseClient()
            .sql(select)
            .bind("id", targetID)
            .map(this::toTargetDetailsRow)
            .all()
            .collectList()
            .map((rows) -> {
                var tags = collectTags(rows);
                var row = rows.getFirst();
                return new TargetDetailsDTO(targetID,
                    row.targetName(),
                    row.url(),
                    Optional.ofNullable(row.description()),
                    tags,
                    Optional.ofNullable(row.headers()),
                    Optional.ofNullable(row.cookies()),
                    row.created(),
                    Optional.ofNullable(row.edited()));
            });
    }

    private record TargetEntryRow(Long targetID, String targetName, String targetUrl) {
    }

    /**
     * Returns the given page of elements.
     *
     * @param page Page to get
     *
     * @return Flux containing targets with tags if they have any.
     */
    public Flux<TargetEntryDTO> findAllBy(Pageable page) {
        String select = """
            SELECT t.id,t.name,t.url
            FROM targets t
            ORDER BY t.id ASC
            LIMIT :limit OFFSET :offset
            """;
        long offset = page.getPageSize() * page.getPageNumber();
        return entityTemplate.getDatabaseClient()
            .sql(select)
            .bind("limit", page.getPageSize())
            .bind("offset", offset)
            .map(this::toTargetEntryRow)
            .all()
            .flatMap((row) -> {
                return tagRepository.findAllByTargetID(row.targetID)
                    .collectList()
                    .map((tags) -> {
                        var dtoTags = tagService.toDTOList(tags);
                        return new TargetEntryDTO(row.targetID(), row.targetName(), row.targetUrl(), dtoTags,
                            Optional.empty());
                    });
            });
    }

    /**
     * Returns the given page of elements which name, url or tag name match the query.
     *
     * @param query Pattern by which look for entries.
     * @param page On which page to look for
     *
     * @return Flux containing targets with tags if they have any.
     */
    public Flux<TargetEntryDTO> findAllBy(String query, Pageable page) {
        String select = """
            SELECT t.id,t.name,t.url
            FROM targets t
            WHERE t.name ILIKE :query OR t.url ILIKE :query2 OR EXISTS (
                SELECT 1 FROM targets_tags tt
                JOIN tags tg ON tg.id=tt.tag_id
                WHERE tt.target_id=t.id AND tg.name ILIKE :query3
            )
            ORDER BY t.name LIMIT :limit OFFSET :offset
            """;
        long offset = page.getPageSize() * page.getPageNumber();
        String queryPattern = "%" + query + "%";
        return entityTemplate.getDatabaseClient()
            .sql(select)
            .bind("query", queryPattern)
            .bind("query2", queryPattern)
            .bind("query3", queryPattern)
            .bind("limit", page.getPageSize())
            .bind("offset", offset)
            .map(this::toTargetEntryRow)
            .all()
            .flatMap((row) -> {
                return tagRepository.findAllByTargetID(row.targetID)
                    .collectList()
                    .map((tags) -> {
                        var dtoTags = tagService.toDTOList(tags);
                        return new TargetEntryDTO(row.targetID(), row.targetName(), row.targetUrl(), dtoTags,
                            Optional.empty());
                    });
            });
    }

    /**
     * Count how many elements match the given query.
     *
     * @param query What to match.
     *
     * @return Mono with element count.
     */
    public Mono<Long> countByQuery(String query) {
        String countQuery = """
            SELECT COUNT(*)
            FROM targets t
            LEFT JOIN targets_tags tt ON t.id=tt.target_id
            LEFT JOIN tags tg ON tt.tag_id=tg.id
            WHERE t.name ILIKE :query OR t.url ILIKE :query2 OR tg.name ILIKE :query3
            """;
        String queryPattern = "%" + query + "%";
        return entityTemplate.getDatabaseClient()
            .sql(countQuery)
            .bind("query", queryPattern)
            .bind("query2", queryPattern)
            .bind("query3", queryPattern)
            .map((t) -> t.get("count", Long.class))
            .one()
            .defaultIfEmpty(0L);
    }

    private record TargetRow(Long targetID, String targetName, String url, String description, JsonNode swaagger,
        Map<String, String> headers, Map<String, String> cookies, UUID creatorID, UUID editorID, LocalDateTime created,
        LocalDateTime edited, Long tagID, String tagName) implements TagRow {
    }

    public Mono<Target> findEntityByID(Long targetID) {
        String select = """
            SELECT t.id,t.name,t.url,t.description,t.swagger,t.headers,t.cookies,t.creator_id,t.editor_id,t.created,
                        t.edited,tg.id,tg.name
            FROM targets t
            LEFT JOIN targets_tags tt ON t.id=tt.target_id
            LEFT JOIN tags tg ON tg.id=tt.tag_id
            WHERE t.id = :id
            """;
        return entityTemplate.getDatabaseClient()
            .sql(select)
            .bind("id", targetID)
            .map(this::toTargetRow)
            .all()
            .collectList()
            .map((rows) -> {
                var tags = rows.stream()
                    .filter((row) -> row.tagName() != null)
                    .map((row) -> new Tag(row.tagID(), row.tagName()))
                    .collect(Collectors.toList());
                var row = rows.getFirst();
                Target target = new Target(row.targetID(),
                    row.targetName(),
                    row.url(),
                    row.description(),
                    row.swaagger(),
                    row.headers(),
                    row.cookies());
                target.setCreatorID(row.creatorID());
                target.setEditorID(row.editorID());
                target.setCreated(row.created());
                target.setEdited(row.edited());
                target.setSwagger(row.swaagger());
                target.setTags(tags);
                return target;
            });
    }

    private TargetEntryRow toTargetEntryRow(Row row, RowMetadata metadata) {
        return new TargetEntryRow(row.get(0, Long.class),
            row.get(1, String.class),
            row.get(2, String.class));
    }

    private TargetDetailRow toTargetDetailsRow(Row row, RowMetadata metadata) {
        return new TargetDetailRow(row.get(0, Long.class),
            row.get(1, String.class),
            row.get(2, String.class),
            row.get(3, String.class),
            convertJsonToMap(row.get(4, Json.class)),
            convertJsonToMap(row.get(5, Json.class)),
            row.get(6, LocalDateTime.class),
            row.get(7, LocalDateTime.class),
            row.get(8, Long.class),
            row.get(9, String.class));
    }

    private TargetRow toTargetRow(Row row, RowMetadata metadata) {
        Json swaggerJson = row.get(4, Json.class);
        JsonNode swagger = (swaggerJson) != null ? mapper.readTree(swaggerJson.asArray()) : null;
        return new TargetRow(row.get(0, Long.class),
            row.get(1, String.class),
            row.get(2, String.class),
            row.get(3, String.class),
            swagger,
            convertJsonToMap(row.get(5, Json.class)),
            convertJsonToMap(row.get(6, Json.class)),
            convertStringToUUID(row.get(7, String.class)),
            convertStringToUUID(row.get(8, String.class)),
            row.get(9, LocalDateTime.class),
            row.get(10, LocalDateTime.class),
            row.get(11, Long.class),
            row.get(12, String.class));
    }

    private Map<String, String> convertJsonToMap(Json jsonMap) {
        return (jsonMap != null) ? mapper.readValue(jsonMap.asArray(), Map.class) : null;
    }

    private UUID convertStringToUUID(String id) {
        return (id != null) ? UUID.fromString(id) : null;
    }

    private List<TagDTO> collectTags(List<? extends TagRow> rows) {
        return rows.stream()
            .filter((r) -> r.tagID() != null)
            .map((r) -> new TagDTO(Optional.of(r.tagID()), r.tagName()))
            .toList();
    }
}
