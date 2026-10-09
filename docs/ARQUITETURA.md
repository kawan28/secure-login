# Arquitetura e decisões de design

Este documento explica como o sistema está organizado, como ele se integra ao MongoDB Atlas e
por que algumas decisões foram tomadas. O objetivo é facilitar a adaptação para outros projetos.

## 1. Visão geral em camadas

```
 Navegador
    │  HTTP (formulários HTML + token CSRF)
    ▼
 Filtros do Spring Security ── autenticação, autorização por rota, CSRF, cabeçalhos
    │
    ▼
 Controllers (controller/) ── recebem a requisição, validam o DTO e escolhem o template
    │
    ▼
 Services (service/) ──────── regras de negócio (hash, perfis, bloqueio, último admin)
    │
    ▼
 Repositories (repository/) ─ Spring Data MongoDB
    │
    ▼
 MongoDB Atlas ────────────── coleções "users" e "sessions" (conexão TLS)
```

Cada camada conhece apenas a de baixo. A interface (Thymeleaf) não contém regra de negócio, e os
services não sabem nada sobre HTML. Por isso a mesma lógica poderia ser reaproveitada numa API REST.

## 2. Fluxo de autenticação

1. O usuário envia o formulário `POST /login` (`email`, `password` e o token CSRF).
2. O `UsernamePasswordAuthenticationFilter` do Spring Security chama o
   `MongoUserDetailsService`, que busca o usuário pelo e-mail.
3. O `DaoAuthenticationProvider` verifica se a conta está **ativa** e **não bloqueada**, e então compara a
   senha digitada com o hash usando `BCryptPasswordEncoder.matches`.
4. Um evento é publicado:
   - sucesso: o `LoginAttemptListener` zera o contador de tentativas e grava `lastLoginAt`;
   - senha errada: o contador aumenta e, ao atingir o limite, `lockedUntil` é preenchido.
5. Em caso de sucesso, o ID da sessão é trocado (proteção contra *session fixation*), o
   `SecurityContext` é salvo na sessão, que o Spring Session persiste no MongoDB, e o usuário
   vai para `/painel`.
6. Em caso de falha, o `LoginFailureHandler` redireciona para `/login?erro=<motivo>`. Para senha
   errada a mensagem é sempre genérica, para não revelar quais e-mails existem.

**Logout:** `POST /logout` invalida a sessão (o documento é removido de `sessions`) e apaga o cookie.

## 3. Autorização (perfis)

- Os perfis ficam no enum `Role` (`USER`, `MANAGER`, `ADMIN`). Um usuário pode ter mais de um perfil
  (`Set<Role>`).
- **Primeira camada:** regras por URL em `SecurityConfig` (`/admin/**` só ADMIN, `/gestao/**`
  MANAGER/ADMIN e assim por diante).
- **Segunda camada:** `@PreAuthorize` em cada controller. Se alguém esquecer uma regra de URL,
  o método continua protegido (defesa em profundidade).
- **Interface:** `sec:authorize` esconde links e cartões, mas isso é só usabilidade; a proteção real
  está nas duas camadas acima.
- **Secure by default:** `anyRequest().authenticated()` faz com que qualquer rota nova exija login.
- Ao mudar os perfis ou desativar um usuário, o `SessionInvalidationService` apaga as sessões dele no
  MongoDB. Assim as novas permissões valem na hora.

Regras de proteção do administrador (em `UserService`):

- o sistema sempre mantém pelo menos um ADMIN;
- o admin não pode remover o próprio perfil ADMIN nem desativar a própria conta.

## 4. Integração com o MongoDB Atlas

| Item | Detalhe |
|---|---|
| Conexão | `spring.data.mongodb.uri=${MONGODB_URI}`. A URI vem de variável de ambiente ou do `.env` (fora do Git) |
| Segurança da conexão | `mongodb+srv://` ativa TLS; o acesso é restrito por usuário/senha do banco e lista de IPs do Atlas |
| Coleção `users` | Documento `User`: nome, e-mail (índice **único**), `passwordHash`, `roles`, `enabled`, `failedLoginAttempts`, `lockedUntil`, `lastLoginAt`, `createdAt`, `updatedAt` |
| Coleção `sessions` | Gerenciada pelo Spring Session. Guarda o `SecurityContext` serializado, com índice TTL que remove sessões expiradas automaticamente |
| Índices | Criados na inicialização (`spring.data.mongodb.auto-index-creation=true`) |
| Auditoria | `@EnableMongoAuditing` preenche `createdAt` e `updatedAt` |

