package artifixal.agrest.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import tools.jackson.databind.JsonNode;

/**
 * Entity class representing scan target.
 */
@Getter
@Setter
@Table("targets")
@NoArgsConstructor
public class Target extends AuditableEntity<Long> {
    private String name;

    /**
     * Base URL to target.
     */
    private String url;
    private String description;

    /**
     * OpenAPI site map.
     */
    private JsonNode swagger;

    /**
     * Map of custom headers included in requests during scan.
     *
     * <p>
     * Key - header name <br>
     * Value - header value
     */
    private Map<String, String> headers;

    /**
     * Map of custom cookies included in requests during scan.
     *
     * <p>
     * Key - cookie name <br>
     * Value - cookie value
     */
    private Map<String, String> cookies;

    /**
     * List of tags related to this target.
     */
    @Transient
    private List<Tag> tags;

    public Target(String name, String url, String description, JsonNode swagger, Map<String, String> headers,
        Map<String, String> cookies) {
        this(null, name, url, description, swagger, headers, cookies);
    }

    public Target(Long id, String name, String url, String description, JsonNode swagger, Map<String, String> headers,
        Map<String, String> cookies) {
        super(id);
        this.name = name;
        this.url = url;
        this.description = description;
        this.swagger = swagger;
        this.headers = headers;
        this.cookies = cookies;
        tags = new ArrayList<>();
    }
}
