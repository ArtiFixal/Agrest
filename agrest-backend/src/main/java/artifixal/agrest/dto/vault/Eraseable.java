package artifixal.agrest.dto.vault;

/**
 * Interface allowing to erase secret data.
 */
public interface Eraseable {
    
    /**
     * Zeroes secret data.
     */
    public void clear();
}
