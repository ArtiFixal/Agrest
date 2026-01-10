package artifixal.agrest.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity class representing tag.
 */
@Table(name = "tags")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tag extends BaseEntity<Long> {

    private String name;

    public Tag(Long id, String name) {
        super(id);
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ":" + getId();
    }
}
