package artifixal.agrest.common;

import artifixal.agrest.dto.user.SecurePassword;
import artifixal.agrest.dto.user.UserAuthenticationDTO;
import artifixal.agrest.serializer.SecurePasswordSerializer;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Base class for integration tests.
 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected WebTestClient http;
    
    @BeforeEach
    public void setUp() throws SSLException{
        var sslContext=SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        http=WebTestClient.bindToServer()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .secure((t)->t.sslContext(sslContext))
            ))
            .codecs((codecs)->{
                SimpleModule mod=new SimpleModule("TestSecurePasswordMod");
                mod.addSerializer(SecurePassword.class,new SecurePasswordSerializer());
                
                JsonMapper mapper=JsonMapper.builder()
                    .addModule(mod)
                    .build();
                
                JacksonJsonEncoder encoder=new JacksonJsonEncoder(mapper);
                codecs.customCodecs()
                    .register(encoder);
            })
            .baseUrl(getHostURL())
            .build();
    }
    
    protected String getHostURL(){
        return "https://localhost:"+port;
    }
    
    /**
     * Creates GET request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * 
     * @return REST GET request.
     */
    protected RestMethodRequest get(String mappingPath,HttpStatus expectedStatus){
        return get(mappingPath,expectedStatus,byte[].class);
    }
    
    //private 
    
    /**
     * Creates GET request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * @param responseType Response body type.
     * 
     * @return REST GET request.
     */
    protected RestMethodRequest get(String mappingPath,HttpStatus expectedStatus,Class responseType){
        return new RestMethodRequest(mappingPath,expectedStatus,responseType){
            @Override
            protected ResponseSpec sendRequest(String mappingPath,Object request,Class responseType,HttpHeaders headers){
                return http.get()
                    .uri(mappingPath)
                    .headers((t)->{
                        t.addAll(headers);
                    })
                    .exchange();
            }
        };
    }
    
    /**
     * Creates POST request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * 
     * @return REST POST request.
     */
    protected RestMethodRequest post(String mappingPath,HttpStatus expectedStatus){
        return post(mappingPath,expectedStatus,byte[].class);
    }
    
    /**
     * Creates POST request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * @param responseType Response body type.
     * 
     * @return REST POST request.
     */
    protected RestMethodRequest post(String mappingPath,HttpStatus expectedStatus,Class responseType){
        return new RestMethodRequest(mappingPath,expectedStatus,responseType){
            @Override
            protected ResponseSpec sendRequest(String url,Object request,Class responseType,HttpHeaders headers){
                return http.post()
                    .uri(mappingPath)
                    .headers((t)->{
                        t.addAll(headers);
                    })
                    .bodyValue(request)
                    .exchange();
            }
        };
    }
    
    /**
     * Creates PUT request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * 
     * @return REST PUT request.
     */
    protected RestMethodRequest put(String mappingPath,HttpStatus expectedStatus){
        return put(mappingPath,expectedStatus,byte[].class);
    }
    
    /**
     * Creates PUT request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * @param responseType Response body type.
     * 
     * @return REST PUT request.
     */
    protected RestMethodRequest put(String mappingPath,HttpStatus expectedStatus,Class responseType){
        return new RestMethodRequest(mappingPath,expectedStatus,responseType){
            @Override
            protected ResponseSpec sendRequest(String mappingPath,Object request,Class responseType,HttpHeaders headers){
                return http.put()
                    .uri(mappingPath)
                    .headers((t)->{
                        t.addAll(headers);
                    })
                    .bodyValue(request)
                    .exchange();
            }
        };
    }
    
    /**
     * Creates PATCH request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * 
     * @return REST PATCH request.
     */
    protected RestMethodRequest patch(String mappingPath,HttpStatus expectedStatus){
        return patch(mappingPath,expectedStatus,byte[].class);
    }
    
    /**
     * Creates PATCH request object with given path to request and status to test.
     * 
     * @param mappingPath Path at which anotated controller method is available.
     * @param expectedStatus Status to test for.
     * @param responseType Response body type.
     * 
     * @return REST PATCH request.
     */
    protected RestMethodRequest patch(String mappingPath,HttpStatus expectedStatus,Class responseType){
        return new RestMethodRequest(mappingPath,expectedStatus,responseType){
            @Override
            protected ResponseSpec sendRequest(String mappingPath,Object request,Class responseType,HttpHeaders headers){
                return http.patch()
                    .uri(mappingPath)
                    .headers((t)->{
                        t.addAll(headers);
                    })
                    .bodyValue(request)
                    .exchange();
            }
        };
    }
    
    protected String login(String email,String password){
        final UserAuthenticationDTO loginCredentials=
            new UserAuthenticationDTO(email,new SecurePassword(password.getBytes()));
        
        return (String)post("/v1/auth/login",HttpStatus.OK,String.class)
            .responseBodyNotNull()
            .test(loginCredentials);
    }
    
    protected HttpCookie getCsrfToken(){
        var result=http.get()
            .uri("/v1/csrf")
            .exchange()
            .expectStatus().isOk()
            .expectCookie()
            .exists("csrf")
            .expectBody()
            .returnResult();
        return result.getResponseCookies()
            .getFirst("csrf");
    }
    
    protected String loginAs(String userRole){
        return "";
    }
}
