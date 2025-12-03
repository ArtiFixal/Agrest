package artifixal.agrest.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;

/**
 * Base class for entities.
 * 
 * @param <T> ID type.
 */
@Getter
@Setter
public abstract class BaseEntity<T> implements Persistable<T>{
    
    @Id
    private T id;

    public BaseEntity(){
        id=null;
    }

    public BaseEntity(T id){
        this.id=id;
    }

    @Override
    public boolean isNew(){
        return id==null;
    }
}
