package artifixal.agrest.services;

import artifixal.agrest.exceptions.JsonPatchException;
import com.flipkart.zjsonpatch.Jackson3JsonPatch;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Service used to patching entities.
 */
@Service
@AllArgsConstructor
public class JsonPatchService {

    private final ObjectMapper mapper;

    private static final int MAX_OPERATIONS = 50;
    private static final int MAX_SIZE = 10 * 1024;
    private static final Duration PATCH_TIMEOUT = Duration.ofSeconds(5);

    private static final Set<String> IMMUTABLE_FIELDS = Set.of(
        "/id",
        "/creatorID",
        "/editorID",
        "/created",
        "/edited");

    public <T> Mono<T> applyPatch(String patchJson, T original, Class<T> clazz) {
        return Mono.just(patchJson)
            .map((patch) -> {
                if (patchJson.length() > MAX_SIZE)
                    throw new JsonPatchException("Patch exceeded max size");
                JsonNode patchNode = mapper.readTree(patchJson);
                validatePatch(patchNode);

                JsonNode originalNode = mapper.valueToTree(original);
                JsonNode patched = Jackson3JsonPatch.apply(patchNode, originalNode);
                return mapper.treeToValue(patched, clazz);
            })
            .timeout(PATCH_TIMEOUT)
            .onErrorMap(TimeoutException.class, (e) -> new JsonPatchException("Patch processing exceeded max time"))
            .onErrorMap(NullPointerException.class, (e) -> new JsonPatchException("Malformed patch - missing fields"));
    }

    public void validatePatch(JsonNode patchNode) {
        if (patchNode.size() > MAX_OPERATIONS)
            throw new JsonPatchException("Patch exceeded max operations");
        for (JsonNode operation : patchNode) {
            String op = operation.get("op").asString();
            String path = operation.get("path").asString();
            verifyImmutable(path);
            // prevent from write
            if (op.equals("copy") || op.equals("move")) {
                String to = operation.get("to").asString();
                verifyImmutable(to);
            }
        }
    }

    /**
     * Checks if path exists in {@link #IMMUTABLE_FIELDS} if so throws exception.
     *
     * @param path What to look for
     */
    private void verifyImmutable(String path) {
        if (IMMUTABLE_FIELDS.contains(path))
            throw new JsonPatchException("Can't modify read-only field: " + path);
    }
}
