# Handoff: Cadastro de Nova Consultoria/Usuário, Nova Prefeitura e Novo Convênio Manual (Onboarding & Ingestão Manual)

> **Destino:** Próximo Agente Antigravity / Desenvolvedor  
> **Foco:** `[TASK-ONBOARDING] Onboarding de Consultoria: Cadastro de Usuário, Nova Prefeitura e Novo Convênio Manual`  
> **Data de Geração:** 2026-09-28  
> **Status Atual do Repositório:** 158/158 testes de backend `core-service` passando (100%) + 71/71 testes de frontend Angular passando (100%). Total monorepo: **314/314 testes automatizados verdes**.

---

## 1. Contexto e Motivação da Demanda

O **GovFlow** foi originalmente arquitetado para que convênios federais fossem ingeridos via pipeline automatizado de dados abertos do Transferegov.br (ETL matinal da TASK-09 no `transferegov-service`). 

No entanto, no cenário de negócio real de uma **nova empresa de consultoria** que fecha contrato com uma **nova prefeitura**:
1. A consultoria precisa criar sua própria conta/usuário de acesso e obter seu `tenant_id` de isolamento.
2. A consultoria precisa cadastrar imediatamente a prefeitura convenente contratante através da interface web do frontend.
3. A consultoria precisa registrar manualmente um convênio ou contrato de repasse recém-assinado ou publicado no DOU (sem esperar a janela de batch da API federal), permitindo que o convênio entre instantaneamente no **Cockpit das 10 Fases**, ative o **Radar CAUC**, receba documentos hábeis do WhatsApp e inicie o cronômetro fatal dos 180 dias da **Cláusula Suspensiva (Fase 02)**.

---

## 2. Diagnóstico Atual: O Que Já Existe vs. O Que Falta Implementar

| Módulo | Backend (`core-service`) | Frontend (`frontend`) | O Que Falta Implementar |
| :--- | :--- | :--- | :--- |
| **Consultoria & Usuário** | `POST /api/v1/consultorias` existe. `POST /api/v1/auth/login` emite JWT de dev. | Tela de Login `/login` com botão de demo e inputs de email/senha. | Aba/botão *"Criar Conta"* no frontend e endpoint de registro de analista associado à consultoria. |
| **Prefeitura Convenente** | `POST /api/v1/prefeituras` implementado com validações de CNPJ e IBGE. | Apenas leitura (`GET /api/v1/prefeituras`) e seletor no Header (`MunicipioContextService`). | Modal interativo *"➕ Nova Prefeitura"* no Header e na listagem de convênios. |
| **Convênio Manual** | `tb_convenios` mapeada no JPA, mas não existe `POST /api/v1/convenios` para cadastro direto pelo analista. | Cockpit (`/convenios`) e Lista (`/convenios/lista`) exibem dados sincronizados ou mockados. | Endpoint `POST /api/v1/convenios`, regras de criação no domínio e modal *"➕ Novo Convênio"* no Cockpit. |

---

## 3. Especificação Detalhada das Novas Funcionalidades

### 3.1 Bloco 1: Cadastro e Onboarding de Consultoria & Analista
- **Backend (`core-service`)**:
  - Endpoint `POST /api/v1/auth/register` (ou extensão de `POST /api/v1/consultorias`):
    - Recebe: `razaoSocial`, `nomeFantasia`, `cnpj`, `emailAnalista`, `senha`, `nomeAnalista`, `telefone`.
    - Cria a entidade `Consultoria` (Tenant) e emite o token JWT com `tenant_id` da nova consultoria.
- **Frontend (`frontend`)**:
  - Na página `/login`, adicionar aba ou alternador: **"Entrar"** e **"Criar Nova Conta"**.
  - Ao registrar, salva a sessão no `AuthService` e redireciona para a tela inicial.

### 3.2 Bloco 2: Modal de Cadastro de Nova Prefeitura Convenente
- **Backend (`core-service`)**:
  - O endpoint `POST /api/v1/prefeituras` **já está pronto**.
  - Payload esperado:
    ```json
    {
      "cnpj": "09.288.665/0001-38",
      "razaoSocial": "Prefeitura Municipal de Esperança",
      "nomeMunicipio": "Esperança",
      "uf": "PB",
      "codigoIbge": "2506004",
      "porteMunicipio": "PEQUENO_I",
      "nomePrefeito": "Nobelino Ferreira",
      "cpfPrefeito": "123.456.789-00",
      "inicioMandato": "2025-01-01",
      "fimMandato": "2028-12-31"
    }
    ```
