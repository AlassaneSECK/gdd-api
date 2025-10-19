package alassane.seck.gddapi.controller;

import alassane.seck.gddapi.configuration.JwtUtils;
import alassane.seck.gddapi.entities.User;
import alassane.seck.gddapi.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    // Services de Spring Security utilisés pour authentifier un utilisateur et fabriquer son jeton.
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        try {
            // Spring vérifie ici que le couple email/mot de passe correspond bien à un utilisateur existant.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            // On génère un JWT signé en utilisant l’email comme identifiant principal dans le token.
            String token = jwtUtils.generateToken(authentication.getName());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            user = new User();
            user.setEmail(request.getEmail());
            user.setRole("ROLE_USER");
        } else {
            // Si l’email est déjà utilisé, on bloque l’inscription. Un flux “mot de passe oublié” prendra le relais plus tard.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        // Toujours stocker le mot de passe chiffré avec BCrypt, jamais en clair.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // On renvoie immédiatement un token valide pour permettre à l’utilisateur fraîchement inscrit de se connecter.
        String token = jwtUtils.generateToken(user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthRequest {
        @NotBlank
        @Email
        @Size(max = 255)
        private String email;
        @NotBlank
        @Size(min = 8)
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
    }
}
