package artifixal.agrest.repository;

import artifixal.agrest.entity.Tag;
import java.util.Collection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for tags.
 */
@Repository
public interface TagRepository extends R2dbcRepository<Tag, Long> {

    Mono<Tag> findByName(String name);

    Flux<Tag> findAllByNameIn(Collection<String> names);

    /**
     * Fetches tag page.
     *
     * @param page Which page to get.
     *
     * @return Flux containing fetched tags.
     */
    Flux<Tag> findByOrderByName(Pageable page);

    @Query("SELECT t.* FROM tags t JOIN targets_tags tt ON t.id=tt.tag_id WHERE tt.target_id=$1")
    Flux<Tag> findAllByTargetID(Long targetID);

    /**
     * Deletes all tags which are not connected to any target.
     *
     * @return Deleted tag count.
     */
    @Modifying
    @Query("DELETE t FROM tags LEFT JOIN targets_tags tt ON t.id=tt.tag_id WHERE tt.tag_id IS NULL")
    Mono<Long> deleteAllOrphaned();
}
