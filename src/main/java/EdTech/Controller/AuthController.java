package EdTech.Controller;

import EdTech.DTO.AuthDTO;
import EdTech.Entity.UserEntity;
import EdTech.Repository.UserRepository;
import EdTech.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDTO.RegisterRequest req) {
        if (req.name() == null || req.name().isBlank()
                || req.email() == null || !req.email().contains("@")
                || req.password() == null || req.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name, a valid email and a password of 8+ characters are required."));
        }
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered."));
        }
        UserEntity user = new UserEntity();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDTO.AuthResponse(jwtService.generate(user), user.getId(), user.getName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO.LoginRequest req) {
        if (req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
        }
        return userRepository.findByEmail(req.email().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(
                        new AuthDTO.AuthResponse(jwtService.generate(u), u.getId(), u.getName())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password.")));
    }
}