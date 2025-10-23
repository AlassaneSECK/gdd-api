package alassane.seck.gddapi.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Représente l'utilisateur authentifié courant avec son identifiant interne.
 * <p>
 * Grâce à cette classe, on expose à la fois les informations habituelles de Spring Security (email,
 * mots de passe, rôles) et l'identifiant technique (`id`). Cela permet aux couches supérieures
 * (contrôleurs, services) de travailler directement avec la clé primaire sans devoir requêter la base
 * à chaque appel pour retrouver l'utilisateur courant.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long id,
                              String email,
                              String password,
                              Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
