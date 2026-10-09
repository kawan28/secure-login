package br.com.securelogin.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Documento de usuário persistido na coleção {@code users} do MongoDB Atlas.
 *
 * <p>A senha NUNCA é armazenada em texto puro: o campo {@code passwordHash} guarda
 * apenas o hash BCrypt gerado pelo {@code PasswordEncoder}.</p>
 *
 * <p>Campos extras do seu PFC (ex.: matrícula, telefone) podem ser adicionados aqui
 * sem impacto na lógica de autenticação.</p>
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    /** E-mail é o "username" do sistema; índice único impede duplicidade no banco. */
    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    /** Um usuário pode ter um ou mais perfis. */
    private Set<Role> roles = new HashSet<>();

    /** Permite ao administrador desativar uma conta sem apagá-la. */
    private boolean enabled = true;

    /** Contador de tentativas de login malsucedidas consecutivas (proteção contra força bruta). */
    private int failedLoginAttempts;

    /** Se preenchido e no futuro, a conta está temporariamente bloqueada. */
    private Instant lockedUntil;

    private Instant lastLoginAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public User() {
    }

    public User(String name, String email, String passwordHash, Set<Role> roles) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles == null || roles.isEmpty() ? EnumSet.of(Role.USER) : EnumSet.copyOf(roles);
    }

    /** Indica se a conta está bloqueada neste momento. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    // ---------------- getters e setters ----------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
