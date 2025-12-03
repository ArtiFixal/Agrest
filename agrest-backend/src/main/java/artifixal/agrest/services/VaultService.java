package artifixal.agrest.services;

import artifixal.agrest.dto.vault.Eraseable;
import artifixal.agrest.dto.vault.KeyPairDTO;
import artifixal.agrest.dto.vault.PepperDTO;
import artifixal.agrest.dto.vault.SecureSecret;
import artifixal.agrest.dto.vault.SingleKeyDTO;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.core.SecretNotFoundException;
import org.springframework.vault.support.Versioned;
import org.springframework.vault.support.Versioned.Metadata;
import reactor.core.publisher.Mono;

/**
 * Service related to HashiCorp Vault operations.
 */
@Service
public class VaultService {
    
    @Value("${spring.cloud.vault.kv.backend}")
    private String keyValPath;
    
    @Value("${spring.cloud.vault.kv.cache.maxttl}")
    private int cacheMaxTTL;
    
    @Value("${spring.cloud.vault.kv.cache.ttl}")
    private int cacheMinTTL;
    
    @Value("${spring.cloud.vault.kv.cache.size}")
    private int cacheSize;
    
    private record TypedKey(String path,Class<? extends Eraseable> type){};
    
    private final ReactiveVaultTemplate vaultTemplate;
    private AsyncLoadingCache<TypedKey,Versioned<? extends Eraseable>> vaultCache;

    public VaultService(ReactiveVaultTemplate vaultTemplate){
        this.vaultTemplate=vaultTemplate;
    }
    
    @PostConstruct
    private void initCache(){
        vaultCache=Caffeine.newBuilder()
            .refreshAfterWrite(Duration.ofMinutes(cacheMinTTL))
            .expireAfterWrite(Duration.ofMinutes(cacheMaxTTL))
            .maximumSize(cacheSize)
            .scheduler(Scheduler.systemScheduler())
            .removalListener((key,value,cause)->{
                if(value!=null)
                    ((Versioned<Eraseable>)value).getData()
                        .clear();
            })
            .buildAsync((key,executor)->{
                return vaultTemplate.opsForVersionedKeyValue(keyValPath)
                    .get(key.path(),key.type())
                    .toFuture();
            });
    }
    
    public Mono<Metadata> writeSecret(String path,String key,char[] value){
        return vaultTemplate.opsForVersionedKeyValue(keyValPath)
            .put(path,key);
    }
    
    public Mono<Versioned<SecureSecret>> readSecret(String path){
        return getOrFetch(path,SecureSecret.class);
    }
    
    private<T extends Eraseable> Mono<Versioned<T>> getOrFetch(String path,Class<T> clazz){
        TypedKey key=new TypedKey(path,clazz);
        return Mono.fromFuture(vaultCache.get(key))
            .onErrorResume((err)->Mono.error(
                 new SecretNotFoundException("Missing secret from Vault|"+err,path))
            ).map((result)->{
                return (Versioned<T>)result;
            });
    }
    
    public Mono<Versioned<SingleKeyDTO>> getCsrfKey(){
         return getOrFetch("agrest-app/csrf",SingleKeyDTO.class);
    }
    
    public Mono<Versioned<KeyPairDTO>> getPasetoKeys(){
        return getOrFetch("agrest-app/paseto-keys",KeyPairDTO.class);
    }
    
    public Mono<Versioned<PepperDTO>> getPepper(){
        return getOrFetch("agrest-app/pepper",PepperDTO.class);
    }
}
