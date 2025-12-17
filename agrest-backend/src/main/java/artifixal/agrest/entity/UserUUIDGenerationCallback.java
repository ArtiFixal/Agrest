package artifixal.agrest.entity;

import java.util.UUID;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Callback generating UUID for the new {@code User} entities.
 */
@Component
public class UserUUIDGenerationCallback implements BeforeConvertCallback<User> {

    @Override
    public Publisher<User> onBeforeConvert(User entity, SqlIdentifier name) {
        if (entity.getId() == null)
            entity.setId(UUID.randomUUID());
        return Mono.just(entity);
    }
}
