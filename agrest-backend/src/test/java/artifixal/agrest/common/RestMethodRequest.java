package artifixal.agrest.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

/**
 * Utility abstract class for testing REST endpoints.
 */
public abstract class RestMethodRequest<T> {

    private boolean tested;

    private final String url;

    /**
     * Repository to use in tests.
     */
    private R2dbcRepository<Object, Object> testRepo;

    private ResponseSpec testResponse;

    private final HttpStatus expectedStatus;

    private Object request;

    /**
     * Tests to execute after request.
     */
    protected ArrayList<Consumer<T>> tests;

    private Supplier<ResponseSpec> testSupplier;

    private final Class<T> responseType;

    private final HttpHeaders headers;

    public RestMethodRequest(String url, HttpStatus expectedStatus, Class responseType) {
        this.tested = false;
        this.url = url;
        this.expectedStatus = expectedStatus;
        this.tests = new ArrayList<>();
        headers = new HttpHeaders();
        this.testSupplier = defaultTest();
        this.responseType = responseType;
    }

    /**
     * Sends REST request.
     *
     * @param url Where to send request.
     * @param request What to send.
     * @param responseType Type of response.
     * @param headers Request headers.
     *
     * @return Received response.
     */
    protected abstract ResponseSpec sendRequest(String url, Object request, Class<T> responseType, HttpHeaders headers);

    /**
     * Tests if row count changed in DB repository on the request.
     *
     * @param testRepo Repository used to test change.
     * @param expectedChange Count of rows affected. Positive if rows were
     *        added, negative if removed, 0 if request shouldn't affect row count.
     */
    public RestMethodRequest testRowChange(R2dbcRepository testRepo, int expectedChange) {
        this.testRepo = testRepo;
        testSupplier = rowCountTest(expectedChange);
        return this;
    }

    public RestMethodRequest addTest(Consumer<T>... additionalTests) {
        tests.addAll(Arrays.asList(additionalTests));
        return this;
    }

    /**
     * Tests if response has body.
     */
    public RestMethodRequest responseBodyNotNull() {
        tests.add((response) -> assertNotNull(response));
        return this;
    }

    /**
     * Tests if response body is a given instance.
     *
     * @param instance Instance to test for.
     */
    public RestMethodRequest instanceOf(Class instance) {
        tests.add((response) -> assertTrue(response.getClass().isInstance(instance)));
        return this;
    }

    public RestMethodRequest headers(HttpHeaders headers) {
        headers.addAll(headers);
        return this;
    }

    public RestMethodRequest bearerAuth(String token) {
        headers.setBearerAuth(token);
        return this;
    }

    /**
     * Fires request without body.
     *
     * @return Request response.
     */
    public T test() {
        return test(null);
    }

    /**
     * Fires request with given body.
     *
     * @param request Body to send.
     *
     * @return Request response.
     */
    public T test(Object request) {
        if (tested)
            throw new IllegalStateException("Single request instance can be tested only once");
        tested = true;
        this.request = request;
        testResponse = testSupplier.get();
        for (Consumer c : tests)
            c.accept(testResponse);
        return testResponse
            .expectBody(responseType)
            .returnResult()
            .getResponseBody();
    }

    private Supplier<ResponseSpec> defaultTest() {
        return () -> {
            ResponseSpec response = sendRequest(url, request, responseType, headers);
            return response.expectStatus()
                .isEqualTo(expectedStatus.value());
        };
    }

    private Supplier<ResponseSpec> rowCountTest(long exceptedChange) {
        return () -> {
            long beforeRequest = testRepo.count()
                .block();
            // Execute request
            ResponseSpec response = defaultTest().get();
            assertEquals(beforeRequest + exceptedChange, testRepo.count());
            return response;
        };
    }
}