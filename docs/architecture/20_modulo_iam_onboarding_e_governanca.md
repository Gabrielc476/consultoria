# Módulo 1: Identidade, Organização e Acesso (IAM & Onboarding)

## 1. Visão Geral e Responsabilidades
O Módulo de IAM & Onboarding é o Bounded Context responsável por gerenciar a identidade corporativa da Consultoria (Tenant), a governança de acesso de seus colaboradores e o cadastro dos entes municipais convenentes (Prefeituras).

### Principais Dores Solucionadas
1. **Cadastro Remendado & Inexistência de Tabela de Usuários**: Anteriormente, o sistema gerava UUIDs determinísticos diretamente a partir do e-mail sem armazenar senhas ou entidades de usuários no banco de dados. Este módulo introduz a persistência formal de usuários com senhas protegidas por BCrypt.
2. **Separação de Papéis (ADMIN vs AGENTE)**:
   - **ADMIN**: Gestor da Consultoria. Pode cadastrar a consultoria, criar e desativar prefeituras, criar agentes, configurar seus celulares e definir quais prefeituras cada agente pode gerenciar.
   - **AGENTE**: Analista operacional. Possui visão restrita apenas às prefeituras vinculadas a ele. Possui número de celular cadastrado para envio de mensagens/documentos e recebimento de alertas operacionais.
3. **Vínculo Flexível N:N entre Agentes e Prefeituras**: Um agente pode gerenciar múltiplas prefeituras, e uma prefeitura pode ser atendida por múltiplos agentes da mesma consultoria.

---

## 2. Modelo de Dados Relacional (PostgreSQL)

```sql
-- 1. Consultoria (Tenant detentor da assinatura)
CREATE TABLE IF NOT EXISTS core_schema.tb_consultorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cnpj VARCHAR(18) NOT NULL UNIQUE,
    razao_social VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(150) NOT NULL,
    email_contato VARCHAR(150) NOT NULL,
    telefone_contato VARCHAR(20),
    plano VARCHAR(30) NOT NULL DEFAULT 'STARTER', -- 'STARTER', 'PROFESSIONAL', 'ENTERPRISE'
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',   -- 'ATIVO', 'SUSPENSO', 'CANCELADO'
    limite_prefeituras INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Usuários / Agentes do Sistema
CREATE TABLE IF NOT EXISTS core_schema.tb_usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    telefone_celular VARCHAR(30), -- E.164 (ex: +5583999998888) usado para WhatsApp e alertas
    role VARCHAR(30) NOT NULL DEFAULT 'AGENTE', -- 'ADMIN', 'AGENTE'
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuarios_tenant ON core_schema.tb_usuarios(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON core_schema.tb_usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_celular ON core_schema.tb_usuarios(telefone_celular);

-- 3. Prefeituras Atendidas pela Consultoria
CREATE TABLE IF NOT EXISTS core_schema.tb_prefeituras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE,
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    nome_municipio VARCHAR(150) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    codigo_ibge VARCHAR(7) NOT NULL,
    porte_municipio VARCHAR(30) NOT NULL, -- 'PEQUENO_I', 'PEQUENO_II', 'MEDIO', 'GRANDE', 'METROPOLE'
    nome_prefeito VARCHAR(150),
    cpf_prefeito VARCHAR(14),
    inicio_mandato DATE,
    fim_mandato DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prefeitura_cnpj_tenant UNIQUE (tenant_id, cnpj)
);

CREATE INDEX IF NOT EXISTS idx_prefeituras_tenant ON core_schema.tb_prefeituras(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prefeituras_ibge ON core_schema.tb_prefeituras(codigo_ibge);

-- 4. Vínculo N:N entre Agentes e Prefeituras
CREATE TABLE IF NOT EXISTS core_schema.tb_usuario_prefeituras (
    usuario_id UUID NOT NULL REFERENCES core_schema.tb_usuarios(id) ON DELETE CASCADE,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    atribuido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, prefeitura_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_pref_usuario ON core_schema.tb_usuario_prefeituras(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_pref_prefeitura ON core_schema.tb_usuario_prefeituras(prefeitura_id);
```