- **Frontend (`frontend`)**:
  - Criar componente standalone `CadastrarPrefeituraModalComponent`:
    - Validação reativa de CNPJ (com máscara `00.000.000/0000-00`).
    - Validação de UF (2 letras) e Código IBGE (7 dígitos).
    - Seleção de Porte: `PEQUENO_I`, `PEQUENO_II`, `MEDIO`, `GRANDE`, `METROPOLE`.
  - Integrar o botão de abertura nos seguintes locais:
    1. No dropdown do **Seletor de Prefeituras** do `HeaderComponent` (item *"➕ Adicionar Nova Prefeitura"*).
    2. Na página `/convenios/lista`.
  - Ao salvar com sucesso:
    - Invocar `MunicipioContextService.recarregarMunicipios()`.
    - Selecionar imediatamente a nova prefeitura como a prefeitura ativa no contexto (`selecionarMunicipio(novaPrefeitura.id)`).

### 3.3 Bloco 3: Cadastro Manual de Convênios & Injeção na Esteira
- **Backend (`core-service`)**:
  - Criar `CadastrarConvenioUseCase.java` (Inbound Port) e DTO de Request:
    ```java
    public record CadastrarConvenioRequest(
        @NotNull UUID prefeituraId,
        @NotBlank @Pattern(regexp = "^\\d{6}/\\d{4}$") String numeroSiconv, // ex: "954120/2026"
        String numeroProcesso,
        @NotBlank String orgaoConcedente,
        @NotBlank String objeto,
        @NotNull @DecimalMin("0.01") BigDecimal valorGlobal,
        @NotNull @DecimalMin("0.01") BigDecimal valorRepasse,
        @NotNull BigDecimal valorContrapartida,
        boolean possuiClausulaSuspensiva,
        LocalDate prazoClausulaSuspensiva, // se nulo e possuiClausulaSuspensiva=true, aplicar hoje + 180 dias
        LocalDate dataInicioVigencia,
        @NotNull LocalDate dataFimVigencia
    ) {}
    ```
  - Criar `ConvenioController.java`:
    - `POST /api/v1/convenios`: Cadastra e persiste `Convenio` no schema da consultoria autenticada (`X-Tenant-Id`).
    - Se `possuiClausulaSuspensiva == true`: auto-seed dos 3 pilares (`ENGENHARIA_PROJETOS_SINAPI`, `LICENCIAMENTO_AMBIENTAL`, `TITULARIDADE_IMOVEL`) em `tb_condicionantes_suspensivas` e inicializa status como `PENDENTE`.
  - Adicionar `GET /api/v1/convenios` com filtros por `prefeituraId` para permitir listagem dinâmica de convênios cadastrados pelo banco de dados.
- **Frontend (`frontend`)**:
  - Criar componente standalone `CadastrarConvenioModalComponent`:
    - Campos estruturados: Número SICONV (ex: `954120/2026`), Órgão Concedente (ex: `FNDE`, `Ministério das Cidades`), Objeto da Obra/Ação, Valores (Global, Repasse, Contrapartida), Toggle de Cláusula Suspensiva (Sim/Não) e Data Fim de Vigência.
  - Adicionar botão **"➕ Novo Convênio"** no topo da página `/convenios/lista` e no header do `/convenios`.
  - Ao salvar com sucesso:
    - Adicionar o convênio na lista reativa de `ConvenioContextService`.
    - Navegar imediatamente para o Cockpit do convênio criado (`/convenios/:id`).

---

## 4. Roteiro Passo a Passo de Execução

### Fase 1: Backend `core-service`
1. **Ports & Model**:
   - Criar `CadastrarConvenioUseCase.java` e `ConsultarConveniosUseCase.java` em `application/port/in/`.
   - Adicionar método de fábrica `Convenio.novo(...)` em `Convenio.java` com validação de soma de valores (`valorRepasse + valorContrapartida == valorGlobal`) e inicialização dos 180 dias da cláusula suspensiva.
2. **Service & Persistência**:
   - Implementar os use cases em `ConvenioService.java` (ou integrar em `ClausulaSuspensivaService` / novo service específico).
   - Injetar `ConvenioRepositoryPort` e `CondicionanteSuspensivaRepositoryPort`.
3. **Adaptador REST**:
   - Criar `ConvenioController.java` com `@PostMapping("/api/v1/convenios")` e `@GetMapping("/api/v1/convenios")`.
   - Mapear responses com DTOs padronizados e documentação OpenAPI.
