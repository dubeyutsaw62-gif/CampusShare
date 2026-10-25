package EdTech.DTO;

public class AuthDTO {
    // DTO/AuthDTOs.java  (3 records in one file is fine; or split them)
    public record RegisterRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record AuthResponse(String token, Integer userId, String name) {}
}