Exemplo de documento em `users`:

```json
{
  "_id": "6703f1...",
  "name": "Maria Silva",
  "email": "maria@email.com",
  "passwordHash": "$2a$12$Qe1...",
  "roles": ["USER"],
  "enabled": true,
  "failedLoginAttempts": 0,
  "lockedUntil": null,
  "lastLoginAt": "2026-10-09T02:15:00Z",
  "createdAt": "2026-10-09T02:10:00Z"
}
```

**Por que guardar as sessões no MongoDB?** Assim a aplicação fica *stateless* em memória: dá para
reiniciar o servidor ou rodar várias instâncias sem derrubar os usuários logados, o que ajuda na
escalabilidade.

## 5. Sistema de temas

O visual foi separado da lógica em dois níveis:

1. **Variáveis CSS:** `static/css/base.css` define a estrutura (grid, cartões, formulários) usando
   apenas variáveis (`--color-primary`, `--font-family`...). Cada tema em
   `static/themes/<tema>/theme.css` define só os valores dessas variáveis.
2. **Sobrescrita de templates:** `ThemeTemplateConfig` registra um resolvedor do Thymeleaf que procura
   primeiro em `templates/themes/<tema>/`. Se o arquivo existir lá, ele é usado; senão, vale o padrão.
   O tema `escuro` usa isso para trocar o rodapé (`templates/themes/escuro/fragments/footer.html`).

Com isso, um tema pode ir de "só trocar as cores" até "trocar o layout inteiro", sem mexer em
controllers. O tema ativo é escolhido por configuração (`APP_THEME`).

Todas as páginas usam o **Thymeleaf Layout Dialect** (`layout:decorate="~{layouts/base}"`). O cabeçalho,
o rodapé e as mensagens são fragmentos reutilizáveis.

## 6. Decisões de segurança

| Decisão | Motivo |
|---|---|
| BCrypt custo 12 | Lento o bastante para inviabilizar força bruta offline, rápido o bastante para o usuário |
| Senha com 8 a 64 caracteres e regras de complexidade | 64 respeita o limite de 72 bytes do BCrypt |
| DTOs separados das entidades | Evita *mass assignment* (o usuário não consegue enviar `roles=ADMIN` no cadastro) |
| E-mail normalizado (minúsculas, sem espaços) e índice único | Evita contas duplicadas como `Maria@x.com` e `maria@x.com` |
| Mensagem genérica no login | Não revela se o e-mail está cadastrado |
| Bloqueio temporário | Limita ataques de força bruta online |
| Logout somente via POST + CSRF | Impede logout forçado por links ou imagens maliciosas |
| CSP sem scripts ou estilos inline | Reduz o impacto de XSS. O Thymeleaf já escapa o HTML com `th:text` |
| `server.error.include-stacktrace=never` | Não expõe detalhes internos em páginas de erro |
| Credenciais fora do código | `.env` e variáveis de ambiente; o `.env` está no `.gitignore` |
| Principal enxuto na sessão (`SecurityUser`) | A sessão guarda só o necessário, e o hash é apagado após o login |

## 7. Como estender

| Quero... | Onde mexer |
|---|---|
| Novo perfil | `Role` + regra em `SecurityConfig` (a tela de admin já o exibe) |
| Nova página protegida | Novo controller com `@PreAuthorize` + template com `layout:decorate` + regra no `SecurityConfig` |
| Novo campo no usuário | `User` + `RegistrationForm` + `templates/auth/register.html` |
| Novo tema | `static/themes/<nome>/theme.css` (+ templates opcionais em `templates/themes/<nome>/`) e `APP_THEME=<nome>` |
| Login social, "lembrar-me" ou recuperação de senha | Acrescentar no `SecurityConfig` (`oauth2Login`, `rememberMe`) ou num novo service, sem alterar o existente |
| API REST | Criar controllers `@RestController` reutilizando o `UserService` |

## 8. Limitações conhecidas

- Não há recuperação de senha por e-mail (exigiria um servidor SMTP).
- O bloqueio por tentativas é por conta, não por IP.
- Em produção, a aplicação deve rodar atrás de HTTPS com `COOKIE_SECURE=true`.
