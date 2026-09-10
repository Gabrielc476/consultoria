# Especificação Arquitetural: API Gateway (Spring Cloud Gateway)

**Serviço:** `gateway`  
**Tecnologia:** Java 21 / Spring Boot 3.x / Spring Cloud Gateway (Reativo - Project Reactor)  
**Padrão Arquitetural:** **Clean Architecture (Arquitetura Limpa)** adaptada para Gateway Reativo  
**Porta Padrão:** `8080`  

---

## 1. Por Que Clean Architecture no API Gateway?

Embora muitos gateways sejam tratados apenas como "arquivos de configuração YAML", um API Gateway corporativo para GovTech precisa lidar com:
- Validação estrita de tokens JWT e extração de contexto de Tenant (Consultoria);
- Políticas dinâmicas de Rate Limiting por consultoria/prefeitura;
- Injeção segura de cabeçalhos de segurança e correlação (`X-Tenant-Id`, `X-User-Id`, `X-Correlation-Id`);
- Auditoria de tráfego e logs de conformidade (LGPD).

Aplicar **Clean Architecture** garante que as regras de segurança, tenanting e auditoria fiquem **100% isoladas dos detalhes de implementação do framework**, permitindo testabilidade unitária de regras de acesso sem precisar subir o contexto web do Spring.

---

## 2. Diagrama de Camadas da Clean Architecture no Gateway

```
+---------------------------------------------------------------------------------+
|                           INFRASTRUCTURE LAYER                                  |
|   Spring Cloud Gateway Filters (GlobalFilter), Spring Security Reactive (JJWT) |
|   Reactive Redis (Rate Limiter), WebClient, Logstash / MDC (Correlation ID)     |
|                                                                                 |
|        +---------------------------------------------------------------+        |
|        |                      APPLICATION LAYER                        |        |
|        |   Use Cases: AuthenticateToken, ResolveTenantContext,         |        |
|        |              EnforceRateLimit, AuditRequest                   |        |
|        |   Input/Output Ports (Interfaces abstratas)                   |        |
|        |                                                               |        |
|        |        +---------------------------------------------+        |        |
|        |        |                DOMAIN LAYER                 |        |        |
|        |        |   Entities & Value Objects:                 |        |        |
|        |        |   TenantContext, UserAuthentication,        |        |        |
|        |        |   RoutePolicy, RateLimitQuota               |        |        |
|        |        |   Regras Puras de Validação de Acesso       |        |        |
|        |        +---------------------------------------------+        |        |
|        +-----------------------------------------------+        |        |
+---------------------------------------------------------------------------------+
```

---

## 3. Estrutura de Diretórios e Pacotes

```
gateway/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/govflow/gateway/
    │   │   ├── domain/                               # Regras e Modelos de Domínio Puros
    │   │   │   ├── model/
    │   │   │   │   ├── TenantContext.java            # Value Object (tenantId, tenantName, status)
    │   │   │   │   ├── UserAuthentication.java       # Entidade (userId, username, roles)
    │   │   │   │   ├── RoutePolicy.java              # Regras de autorização por rota
    │   │   │   │   └── RateLimitQuota.java           # Limites por plano de consultoria
    │   │   │   └── exception/
    │   │   │       ├── InvalidTokenException.java
    │   │   │       ├── TenantSuspendedException.java
    │   │   │       └── RateLimitExceededException.java
    │   │   │
    │   │   ├── application/                          # Casos de Uso e Portas (Ports)
    │   │   │   ├── port/
    │   │   │   │   ├── in/                           # Portas de Entrada (Use Cases)
    │   │   │   │   │   ├── AuthenticateRequestUseCase.java
    │   │   │   │   │   ├── ResolveTenantContextUseCase.java
    │   │   │   │   │   └── EnforceRateLimitUseCase.java
    │   │   │   │   └── out/                          # Portas de Saída (SPIs)
    │   │   │   │       ├── TokenDecoderPort.java
    │   │   │   │       ├── RateLimiterPort.java
    │   │   │   │       └── AuditLoggerPort.java
    │   │   │   └── usecase/                          # Implementação dos Use Cases (Lógica de Aplicação)
    │   │   │       ├── AuthenticateRequestService.java
    │   │   │       ├── ResolveTenantContextService.java
    │   │   │       └── EnforceRateLimitService.java
    │   │   │
    │   │   └── infrastructure/                       # Adaptadores e Detalhes de Framework
    │   │       ├── adapter/
    │   │       │   ├── security/                     # Adaptador de Segurança
    │   │       │   │   └── JwtTokenDecoderAdapter.java (implements TokenDecoderPort)
    │   │       │   ├── ratelimit/                    # Adaptador de Rate Limiting
    │   │       │   │   └── RedisRateLimiterAdapter.java (implements RateLimiterPort)
    │   │       │   └── logging/                      # Adaptador de Auditoria
    │   │       │       └── Slf4jAuditLoggerAdapter.java (implements AuditLoggerPort)
    │   │       ├── filter/                           # Filtros Reativos do Spring Cloud Gateway
    │   │       │   ├── AuthenticationGlobalFilter.java
    │   │       │   ├── TenantContextInjectionFilter.java
    │   │       │   ├── CorrelationIdFilter.java
    │   │       │   └── RateLimiterGatewayFilterFactory.java
    │   │       └── config/                           # Configurações do Spring
    │   │           ├── GatewayRoutesConfig.java
    │   │           ├── SecurityConfig.java
    │   │           └── RedisConfig.java
    │   └── resources/
    │       └── application.yml
    └── test/                                         # Testes Unitários de Domínio (sem Spring) e de Integração
```

