package artifixal.agrest.repository;

import artifixal.agrest.entity.Target;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Basic R2DBC repository for {@code Target}
 */
@Repository
public interface TargetBasicRepository extends R2dbcRepository<Target, Long> {

}
