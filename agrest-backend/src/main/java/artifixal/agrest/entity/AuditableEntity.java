package artifixal.agrest.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Base class for entities which will support auditing.
 * 
 * @param <T> ID type.
 */
@Getter
@Setter
public abstract class AuditableEntity<T> extends BaseEntity<T>{

    public AuditableEntity(){
        super(null);
    }

    public AuditableEntity(T id){
        super(id);
    }

    public AuditableEntity(T id,UUID creatorID,UUID editorID,LocalDateTime created,LocalDateTime edited){
        super(id);
        this.creatorID=creatorID;
        this.editorID=editorID;
        this.created=created;
        this.edited=edited;
    }
    
    @CreatedBy
    private UUID creatorID;
    
    @LastModifiedBy
    private UUID editorID;
    
    @CreatedDate
    private LocalDateTime created;
    
    @LastModifiedDate
    private LocalDateTime edited;
}