---

## 4. Detalhamento dos Componentes Chave

### 4.1 Camada de Domínio (Domain)
* **`TenantContext` (Value Object):**
  ```java
  public record TenantContext(UUID tenantId, String tenantCode, boolean active) {
      public void validateActive() {
          if (!active) {
              throw new TenantSuspendedException("Consultoria com acesso suspenso.");
          }
      }
  }
  ```
* **`UserAuthentication` (Entidade de Domínio):**
  ```java
  public record UserAuthentication(UUID userId, String username, Set<String> roles, TenantContext tenant) {
      public boolean hasRole(String role) {
          return roles.contains(role);
      }
  }
  ```

### 4.2 Camada de Aplicação (Application & Ports)
* **Porta de Saída (`TokenDecoderPort`):**
  ```java
  public interface TokenDecoderPort {
      Mono<UserAuthentication> decode(String bearerToken);
  }
  ```
* **Caso de Uso (`AuthenticateRequestUseCase`):**
  ```java
  public interface AuthenticateRequestUseCase {
      Mono<UserAuthentication> execute(String authHeader, String path);
  }
  ```

### 4.3 Camada de Infraestrutura (Infrastructure & Filters)
* **Filtro Reativo do Gateway (`AuthenticationGlobalFilter`):**
  Intercepta toda requisição reativa (`ServerWebExchange`), chama o Caso de Uso de Autenticação e injeta os cabeçalhos internos imutáveis:
  ```java
  @Component
  @Order(-100) // Executa antes do roteamento
  public class AuthenticationGlobalFilter implements GlobalFilter {
      private final AuthenticateRequestUseCase authenticateRequestUseCase;

      public AuthenticationGlobalFilter(AuthenticateRequestUseCase authenticateRequestUseCase) {
          this.authenticateRequestUseCase = authenticateRequestUseCase;
      }

      @Override
      public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          String path = exchange.getRequest().getURI().getPath();
          if (isPublicPath(path)) {
              return chain.filter(exchange);
          }

          String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
          return authenticateRequestUseCase.execute(authHeader, path)
              .flatMap(userAuth -> {
                  // Injeta cabeçalhos sanitizados para os microsserviços internos
                  ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                      .header("X-Tenant-Id", userAuth.tenant().tenantId().toString())
                      .header("X-User-Id", userAuth.userId().toString())
                      .header("X-User-Roles", String.join(",", userAuth.roles()))
                      .build();
                  return chain.filter(exchange.mutate().request(mutatedRequest).build());
              });
      }
  }
  ```

---

## 5. Roteamento Centralizado de Microsserviços

O `GatewayRoutesConfig` define o roteamento reativo para a malha interna do Docker Compose:

| Rota Externa | Microsserviço de Destino | Porta Interna | Filtros Aplicados |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth/**` | `core-service` | `http://core-service:8081` | RateLimiting público, CorrelationID |
| `/api/v1/prefeituras/**` | `core-service` | `http://core-service:8081` | AuthGlobalFilter, TenantContextInjection |
| `/api/v1/convenios/**` | `core-service` | `http://core-service:8081` | AuthGlobalFilter, TenantContextInjection |
| `/api/v1/documentos/**` | `core-service` | `http://core-service:8081` | AuthGlobalFilter, TenantContextInjection |
| `/api/v1/transferegov/**` | `transferegov-service` | `http://transferegov-service:8082` | AuthGlobalFilter, TenantContextInjection |
| `/api/v1/whatsapp/**` | `whatsapp-service` | `http://whatsapp-service:8083` | WebhookFilter (para Evolution) + AuthGlobalFilter |
| `/api/v1/ai/**` | `ai-service` | `http://ai-service:8000` | AuthGlobalFilter, TenantContextInjection |

---

## 6. Tratamento de Erros e Resiliência (Circuit Breaker & Fallback)

1. **RFC 7807 (Problem Details):** Qualquer erro de autenticação ou rate limit gera uma resposta JSON padronizada:
   ```json
   {
     "type": "https://govflow.com.br/errors/unauthorized",
     "title": "Token Inválido ou Ausente",
     "status": 401,
     "detail": "O token fornecido expirou ou não possui assinatura válida.",
     "instance": "/api/v1/convenios"
   }
   ```
2. **Resilience4j Reactive Circuit Breaker:**
   Se o `ai-service` ou o `transferegov-service` sofrerem instabilidade, o Gateway abre o circuito em 5 segundos e retorna resposta degradada elegante (*fallback* rápido) sem travar a interface do usuário.
