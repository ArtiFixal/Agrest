package artifixal.agrest.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing many-to-many relationship between tags and targets.
 */
@Getter
@Setter
@Table(name = "targets_tags")
public class TargetTag implements Persistable<TargetTagKey> {

    @Id
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private TargetTagKey id;

    @CreatedBy
    private UUID creatorID;

    @CreatedDate
    private LocalDateTime created;

    public TargetTag(TargetTagKey id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        // Entity will always be newly created, as there is no fields to update.
        return true;
    }
}
