package br.com.securelogin.repository;

import br.com.securelogin.domain.Role;
import br.com.securelogin.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos dados de usuários. O Spring Data gera a implementação automaticamente
 * a partir do nome dos métodos (query methods).
 *
 * <p>Os e-mails são sempre normalizados para minúsculas antes de salvar/consultar
 * (ver {@code UserService#normalizeEmail}), por isso as buscas são exatas.</p>
 */
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado para garantir que sempre exista ao menos um administrador. */
    long countByRolesContaining(Role role);

    List<User> findAllByOrderByCreatedAtDesc();
}
