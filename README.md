# Secure Login: Spring Boot + Spring Security + Thymeleaf + MongoDB Atlas

Sistema de login seguro, modular e genérico, com cadastro, login, logout e controle de acesso por
perfis. Foi pensado como base reutilizável: dá para adaptar ao tema do PFC trocando o tema visual e
as regras de perfil, sem reescrever a lógica central.

## Funcionalidades

| Recurso | Como foi implementado |
|---|---|
| Cadastro com validação | Bean Validation (`@NotBlank`, `@Email`, `@Pattern`) + validadores próprios `@StrongPassword` e `@PasswordsMatch` |
| Senhas seguras | Hash **BCrypt** (custo 12, com salt automático). A senha nunca é salva em texto puro |
| Login / Logout | Spring Security (form login). Logout via POST com token CSRF |
| 3 perfis (roles) | `USER` (Usuário), `MANAGER` (Gestor), `ADMIN` (Administrador) |
| Autorização por rota | `SecurityConfig` + `@PreAuthorize` nos controllers (duas camadas) |
| Força bruta | Conta bloqueada por 15 min após 5 senhas erradas (configurável) |
| Sessões no banco | **Spring Session** grava as sessões na coleção `sessions` do MongoDB Atlas |
| Administração | Admin lista usuários, altera perfis, ativa/desativa e desbloqueia contas |
| Revogação de acesso | Ao desativar uma conta ou mudar seus perfis, as sessões ativas dela são encerradas |
| Proteções web | CSRF, Content-Security-Policy, X-Frame-Options, cookie `HttpOnly`/`SameSite`, troca do ID de sessão no login |
| Temas | CSS por variáveis + sobrescrita de templates por tema (`padrao` e `escuro` incluídos) |

### Perfis e rotas

| Rota | Público | USER | MANAGER | ADMIN |
|---|:-:|:-:|:-:|:-:|
| `/`, `/login`, `/cadastro` | ✅ | ✅ | ✅ | ✅ |
| `/painel`, `/usuario/perfil` | ❌ | ✅ | ✅ | ✅ |
| `/gestao` | ❌ | ❌ | ✅ | ✅ |
| `/admin/usuarios` | ❌ | ❌ | ❌ | ✅ |

## Tecnologias

- Java 17
- Spring Boot 3.3 (Web, Security, Data MongoDB, Validation, Thymeleaf)
- Spring Session Data MongoDB
- Thymeleaf Layout Dialect e Thymeleaf Extras Spring Security 6
- MongoDB Atlas (camada gratuita M0)
- Maven
- JUnit 5 e Mockito (testes)

## Estrutura do projeto

```
src/main/java/br/com/securelogin
├── SecureLoginApplication.java      # classe principal
├── config/                          # configurações (segurança, Mongo, temas, admin inicial)
│   ├── SecurityConfig.java          #   regras de acesso, login, logout, cabeçalhos, BCrypt
│   ├── AppProperties.java           #   propriedades customizáveis (app.*)
│   ├── ThemeTemplateConfig.java     #   sistema de temas (sobrescrita de templates)
│   ├── UiSettings.java              #   dados do tema expostos aos templates (@ui)
│   ├── MongoConfig.java             #   auditoria (createdAt/updatedAt)
│   └── AdminInitializer.java        #   cria o 1º administrador
├── domain/                          # entidades (User, Role)
├── repository/                      # acesso ao MongoDB (Spring Data)
├── dto/                             # formulários (RegistrationForm, ChangePasswordForm)
├── validation/                      # validações customizadas (senha forte, confirmação)
├── security/                        # integração com Spring Security
├── service/                         # regras de negócio (UserService, sessões)
├── exception/                       # exceções de negócio
└── controller/                      # telas (Auth, Home, Profile, Manager, Admin)

src/main/resources
├── application.properties           # configuração (sem senhas; lê variáveis de ambiente / .env)
├── templates/                       # páginas Thymeleaf
│   ├── layouts/base.html            #   layout comum a todas as páginas
│   ├── fragments/                   #   cabeçalho, rodapé, mensagens
│   ├── auth/ admin/ manager/ user/ error/
│   └── themes/<tema>/               #   (opcional) arquivos que sobrescrevem os padrões
└── static/
    ├── css/base.css                 # estrutura visual (usa só variáveis CSS)
    └── themes/<tema>/theme.css      # cores e fontes de cada tema
```

A explicação das decisões de arquitetura está em **[docs/ARQUITETURA.md](docs/ARQUITETURA.md)**.

---

## Como executar localmente

### 1. Pré-requisitos

