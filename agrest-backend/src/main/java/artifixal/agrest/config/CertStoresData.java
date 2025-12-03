package artifixal.agrest.config;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * KeyStore and TrustStore data.
 */
@Configuration
public class CertStoresData {

    @Value("${secret.keystore.path}")
    private String keyStorePassPath;
    
    @Value("${secret.truststore.path}")
    private String trustStorePassPath;
    
    private String keyStorePassword;
    private String trustStorePassword;
    
    @PostConstruct
    public void init(){
        keyStorePassword=readPassword(keyStorePassPath);
        trustStorePassword=readPassword(trustStorePassPath);
    }
    
    private String readPassword(String filePath){
        final File passwordFile=new File(filePath);
        try(Scanner in=new Scanner(passwordFile)){
            return in.nextLine();
        }catch(FileNotFoundException ex){
            throw new RuntimeException("File with SSL secret not found: "+filePath);
        }finally{
            passwordFile.delete();
        }
    }

    public String getKeyStorePassword(){
        return keyStorePassword;
    }

    public String getTrustStorePassword(){
        return trustStorePassword;
    }
}
