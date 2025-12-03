package artifixal.agrest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ClientCertificateAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractReactiveVaultConfiguration;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;

/**
 * Configuration of Vault client.
 */
@Configuration
@RequiredArgsConstructor
public class VaultClientConfig extends AbstractReactiveVaultConfiguration{
    
    @Value("${spring.cloud.vault.uri}")
    private String vaultUri;
    
    @Value("${server.ssl.key-store}")
    private String keyStorePath;
    
    @Value("${server.ssl.trust-store}")
    private String trustStorePath;
    
    @Value("${spring.cloud.vault.ssl.key-alias}")
    private String keyAlias;
    
    private final CertStoresData certStoresData;
    
    @Override
    public VaultEndpoint vaultEndpoint(){
        return VaultEndpoint.from(vaultUri);
    }
    
    @Override
    public ClientAuthentication clientAuthentication(){
        return new ClientCertificateAuthentication(restOperations());
    }

    @Override
    public SslConfiguration sslConfiguration(){
        FileSystemResource keyStoreRes=new FileSystemResource(keyStorePath);
        KeyStoreConfiguration keyStoreConf=KeyStoreConfiguration.of(keyStoreRes,
            certStoresData.getKeyStorePassword().toCharArray());
        KeyConfiguration keyConfig=KeyConfiguration.of(null,keyAlias);
        
        FileSystemResource trustStoreRes=new FileSystemResource(trustStorePath);
        return SslConfiguration.forTrustStore(trustStoreRes,
            certStoresData.getTrustStorePassword().toCharArray())
            .withKeyStore(keyStoreConf,keyConfig)
            .withEnabledProtocols("TLSv1.3");
    }
}