- **Java 17** ou superior ([Adoptium Temurin](https://adoptium.net/))
- **IntelliJ IDEA** (Community serve, já inclui o Maven) **ou** Maven 3.9+ instalado
- **Git**
- Conta gratuita no **[MongoDB Atlas](https://www.mongodb.com/cloud/atlas)**

### 2. Configurar o MongoDB Atlas

1. Crie um cluster gratuito **M0**.
2. Em **Database Access**, crie um usuário de banco com permissão *Read and write to any database*.
   Prefira uma senha só com letras e números. Se usar caracteres especiais (`@`, `:`, `/`, `#`),
   eles precisam ser codificados na URI (por exemplo, `@` vira `%40`).
3. Em **Network Access**, adicione o seu IP ou `0.0.0.0/0` (acesso de qualquer lugar, aceitável
   para desenvolvimento).
4. Em **Database → Connect → Drivers**, copie a string de conexão, algo como:
   ```
   mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=SecureLogin
   ```

> O protocolo `mongodb+srv://` usa **TLS** automaticamente, então a conexão é sempre criptografada.
> O banco (`securelogin`) e as coleções (`users`, `sessions`) são criados sozinhos no primeiro uso.

### 3. Configurar as variáveis (arquivo `.env`)

Na raiz do projeto, copie o modelo e preencha:

```bash
# Windows (PowerShell)
copy .env.example .env
# Linux / macOS
cp .env.example .env
```

Edite o `.env`:

```properties
MONGODB_URI=mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=SecureLogin
MONGODB_DATABASE=securelogin
ADMIN_EMAIL=admin@admin.com
ADMIN_PASSWORD=Troque@Esta1
APP_THEME=padrao
```

O `.env` está no `.gitignore` e **nunca vai para o GitHub**. A aplicação lê esse arquivo
automaticamente (`spring.config.import` no `application.properties`). Também é possível
definir as mesmas chaves como variáveis de ambiente do sistema.

### 4. Executar

**Pelo IntelliJ:** abra a pasta do projeto (*File → Open*), espere o Maven baixar as dependências
e execute a classe `SecureLoginApplication` (botão ▶ ao lado do `main`).

**Pelo terminal (com Maven instalado):**

```bash
mvn spring-boot:run
```

Acesse **http://localhost:8080**.

### 5. Primeiro acesso

- No primeiro start, se não existir nenhum administrador, é criado um com `ADMIN_EMAIL` /
  `ADMIN_PASSWORD`. Se `ADMIN_PASSWORD` estiver vazio, uma senha aleatória é gerada e
  **exibida no console** uma única vez.
- Novos cadastros pela tela `/cadastro` recebem o perfil **Usuário**.
- Para testar os 3 perfis: cadastre duas contas, entre como admin em **Usuários** e dê o perfil
  **Gestor** a uma delas.

### 6. Testes

```bash
mvn test
```

Os testes unitários (`UserServiceTest`, `StrongPasswordValidatorTest`) **não precisam** de conexão
com o Atlas.

---

## Configurações disponíveis

| Variável / propriedade | Padrão | Descrição |
|---|---|---|
| `MONGODB_URI` | *(obrigatória)* | String de conexão do Atlas |
| `MONGODB_DATABASE` | `securelogin` | Nome do banco |
| `ADMIN_NAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `Administrador` / `admin@admin.com` / *(gerada)* | Administrador inicial |
| `APP_NAME` | `Secure Login` | Nome exibido nas páginas |
| `APP_THEME` | `padrao` | Tema visual (`padrao`, `escuro` ou um criado por você) |
| `app.security.max-failed-attempts` | `5` | Tentativas erradas antes do bloqueio |
| `app.security.lock-minutes` | `15` | Duração do bloqueio |
| `spring.session.timeout` | `30m` | Expiração da sessão por inatividade |
| `COOKIE_SECURE` | `false` | Use `true` em produção com HTTPS |
| `PORT` | `8080` | Porta HTTP |

## Adaptando para o seu PFC

1. **Perfis:** edite o enum `Role` (por exemplo, `PROFESSOR`, `ALUNO`, `COORDENADOR`) e ajuste as rotas em
   `SecurityConfig.securityFilterChain`. A tela de administração lista os perfis automaticamente.
2. **Visual:** crie `static/themes/meutema/theme.css` (copie um existente e mude as cores) e defina
   `APP_THEME=meutema`. Para mudar a estrutura HTML, crie `templates/themes/meutema/layouts/base.html`
   ou qualquer outro template com o mesmo caminho do original.
3. **Novas páginas:** crie o controller, o template com `layout:decorate="~{layouts/base}"` e a regra de
   acesso no `SecurityConfig`. Rotas novas já exigem login por padrão.
4. **Dados extras do usuário:** acrescente campos em `User` e em `RegistrationForm`.

Mais detalhes em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Problemas comuns

| Erro | Solução |
|---|---|
| `Could not resolve placeholder 'MONGODB_URI'` | O `.env` não foi criado ou não está na raiz do projeto (a pasta de trabalho da execução precisa ser a raiz) |
| `MongoTimeoutException` / `connection timed out` | Libere o seu IP em **Network Access** no Atlas |
| `Authentication failed` (Mongo) | Usuário ou senha do banco incorretos na URI, ou caracteres especiais sem codificação |
| `Port 8080 already in use` | Feche a outra aplicação ou defina `PORT=8081` no `.env` |
