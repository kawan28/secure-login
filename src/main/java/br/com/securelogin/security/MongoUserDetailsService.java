package br.com.securelogin.security;

import br.com.securelogin.repository.UserRepository;
import br.com.securelogin.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ensina o Spring Security a buscar usuários no MongoDB.
 *
 * <p>Chamado automaticamente durante o login: o Spring compara a senha digitada com o
 * hash retornado aqui usando o {@code PasswordEncoder} (BCrypt) e verifica se a conta
 * está habilitada e não bloqueada.</p>
 */
@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MongoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(UserService.normalizeEmail(email))
                .map(SecurityUser::new)
                // A mensagem não é exibida ao usuário (o Spring a converte em "credenciais inválidas"),
                // evitando revelar quais e-mails estão cadastrados.
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
