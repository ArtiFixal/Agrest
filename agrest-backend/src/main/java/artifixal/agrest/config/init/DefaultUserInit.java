package artifixal.agrest.config.init;

import artifixal.agrest.auth.UserRole;
import artifixal.agrest.dto.user.SecurePassword;
import artifixal.agrest.entity.User;
import artifixal.agrest.repository.UserRepository;
import artifixal.agrest.services.UserService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.ReactiveVaultTemplate;

/**
 * Creates default admin user with password change enforcement.
 */
@Component
@RequiredArgsConstructor
public class DefaultUserInit implements CommandLineRunner{
    
    @Value("${spring.cloud.vault.kv.backend}")
    private String keyValPath;
    
    @Value("${vault.path.app.init}")
    private String appInitPath;
    
    private final UserRepository userRepo;
    private final UserService userService;
    private final ReactiveVaultTemplate vaultTemplate;

    @Override
    public void run(String... args) throws Exception{
        var k2=vaultTemplate.opsForVersionedKeyValue(keyValPath);
        var initCheck=k2
            .get(appInitPath)
            .blockOptional(); 
        if(initCheck.isEmpty())
        {
            byte[] hash=userService.hashPassword(new SecurePassword("NotDefaultAdminPassword1!".getBytes()));
            User admin=new User("admin",hash,UserRole.ADMIN,LocalDateTime.MAX,true,false,true);
            admin.setCreated(LocalDateTime.now());
            UUID systemUserID=UUID.fromString("00000000-0000-0000-0000-000000000000");
            admin.setCreatorID(systemUserID);
            UsernamePasswordAuthenticationToken systemUser=UsernamePasswordAuthenticationToken
                .authenticated(systemUserID,null,
                    Collections.singleton(UserRole.ADMIN.toAuthority()));
            userRepo.save(admin)
                .flatMap((userInit)->{
                    HashMap<String,String> initData=new HashMap<>();
                    initData.put("userInit","true");
                    return k2.put(appInitPath,initData);
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(systemUser))
                .block();
        }   
    }
}
