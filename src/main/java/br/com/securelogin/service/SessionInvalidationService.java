package br.com.securelogin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

/**
 * Encerra as sessões ativas de um usuário (que ficam na coleção {@code sessions} do MongoDB).
 *
 * <p>Usado quando o administrador desativa uma conta ou altera seus perfis: sem isso,
 * o usuário continuaria logado com as permissões antigas até a sessão expirar.</p>
 */
@Service
public class SessionInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(SessionInvalidationService.class);

    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessionRepository;

    public SessionInvalidationService(
            ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /** Remove todas as sessões cujo usuário autenticado é {@code email}. */
    public void invalidateSessionsOf(String email) {
        FindByIndexNameSessionRepository<? extends Session> repository = sessionRepository.getIfAvailable();
        if (repository == null) {
            return; // sessões não estão no banco (ex.: em alguns testes)
        }
        var sessions = repository.findByPrincipalName(email);
        sessions.keySet().forEach(repository::deleteById);
        if (!sessions.isEmpty()) {
            log.info("{} sessão(ões) encerrada(s) para o usuário {}", sessions.size(), email);
        }
    }
}
