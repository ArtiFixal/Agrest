package artifixal.agrest.repository;

import artifixal.agrest.entity.User;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository related to users.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    Mono<User> findByEmail(String email);
}
