package artifixal.agrest.controllers;

import artifixal.agrest.auth.UserRole;
import artifixal.agrest.auth.WithMockToken;
import artifixal.agrest.common.IntegrationTest;
import artifixal.agrest.dto.TargetEntryDTO;
import artifixal.agrest.dto.target.TargetDTO;
import artifixal.agrest.entity.Tag;
import artifixal.agrest.repository.TagRepository;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import artifixal.agrest.repository.TargetBasicRepository;
import artifixal.agrest.services.CsrfService;
import artifixal.agrest.services.PageService;
import artifixal.agrest.services.TargetService;
import com.flipkart.zjsonpatch.Jackson3JsonDiff;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;

/**
 * Integration tests for TargetController.
 */
public class TargetControllerIntegrationTest extends IntegrationTest {

    @Autowired
    private TargetService targetService;

    @Autowired
    private TargetBasicRepository targetRepo;

    @Autowired
    private TagRepository tagRepository;

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void addTarget() {
        final var tags = List.of("tag1", "tag2");
        final TargetDTO newTarget = new TargetDTO("TestTarget", "https://api.example.com",
            Optional.of("Short description"),
            Optional.of(tags),
            Optional.of(Map.of("API-Vesrion", "v1")),
            Optional.of(Map.of("userID", "123456")));
        ClassPathResource swaggerFile = new ClassPathResource("files/exampleSwagger.yaml");

        testTargetAddWithRepoCountChecks(newTarget, Optional.of(swaggerFile), HttpStatus.OK, Long.class, 1,
            tags.size(), true);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotAddTargetMalformedFile() {
        final var tags = List.of("tag3m", "tag4m");
        final TargetDTO newTarget = new TargetDTO("TestMalformedFile", "https://malformed.example.com",
            Optional.of("Short description"),
            Optional.of(tags),
            Optional.of(Map.of("API-Vesrion", "v2m")),
            Optional.of(Map.of("userID", "321")));
        ClassPathResource malformedFile = new ClassPathResource("files/malformedSwagger.yaml");

        testTargetAddWithRepoCountChecks(newTarget, Optional.of(malformedFile), HttpStatus.BAD_REQUEST, Void.class,
            0, 0, true);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotAddTargetBlankName() {
        final TargetDTO malformedName = new TargetDTO("\n \t\r\n ", "https://api.example.com",
            Optional.of("Short description"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

        testTargetAddWithRepoCountChecks(malformedName, Optional.empty(), HttpStatus.BAD_REQUEST, Void.class, 0, 0,
            true);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotAddTargetMalformedURL() {
        final TargetDTO malformedURL = new TargetDTO("Malformed URL", "ab:::cd::/adasd",
            Optional.of("Short description"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

        testTargetAddWithRepoCountChecks(malformedURL, Optional.empty(), HttpStatus.BAD_REQUEST, Void.class, 0, 0,
            true);
    }

    @Test
    @WithMockToken(role = "ROLE_ANALYST")
    public void shouldNotAddTargetAnalystRole() {
        final TargetDTO fineDto = new TargetDTO("TestAnalystUser", "http://forbidden.example.com",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

        testTargetAddWithRepoCountChecks(fineDto, Optional.empty(), HttpStatus.FORBIDDEN, Void.class, 0, 0, true);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotAddTargetNoCsrfToken() {
        final TargetDTO fineDto = new TargetDTO("TestNoCsrfToken", "http://nocsrf.example.com",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

        testTargetAddWithRepoCountChecks(fineDto, Optional.empty(), HttpStatus.FORBIDDEN, Void.class, 0, 0, false);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void editTarget() {
        final String TO_REMOVE = "todelete1";
        final Tag existing = new Tag("existing1");
        tagRepository.save(existing)
            .block();

        final var originalTags = List.of(existing.getName(), TO_REMOVE);
        final TargetDTO original = new TargetDTO("TargetOrg", "https://org.target.com", Optional.empty(),
            Optional.of(originalTags), Optional.empty(), Optional.empty());

        final var updatedTags = List.of(existing.getName(), "tag1", "tag2", "newTag3");
        final TargetDTO updatedData = new TargetDTO("TargetUpdated", "https://update.target.com",
            Optional.of("Example desc"),
            Optional.of(updatedTags),
            Optional.of(Map.of("version", "v1")),
            Optional.of(Map.of("userID", "1234")));

        Long targetID = testTargetEdit(original, updatedData, HttpStatus.OK, true);

        var updatedTarget = targetService.getTargetEntity(targetID)
            .block();
        assertEquals(updatedData.name(), updatedTarget.getName());
        assertEquals(updatedData.url(), updatedTarget.getUrl());
        assertEquals(updatedData.description().get(), updatedTarget.getDescription());
        assertEquals(updatedData.headers().get(), updatedTarget.getHeaders());
        assertEquals(updatedData.cookies().get(), updatedTarget.getCookies());
        assertNotNull(updatedTarget.getEditorID());
        assertNotNull(updatedTarget.getEdited());

        var updatedTargetTags = updatedTarget.getTags()
            .stream()
            .map((tag) -> tag.getName())
            .toList();
        assertTrue(updatedTargetTags.contains(TO_REMOVE) == false, "Tag: \"todelete1\" not deleted");
        assertTrue(updatedTargetTags.containsAll(updatedTags), "Updated tags mismatch");
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotEditTargetBlankName() {
        final TargetDTO original = new TargetDTO("NotBlankName", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
        final TargetDTO update = new TargetDTO("\r \t \n  \t\r", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());

        testTargetEdit(original, update, HttpStatus.BAD_REQUEST, true);
    }

    @Test
    @WithMockToken(role = "ROLE_ANALYST")
    public void shouldNotEditTargetAnalystRole() {
        final TargetDTO original = new TargetDTO("TargetByUser", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
        final TargetDTO update = new TargetDTO("TargetByAnalyst", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());

        testTargetEdit(original, update, HttpStatus.FORBIDDEN, true);
    }

    @Test
    @WithMockToken(role = "ROLE_USER")
    public void shouldNotEditTargetNoCsrfToken() {
        final TargetDTO original = new TargetDTO("MissingCsrfToken", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
        final TargetDTO update = new TargetDTO("CsrfCheckFail", "https://example.target.com", Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());

        testTargetEdit(original, update, HttpStatus.FORBIDDEN, false);
    }

    @Test
    @WithMockToken(role = "ROLE_ANALYST")
    public void getTargetPage() {
        // Populate the page
        final TargetDTO[] entries = {
            new TargetDTO("Target1", "https://target1.com", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Target2", "https://target2.com", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Target3", "https://target3.com", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()),
        };
        // Count existing targets from other tests
        long existingItems = targetRepo.count()
            .block();

        // Insert items as admin
        var task = targetService.addTarget(entries[0], Optional.empty())
            .then(targetService.addTarget(entries[1], Optional.empty()))
            .then(targetService.addTarget(entries[2], Optional.empty()));
        doAs(task, UserRole.ADMIN)
            .block();

        long totalPages = (existingItems + entries.length) / 20 + 1;

        var response = http.get()
            .uri("/v1/targets")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchangeSuccessfully()
            .expectHeader()
            .valueEquals(PageService.TOTAL_COUNT, existingItems + entries.length)
            .expectHeader()
            .valueEquals(PageService.CURRENT_PAGE, 0)
            .expectHeader()
            .valueEquals(PageService.PAGE_SIZE, 20)
            .expectHeader()
            .valueEquals(PageService.TOTAL_PAGES, totalPages)
            .expectBody()
            .returnResult();
        assertNotNull(response.getResponseBody());
        assertNotEquals(0, response.getResponseBody().length);
    }

    @Test
    @WithMockToken(role = "ROLE_ANALYST")
    public void getTargetPageByQuery() {
        // Populate the page
        final TargetDTO[] entries = {
            new TargetDTO("Pattern1", "https://pattern1.com", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Patt2", "https://Pattern2.com", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Not1", "https://ptarget3.com", Optional.of("Pattern3"), Optional.empty(),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Pat3", "https://ptarget4.com", Optional.empty(), Optional.of(List.of("pattern")),
                Optional.empty(), Optional.empty()),
            new TargetDTO("Not2", "https://pnot1.com", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty())
        };
        final int MATCHING_COUNT = 3;
        // Count existing targets from other tests
        long existingItems = targetRepo.count()
            .block();

        // Insert items as admin
        var task = targetService.addTarget(entries[0], Optional.empty())
            .then(targetService.addTarget(entries[1], Optional.empty()))
            .then(targetService.addTarget(entries[2], Optional.empty()))
            .then(targetService.addTarget(entries[3], Optional.empty()))
            .then(targetService.addTarget(entries[4], Optional.empty()));
        doAs(task, UserRole.ADMIN)
            .block();

        long totalPages = (existingItems + entries.length) / 20 + 1;

        var response = http.get()
            .uri("/v1/targets?query=pattern")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchangeSuccessfully()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectHeader()
            .valueEquals(PageService.TOTAL_COUNT, MATCHING_COUNT)
            .expectHeader()
            .valueEquals(PageService.CURRENT_PAGE, 0)
            .expectHeader()
            .valueEquals(PageService.PAGE_SIZE, 20)
            .expectHeader()
            .valueEquals(PageService.TOTAL_PAGES, totalPages)
            .returnResult(TargetEntryDTO.class);

        // Check if our query matched
        StepVerifier.create(response.getResponseBody())
            .expectNextCount(MATCHING_COUNT)
            .thenCancel()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    @WithMockToken(role = "ROLE_ANALYST")
    public void shoulNotGetPageIllegalPageSize() {
        http.get()
            .uri("/v1/targets?size=1000")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    private void testTargetAddWithRepoCountChecks(TargetDTO targetData, Optional<ClassPathResource> swagger,
        HttpStatus expectedStatus, Class responseClass, long expectedChangeTarget, long expectedChangeTags,
        boolean useCsrfToken) {
        long tagCountBefore = tagRepository.count()
            .block();
        long targetCountBefore = targetRepo.count()
            .block();

        testTargetAdd(targetData, swagger, expectedStatus, responseClass, useCsrfToken);

        long tagCountAfter = tagRepository.count()
            .block();
        long targetCountAfter = targetRepo.count()
            .block();

        assertEquals(targetCountBefore + expectedChangeTarget, targetCountAfter);
        assertEquals(tagCountBefore + expectedChangeTags, tagCountAfter);
    }

    private void testTargetAdd(TargetDTO targetData, Optional<ClassPathResource> swagger,
        HttpStatus expectedStatus, Class clazz, boolean useCsrfToken) {
        MultipartBodyBuilder requestBuilder = new MultipartBodyBuilder();
        requestBuilder.part("target", targetData, MediaType.APPLICATION_JSON)
            .header("Content-Disposition", "form-data; name=target");

        if (swagger.isPresent()) {
            requestBuilder.part("swagger", swagger.get());
        }
        String csrfToken = (useCsrfToken) ? getCsrfToken().getValue() : null;

        http.post()
            .uri("/v1/targets")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(requestBuilder.build()))
            .cookie(CsrfService.CSRF_COOKIE, csrfToken)
            .header(CsrfService.CSRF_HEADER, csrfToken)
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus)
            .expectBody(clazz);
    }

    /**
     * @return Original target ID after insert.
     */
    private Long testTargetEdit(TargetDTO originalData, TargetDTO updatedData, HttpStatus exceptedStatus,
        boolean useCsrfToken) {
        final JsonNode originalNode = objectMapper.valueToTree(originalData);
        final JsonNode updateNode = objectMapper.valueToTree(updatedData);

        String patch = Jackson3JsonDiff.asJson(originalNode, updateNode)
            .toString();
        ClassPathResource swaggerFile = new ClassPathResource("files/exampleSwagger.yaml");

        // Add as system so we can test update attemps as a mock user
        Long targetID = doAs(targetService.addTarget(originalData, Optional.empty()), UserRole.ADMIN)
            .block();

        MultipartBodyBuilder requestBuilder = new MultipartBodyBuilder();
        requestBuilder.part("target", patch, MediaType.APPLICATION_JSON)
            .header("Content-Disposition", "form-data; name=target");
        requestBuilder.part("swagger", swaggerFile);

        String csrfToken = (useCsrfToken) ? getCsrfToken().getValue() : null;

        http.patch()
            .uri("/v1/targets/" + targetID)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(requestBuilder.build()))
            .cookie(CsrfService.CSRF_COOKIE, csrfToken)
            .header(CsrfService.CSRF_HEADER, csrfToken)
            .exchange()
            .expectStatus()
            .isEqualTo(exceptedStatus)
            .expectBody();
        return targetID;
    }
}
