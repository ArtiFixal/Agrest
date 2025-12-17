package artifixal.agrest.exceptions;

import lombok.Getter;

/**
 * Exception related to missing entity.
 */
@Getter
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object entityID;

    public EntityNotFoundException(String entityName, Object entityID) {
        super(entityName + " with ID " + entityID + " not found");
        this.entityID = entityID;
        this.entityName = entityName;
    }
}