---

## 3. Endpoints da API REST (Contrato de Interface)

### 3.1 Onboarding e Autenticação
- `POST /api/v1/auth/register-consultoria`:
  - Criação da Consultoria (`tb_consultorias`) e do primeiro usuário `ADMIN`.
  - Body: `{ cnpj, razaoSocial, nomeFantasia, emailContato, telefoneContato, nomeAdmin, emailAdmin, senhaAdmin, celularAdmin }`.
  - Retorno: Token JWT com claims `tenant_id`, `user_id`, `role: ADMIN`.

- `POST /api/v1/auth/login`:
  - Autenticação por e-mail e senha.
  - Verificação de hash BCrypt.
  - Retorno: Token JWT com claims do usuário autenticado.

- `GET /api/v1/auth/me`:
  - Retorna o perfil completo do usuário autenticado, seus papéis e a lista de IDs de prefeituras atribuídas.

### 3.2 Gestão de Prefeituras (Prerrogativa ADMIN)
- `POST /api/v1/prefeituras`:
  - Cadastra nova prefeitura para a consultoria autenticada.
  - Valida unicidade de CNPJ por tenant e código IBGE.
- `GET /api/v1/prefeituras`:
  - Se o usuário for `ADMIN`: retorna todas as prefeituras da consultoria.
  - Se o usuário for `AGENTE`: retorna **apenas as prefeituras vinculadas a ele** via `tb_usuario_prefeituras`.
- `PUT /api/v1/prefeituras/{id}`:
  - Atualiza dados cadastrais da prefeitura.

### 3.3 Gestão de Agentes e Vínculos (Prerrogativa ADMIN)
- `POST /api/v1/agentes`:
  - Cria um novo usuário com `role: AGENTE`.
  - Body: `{ nome, email, senha, telefoneCelular, prefeiturasIds: [UUID] }`.
- `GET /api/v1/agentes`:
  - Lista todos os agentes da consultoria com as prefeituras associadas a cada um.
- `PUT /api/v1/agentes/{id}`:
  - Atualiza nome, telefone celular, status e prefeituras vinculadas.
- `DELETE /api/v1/agentes/{id}`:
  - Desativa o acesso do agente.

---

## 4. Regras de Isolamento e Segurança

1. **Propagação de Contexto do Gateway para o `core-service`**:
   - O `gateway` intercepta as requisições autenticadas e injeta nos headers downstream:
     - `X-Tenant-Id`: ID do tenant da consultoria.
     - `X-User-Id`: ID do usuário autenticado no banco (`tb_usuarios.id`).
     - `X-User-Roles`: Papéis atribuídos ao usuário (ex: `ADMIN` ou `AGENTE`).
   - O `TenantInterceptor` do `core-service` consome esses headers e popula o `UserContext` (ThreadLocal), tornando os dados de identidade disponíveis para a camada de aplicação e domínio.

2. **Filtro Multi-Tenant**: Toda consulta aos dados de convênios, documentos e mensagens deve filtrar estritamente por `tenant_id`.

3. **Filtro de Escopo do Agente**:
   - Se `UserContext.getRole() == 'ADMIN'`: o usuário visualiza e opera todas as prefeituras e convênios cadastrados para a consultoria.
   - Se `UserContext.getRole() == 'AGENTE'`: as consultas de prefeituras e convênios filtram estritamente:
     `WHERE c.prefeitura_id IN (SELECT up.prefeitura_id FROM tb_usuario_prefeituras up WHERE up.usuario_id = :currentUserId)`.
   - Tentativas de acesso direto por ID a recursos de prefeituras não atribuídas resultam em HTTP `403 Forbidden` (`AcessoNegadoException`).

4. **Tratamento e Formato do Celular do Agente**:
   - O número de celular informado para o agente é sanitizado e validado no padrão internacional E.164 (ex: `+5583999998888`).
   - Este número é o canal ativo de identificação no Módulo de Comunicação & WhatsApp (para recebimento/envio de mensagens, áudios, documentos e alertas críticos de prazos).

