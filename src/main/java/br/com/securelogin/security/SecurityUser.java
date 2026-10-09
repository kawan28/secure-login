package br.com.securelogin.security;

import br.com.securelogin.domain.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre o nosso {@link User} (documento do MongoDB) e a interface
 * {@link UserDetails} exigida pelo Spring Security.
 *
 * <p>Por que não usar o próprio {@code User}? Porque o principal autenticado é
 * serializado dentro da sessão (que fica no MongoDB). Guardar só os dados essenciais
 * mantém a sessão pequena e evita vazar dados desnecessários. Após o login o hash da
 * senha é apagado da memória ({@link #eraseCredentials()}).</p>
 */
public class SecurityUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String email;
    private String password;
    private final List<SimpleGrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
        this.enabled = user.isEnabled();
        this.accountNonLocked = !user.isLocked();
    }

    public String getId() { return id; }

    /** Nome exibido na interface (ex.: "Olá, Maria"). */
    public String getName() { return name; }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
