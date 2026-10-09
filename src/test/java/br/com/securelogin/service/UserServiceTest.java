package br.com.securelogin.service;

import br.com.securelogin.config.AppProperties;
import br.com.securelogin.domain.Role;
import br.com.securelogin.domain.User;
import br.com.securelogin.dto.RegistrationForm;
import br.com.securelogin.exception.BusinessException;
import br.com.securelogin.exception.EmailAlreadyUsedException;
import br.com.securelogin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários das regras de negócio de {@link UserService}.
 * O repositório é simulado com Mockito, então não é preciso conexão com o MongoDB.
 */
class UserServiceTest {

    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder(4); // custo baixo só para os testes rodarem rápido
        AppProperties props = new AppProperties("Teste", null, new AppProperties.Security(3, 15), null);
        service = new UserService(repository, encoder, props, mock(SessionInvalidationService.class));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegistrationForm form(String email) {
        RegistrationForm f = new RegistrationForm();
        f.setName("  Maria Silva ");
        f.setEmail(email);
        f.setPassword("Senha@123");
        f.setConfirmPassword("Senha@123");
        return f;
    }

    @Test
    void cadastroSalvaHashNormalizaEmailEAtribuiPerfilPadrao() {
        User saved = service.register(form("  Maria@Email.COM "));

        assertEquals("maria@email.com", saved.getEmail());
        assertEquals("Maria Silva", saved.getName());
        assertEquals(EnumSet.of(Role.USER), saved.getRoles());
        assertNotEquals("Senha@123", saved.getPasswordHash());       // nunca em texto puro
        assertTrue(encoder.matches("Senha@123", saved.getPasswordHash()));
    }

    @Test
    void cadastroRecusaEmailDuplicado() {
        when(repository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThrows(EmailAlreadyUsedException.class, () -> service.register(form("maria@email.com")));
        verify(repository, never()).save(any());
    }

    @Test
    void bloqueiaContaAposAtingirLimiteDeTentativas() {
        User user = new User("Maria", "maria@email.com", "hash", EnumSet.of(Role.USER));
        when(repository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));

        service.registerFailedLogin("maria@email.com");
        service.registerFailedLogin("maria@email.com");
        assertEquals(2, user.getFailedLoginAttempts());

        service.registerFailedLogin("maria@email.com"); // 3ª tentativa = limite configurado
        assertTrue(user.isLocked());
        assertNotNull(user.getLockedUntil());
    }

    @Test
    void loginComSucessoZeraTentativas() {
        User user = new User("Maria", "maria@email.com", "hash", EnumSet.of(Role.USER));
        user.setFailedLoginAttempts(2);
        when(repository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));

        service.registerSuccessfulLogin("maria@email.com");

        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void naoPermiteRemoverUltimoAdministrador() {
        User admin = new User("Admin", "admin@email.com", "hash", EnumSet.of(Role.ADMIN));
        admin.setId("1");
        when(repository.findById("1")).thenReturn(Optional.of(admin));
        when(repository.countByRolesContaining(Role.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> service.updateRoles("1", EnumSet.of(Role.USER), "outro@email.com"));
    }

    @Test
    void adminNaoPodeDesativarAPropriaConta() {
        User admin = new User("Admin", "admin@email.com", "hash", EnumSet.of(Role.ADMIN));
        admin.setId("1");
        when(repository.findById("1")).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class, () -> service.setEnabled("1", false, "admin@email.com"));
    }
}