5. **Armazenamento Seguro de Credenciais**:
   - Senhas são criptografadas utilizando `BCryptPasswordEncoder` com custo de trabalho (work factor) configurado para 12.
   - Nenhuma senha em texto claro é persistida ou exposta em DTOs ou logs de auditoria.

---

## 5. Roteiro de Teste Manual Passo a Passo (Playbook Operacional do Módulo 1)

### 5.1 Teste Manual de Banco de Dados (PostgreSQL & Flyway)
1. **Aplicar a Migração**:
   - Executar o Flyway ou subir o container com `docker compose up -d postgres flyway`.
   - Conectar via `psql` ou DBeaver no banco `govflow_db`.
2. **Checar Criação das Tabelas**:
   ```sql
   \d core_schema.tb_usuarios
   \d core_schema.tb_usuario_prefeituras
   ```
   - Verificar se as foreign keys `REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE` estão presentes.
3. **Validar Constraint Composta em Prefeituras**:
   ```sql
   -- Deve impedir prefeitura com mesmo CNPJ no mesmo tenant, mas permitir se o tenant_id for diferente:
   SELECT conname, contype FROM pg_constraint WHERE conname = 'uk_prefeitura_cnpj_tenant';
   ```

### 5.2 Teste Manual de Cadastro e Autenticação (Frontend & cURL)
1. **Cadastro via Frontend (`/register`)**:
   - Acessar `http://localhost:4200/register`.
   - Preencher:
     - Razão Social: `Planeja Brasil Consultoria Municipal Ltda`
     - Nome Fantasia: `Planeja Brasil`
     - CNPJ: `12.345.678/0001-90`
     - Nome Administrador: `Carlos Eduardo Gestor`
     - E-mail Administrador: `carlos@planejabrasil.com.br`
     - Celular: `(83) 98888-7777`
     - Senha: `GovFlow2026!`
   - Submeter o formulário.
   - *Validação*: Redirecionamento automático para a Home logada com toast de sucesso.
2. **Conferência da Criptografia no Banco**:
   ```sql
   SELECT id, email, senha_hash, role, telefone_celular FROM core_schema.tb_usuarios WHERE email = 'carlos@planejabrasil.com.br';
   ```
   - *Validação*: A coluna `senha_hash` inicia com `$2a$12$` ou `$2b$12$` e não contém a string `GovFlow2026!` em texto claro.
3. **Teste de Rejeição de Senha Errada**:
   - Fazer logout no frontend.
   - Na tela `/login`, tentar autenticar com `carlos@planejabrasil.com.br` e senha `senha_errada_123`.
   - *Validação*: Resposta HTTP `401 Unauthorized`. Toast de alerta exibido na tela ("E-mail ou senha inválidos"). Nenhuma sessão iniciada.
4. **Teste de Sucesso com Senha Correta**:
   - Digitar a senha correta `GovFlow2026!`.
   - *Validação*: HTTP `200 OK` retornando o token JWT. Abrir `jwt.io` e colar o token:
     - Claim `sub`: UUID do usuário.
     - Claim `tenant_id`: UUID da consultoria.
     - Claim `roles`: `["ADMIN"]`.

### 5.3 Teste Manual de Gestão de Agentes e Celular (Prerrogativa ADMIN)
1. **Acessar Gestão da Equipe**:
   - No menu lateral ou superior, clicar em "Agentes & Equipe" (`/admin/agentes`).
   - Clicar no botão "Adicionar Agente".
2. **Preencher Modal de Cadastro**:
   - Nome: `Mariana Analista`
   - E-mail: `mariana@planejabrasil.com.br`
   - Celular (WhatsApp): `+5583991112222` (validar máscara internacional)
   - Senha Inicial: `Agente@2026`
   - Seleção de Prefeituras: Marcar "Prefeitura Municipal de Patos" e "Prefeitura de Sousa" (deixar "Cajazeiras" desmarcada).
   - Salvar.
