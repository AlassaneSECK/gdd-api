package alassane.seck.gddapi.service;

import alassane.seck.gddapi.entities.User;
import alassane.seck.gddapi.repository.UserRepository;
import alassane.seck.gddapi.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Spring Security délègue ici la récupération de l'utilisateur persistant (pour vérifier le mot de passe et charger les rôles).
        User user = userRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }

        // On encapsule l'utilisateur dans `AuthenticatedUser` afin de conserver son identifiant interne.
        // Cela évite de repasser par une requête SQL pour retrouver l'id à chaque fois que l'on consomme
        // le principal dans les contrôleurs (ex: budget).
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
