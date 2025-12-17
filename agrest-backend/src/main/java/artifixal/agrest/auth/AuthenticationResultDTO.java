package artifixal.agrest.auth;

/**
 * DTO with user basic data.
 */
public record AuthenticationResultDTO(String id, String email, int role) {

}