3. **Verificação no Banco de Vínculos N:N**:
   ```sql
   SELECT u.nome, p.nome_municipio, up.atribuido_em
   FROM core_schema.tb_usuario_prefeituras up
   JOIN core_schema.tb_usuarios u ON u.id = up.usuario_id
   JOIN core_schema.tb_prefeituras p ON p.id = up.prefeitura_id
   WHERE u.email = 'mariana@planejabrasil.com.br';
   ```
   - *Validação*: Devem retornar exatamente 2 linhas (Patos e Sousa).

### 5.4 Teste Manual de Restrição de Escopo (Visão do AGENTE vs Bloqueio 403)
1. **Login como Agente**:
   - Deslogar da conta de Administrador e logar como `mariana@planejabrasil.com.br`.
2. **Verificação do Seletor no Header**:
   - Abrir o dropdown de municípios no topo da aplicação.
   - *Validação*: Apenas "Patos" e "Sousa" aparecem no menu de seleção. "Cajazeiras" não é listada.
3. **Teste de Tentativa de Acesso Indevido (IDOR / Scope Violation)**:
   - Obter o UUID da Prefeitura de Cajazeiras (não atribuída à Mariana).
   - No navegador, tentar acessar a URL direta: `http://localhost:4200/prefeituras/<UUID_CAJAZEIRAS>`.
   - Ou executar via cURL com o Bearer token da Mariana:
     ```bash
     curl -i -X GET http://localhost:8080/api/v1/prefeituras/<UUID_CAJAZEIRAS> \
       -H "Authorization: Bearer <TOKEN_MARIANA>"
     ```
   - *Validação*: A API deve responder categoricamente com HTTP `403 Forbidden` (`AcessoNegadoException`) e o frontend deve apresentar tela/toast de acesso restrito.

---

## 6. Decisões Técnicas e Refinamentos de Implementação

### 6.1 Roteamento e Interceptação no API Gateway (`gateway`)
- **Mapeamento de Rotas**: Toda nova API protegida ou pública no ecossistema precisa estar explicitamente declarada em [`GatewayRoutesConfig.java`](file:///c:/projetos/estudo%20spring/govflow/gateway/src/main/java/br/com/govflow/gateway/infrastructure/config/GatewayRoutesConfig.java). A rota `core-agentes` mapeia `/api/v1/agentes/**` e `/api/v1/agentes` para o `core-service`.
- **Injeção de Identidade**: O `AuthenticationGlobalFilter` autentica o JWT do cliente na porta 8080 e o `TenantContextInjectionFilter` injeta `X-Tenant-Id`, `X-User-Id` e `X-User-Roles` para os serviços internos, mantendo o isolamento multi-tenant.

### 6.2 Deserialização Segura com Records Jackson
- Em DTOs implementados como `record` no Java 21, métodos auxiliares ou getters computados (ex: `celularAdmin()`, `emailAnalista()`, `getPlanoOrDefault()`) devem ser decorados com `@JsonIgnore` para evitar que o Jackson tente instanciá-los como propriedades de construtor sem setter correspondente.

### 6.3 Interface de Associação N:N de Prefeituras a Agentes Existentes
- Foi disponibilizado o componente [`VincularPrefeiturasModalComponent`](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/agentes/components/vincular-prefeituras-modal/vincular-prefeituras-modal.component.ts), acessível a partir do card do agente em `/admin/agentes`.
- O modal permite selecionar e desmarcar prefeituras cadastradas na consultoria a qualquer momento, invocando `PUT /api/v1/agentes/{id}` e sincronizando a tabela associativa `tb_usuario_prefeituras`.

### 6.4 Eliminação de Fallbacks Silenciosos
- Todos os fallbacks de modo demonstrativo que mascaravam erros HTTP reais no frontend durante operações de escrita (cadastro de consultoria, login, cadastro de agente e cadastro de prefeitura) foram eliminados. Erros do backend são expostos fielmente via toasts e banners informativos.
