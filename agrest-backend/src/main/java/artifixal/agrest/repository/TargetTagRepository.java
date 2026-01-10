package artifixal.agrest.repository;

import artifixal.agrest.entity.TargetTag;
import artifixal.agrest.entity.TargetTagKey;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * R2DBC repository related to many-to-many relationship between tags and targets.
 */
@Repository
public interface TargetTagRepository extends R2dbcRepository<TargetTag, TargetTagKey> {

}
