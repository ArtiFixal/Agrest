package artifixal.agrest.serializer;

import tools.jackson.databind.annotation.JsonSerialize;

/**
 * MixIn for {@code SecurePassword} serializer.
 */
@JsonSerialize(using = SecurePasswordSerializer.class)
public interface SecurePasswordTestMixIn {

}