4. **Testes Automatizados**:
   - `ConvenioControllerTest.java` (MockMvc cobrindo criação com sucesso, erro de validação e isolamento multi-tenant).
   - Rodar: `mvn test -f services/core-service/pom.xml`.

### Fase 2: Gateway de Rotas (`gateway`)
1. Verificar e garantir a rota `/api/v1/convenios/**` mapeada para o `core-service` no `GatewayRoutesConfig.java`.
2. Rodar: `mvn test -f services/gateway/pom.xml`.

### Fase 3: Frontend Angular 19+ (`frontend`)
1. **Serviços HTTP**:
   - Criar `prefeitura.service.ts` com método `cadastrarPrefeitura(payload: CadastrarPrefeituraPayload): Observable<Prefeitura>`.
   - Criar `convenio.service.ts` com método `cadastrarConvenio(payload: CadastrarConvenioPayload): Observable<ConvenioCockpit>`.
   - Atualizar `api-endpoints.ts` com as rotas correspondentes.
2. **Componentes Modais**:
   - Scaffolding de `cadastrar-prefeitura-modal.component.ts` com Tailwind Obsidian Dark.
   - Scaffolding de `cadastrar-convenio-modal.component.ts` com cálculo em tempo real de contrapartida.
3. **Integração nas Telas**:
   - Inserir botão de *"➕ Nova Prefeitura"* no dropdown de municípios do `HeaderComponent`.
   - Inserir botão de *"➕ Novo Convênio"* no `convenios-list-page.component.ts` e no `convenio-cockpit-page.component.ts`.
4. **Testes Unitários**:
   - Criar specs para os novos modais e serviços.
   - Rodar: `npm test -- --watch=false --browsers=ChromeHeadless`.

### Fase 4: Validação & Build dos Containers
1. `docker compose up -d --build core-service gateway frontend`.
2. Testar manualmente a jornada completa:
   - Acessar `http://localhost:4200`.
   - Cadastrar nova prefeitura (ex: *"Prefeitura de Monteiro - PB"*).
   - Cadastrar novo convênio (ex: *"Convênio SICONV 987654/2026 - Pavimentação Asfáltica"* com Cláusula Suspensiva).
   - Acessar o Cockpit do convênio recém-criado, abrir a **Fase 02** e comprovar os 3 pilares gerados e prontos para aprovação técnica.

---

## 5. Critérios de Aceite da Tarefa

- [ ] **CA-01 (Prefeitura na UI)**: Analista autenticado consegue preencher o formulário no modal e cadastrar uma nova prefeitura, que passa a constar imediatamente no seletor de municípios do Header.
- [ ] **CA-02 (Validação de Prefeitura)**: Rejeição de CNPJs inválidos e validação de código IBGE de 7 dígitos.
- [ ] **CA-03 (Convênio na UI & API)**: Analista consegue registrar um novo convênio informando dados mínimos do SICONV e vincular à prefeitura selecionada.
- [ ] **CA-04 (Auto-seed da Cláusula Suspensiva)**: Caso o convênio possua cláusula suspensiva, os 3 pilares da Caixa GIGOV são automaticamente criados no banco e exibidos no modal da Fase 02.
- [ ] **CA-05 (Isolamento Multi-Tenant)**: A prefeitura e os convênios cadastrados ficam estritamente vinculados ao `tenant_id` da consultoria autenticada.
- [ ] **CA-06 (Zero Regressões)**: 100% dos testes existentes no backend (158+ testes) e frontend (71+ testes) continuam passando com sucesso.

---

## 6. Arquivos Chave de Referência

- [`services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/PrefeituraController.java`](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/PrefeituraController.java)
- [`services/core-service/src/main/java/br/com/govflow/core/domain/model/convenio/Convenio.java`](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/domain/model/convenio/Convenio.java)
- [`services/core-service/src/main/java/br/com/govflow/core/application/service/ClausulaSuspensivaService.java`](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/application/service/ClausulaSuspensivaService.java)
- [`frontend/src/app/core/context/municipio-context.service.ts`](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/core/context/municipio-context.service.ts)
- [`frontend/src/app/features/convenios/services/convenio-context.service.ts`](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/convenios/services/convenio-context.service.ts)
- [`frontend/src/app/features/convenios/pages/convenio-cockpit/convenio-cockpit-page.component.ts`](file:///c:/projetos/estudo%20spring/govflow/frontend/src/app/features/convenios/pages/convenio-cockpit/convenio-cockpit-page.component.ts)
