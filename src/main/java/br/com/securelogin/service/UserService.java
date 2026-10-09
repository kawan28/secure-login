package br.com.securelogin.service;

import br.com.securelogin.config.AppProperties;
import br.com.securelogin.domain.Role;
import br.com.securelogin.domain.User;
import br.com.securelogin.dto.ChangePasswordForm;
import br.com.securelogin.dto.RegistrationForm;
import br.com.securelogin.exception.BusinessException;
import br.com.securelogin.exception.EmailAlreadyUsedException;
import br.com.securelogin.exception.ResourceNotFoundException;
import br.com.securelogin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Regras de negócio relacionadas a usuários.
 *
 * <p>Os controllers nunca acessam o repositório diretamente: toda regra (hash de senha,
 * perfis padrão, proteção do último administrador, bloqueio por tentativas) fica aqui,
 * num único lugar, independente da interface (Thymeleaf hoje, uma API REST amanhã).</p>
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Perfil atribuído a todo novo cadastro público. */
    public static final Role DEFAULT_ROLE = Role.USER;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.Security securityProps;
    private final SessionInvalidationService sessionInvalidation;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AppProperties appProperties, SessionInvalidationService sessionInvalidation) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProps = appProperties.security();
        this.sessionInvalidation = sessionInvalidation;
    }

    /** E-mails são comparados sem diferenciar maiúsculas/minúsculas nem espaços extras. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // ===================================================================== CADASTRO

    /**
     * Cadastra um novo usuário com o perfil padrão ({@link #DEFAULT_ROLE}).
     * A senha é transformada em hash BCrypt antes de ser salva.
     */
    public User register(RegistrationForm form) {
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User(form.getName().trim(), email,
                passwordEncoder.encode(form.getPassword()), EnumSet.of(DEFAULT_ROLE));
        try {
            User saved = userRepository.save(user);
            log.info("Novo usuário cadastrado: id={}", saved.getId());
            return saved;
        } catch (DuplicateKeyException e) {
            // Duas requisições simultâneas com o mesmo e-mail: o índice único do Mongo barra a segunda.
            throw new EmailAlreadyUsedException();
        }
    }

    /**
     * Cria um usuário com perfis específicos (uso interno, ex.: administrador inicial).
     */
    public User createUser(String name, String email, String rawPassword, Set<Role> roles) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException();
        }
        return userRepository.save(new User(name, normalized, passwordEncoder.encode(rawPassword), roles));
    }

    // ===================================================================== CONSULTAS

    public User findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public List<User> findAll() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public boolean adminExists() {
        return userRepository.countByRolesContaining(Role.ADMIN) > 0;
    }

    /** Quantidade de usuários por perfil (usado no painel de gestão). */
    public Map<Role, Long> countByRole() {
        Map<Role, Long> result = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            result.put(role, userRepository.countByRolesContaining(role));
        }
        return result;
    }

    public long countAll() {
        return userRepository.count();
    }

    // ===================================================================== ADMINISTRAÇÃO

    /**
     * Altera os perfis de um usuário.
     *
     * @param actingEmail e-mail de quem está fazendo a alteração (o administrador logado)
     */
    public void updateRoles(String userId, Set<Role> newRoles, String actingEmail) {
        User user = findById(userId);
        Set<Role> roles = (newRoles == null || newRoles.isEmpty()) ? EnumSet.of(DEFAULT_ROLE) : EnumSet.copyOf(newRoles);

        boolean losingAdmin = user.hasRole(Role.ADMIN) && !roles.contains(Role.ADMIN);
        if (losingAdmin) {
            if (user.getEmail().equals(normalizeEmail(actingEmail))) {
                throw new BusinessException("Você não pode remover o seu próprio perfil de administrador.");
            }
            if (userRepository.countByRolesContaining(Role.ADMIN) <= 1) {
                throw new BusinessException("O sistema precisa ter pelo menos um administrador.");
            }
        }
        user.setRoles(roles);
        userRepository.save(user);
        // Força novo login para que as novas permissões passem a valer imediatamente
        sessionInvalidation.invalidateSessionsOf(user.getEmail());
        log.info("Perfis do usuário id={} alterados para {} por {}", userId, roles, actingEmail);
    }

    /** Ativa ou desativa uma conta (contas desativadas não conseguem fazer login). */
    public void setEnabled(String userId, boolean enabled, String actingEmail) {
        User user = findById(userId);
        if (!enabled && user.getEmail().equals(normalizeEmail(actingEmail))) {
            throw new BusinessException("Você não pode desativar a sua própria conta.");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        if (!enabled) {
            sessionInvalidation.invalidateSessionsOf(user.getEmail()); // derruba quem já estava logado
        }
        log.info("Conta id={} {} por {}", userId, enabled ? "ativada" : "desativada", actingEmail);
    }

    /** Remove o bloqueio temporário causado por tentativas de login erradas. */
    public void unlock(String userId) {
        User user = findById(userId);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // ===================================================================== SENHA

    /** Troca de senha pelo próprio usuário, exigindo a senha atual. */
    public void changePassword(String email, ChangePasswordForm form) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(form.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha deve ser diferente da atual.");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.save(user);
        log.info("Senha alterada para o usuário id={}", user.getId());
    }

    // ===================================================================== TENTATIVAS DE LOGIN

    /** Chamado a cada senha errada; bloqueia a conta ao atingir o limite configurado. */
    public void registerFailedLogin(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= securityProps.maxFailedAttempts()) {
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(securityProps.lockMinutes())));
                user.setFailedLoginAttempts(0);
                log.warn("Conta id={} bloqueada por {} min após {} tentativas inválidas",
                        user.getId(), securityProps.lockMinutes(), attempts);
            }
            userRepository.save(user);
        });
    }

    /** Chamado após login bem-sucedido: zera contadores e registra o último acesso. */
    public void registerSuccessfulLogin(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }
}
