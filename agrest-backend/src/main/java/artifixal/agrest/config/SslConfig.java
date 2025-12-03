package artifixal.agrest.config;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * App SSL config.
 */
@Configuration
@RequiredArgsConstructor
public class SslConfig implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory>{

    @Value("${server.ssl.key-store}")
    private String keyStorePath;
    
    @Value("${server.ssl.trust-store}")
    private String trustStorePath;
    
    @Value("${server.ssl.key-alias}")
    private String sslKeyAlias;
    
    /**
     * Server telling http client to upgrade to the https.
     */
    private DisposableServer httpServer;
    
    @Value("${server.http.port}")
    private int httpPort;
    
    private final CertStoresData certStoresData;
    
    @SneakyThrows
    @Override
    public void customize(NettyReactiveWebServerFactory factory){
        Ssl ssl=new Ssl();
        ssl.setEnabled(true);
        ssl.setProtocol("TLSv1.3");
        ssl.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});
        ssl.setKeyStore(keyStorePath);
        ssl.setKeyStoreType("PKCS12");
        ssl.setKeyAlias(sslKeyAlias);
        ssl.setKeyStorePassword(certStoresData.getKeyStorePassword());
        ssl.setTrustStore(trustStorePath);
        ssl.setTrustStoreType("PKCS12");
        ssl.setTrustStorePassword(certStoresData.getTrustStorePassword());
        ssl.setClientAuth(Ssl.ClientAuth.NONE);
        factory.setSsl(ssl);
    }
    
    @PostConstruct
    public void startRedirectServer(){
        httpServer=HttpServer.create()
            .port(httpPort)
            .route((routes)->routes.route((request)->true,(request,response)->{
                // Tell client that he attempted to use HTTP
                return response.status(HttpResponseStatus.UPGRADE_REQUIRED)
                    .header(HttpHeaderNames.UPGRADE,"TLS/1.2, HTTP/1.1")
                    .header(HttpHeaderNames.CONNECTION,"Upgrade")
                    .header(HttpHeaderNames.CONTENT_TYPE,MediaType.TEXT_PLAIN_VALUE)
                    .sendString(Mono.just("Upgrade required: You are attempting to use HTTP. Use HTTPS instead."));
            })).bindNow();
    }
    
    @PreDestroy
    public void stopRedirectServer(){
        if(httpServer!=null)
            httpServer.disposeNow();
    }
}
