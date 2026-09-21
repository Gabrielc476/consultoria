# Handoff: Transição para TASK-08 — Extensão Chrome (Detecção do Portal Transferegov e Injeção no DOM)

## 1. Resumo Executivo & Estado Atual do Repositório

- **Última Entrega Concluída (TASK-07 — Frontend Angular):**
  - **Tela de Conferência Lado a Lado (Side-by-Side Review) 100% implementada:**
    - Arquitetura **Feature-Sliced Design (FSD)** em **Angular 19/20 Standalone** com **Signals puros**.
    - Estilização de alta densidade operacional (cockpit fiscal) com **Tailwind CSS**, tokens customizados e **Lucide Icons**.
    - Visualizador de mídia híbrido (**PDF via `ngx-extended-pdf-viewer`** ou **Imagem JPEG/PNG com zoom**) ocupando 50% da tela.
    - Camada overlay SVG/HTML responsiva renderizando **Bounding Boxes** da IA sobre a mídia com coordenadas percentuais dinâmicas.
    - **Reflexo Bi-direcional Sincronizado (Spotlight):** hover/foco em campos do formulário ativa glow azul cobalto na caixa correspondente do PDF; clique na caixa do PDF foca o campo do formulário.
    - Formulário estruturado com **semáforos de confiança da IA** (verde >90%, amarelo 70-90%, vermelho <70%), recálculo reativo de deduções, alerta de divergência matemática e botão de 1-clique para balancear o valor líquido (`Alt + S`).
    - **Fila contínua com Drawer lateral retrátil (`Alt + Q`)**: avanço automático instantâneo para o próximo documento pendente após aprovação (`Ctrl + Enter`) ou rejeição (`Alt + R` / `Esc`).
    - **Tela formal de login (`/login`)** integrada com `POST /api/v1/auth/login` no Core Service, gerando JWT, persistindo sessão no `AuthService` e interceptando requisições com Bearer token.
    - **Gateway & Core Service integrados:**
      - Gateway roteia `/api/v1/documentos/**` para o `core-service` com CORS habilitado para `http://localhost:4200` e bypass de preflight `OPTIONS`.
      - Core Service disponibiliza `GET /api/v1/documentos/{id}/arquivo` com streaming binário seguro do MinIO S3 (e fallback automático para testes).
    - **100% dos testes passando:**
      - 15 testes unitários do frontend (`npm test -- --watch=false --browsers=ChromeHeadless`).
      - 94 testes automatizados do `core-service` (`mvn test`).
      - 20 testes do `gateway` (`mvn test`).
      - `npm run build` do frontend gerando bundle de produção com sucesso.

---

## 2. Foco da Próxima Sessão: [TASK-08]

### Objetivo da TASK-08
Implementar a **Extensão Oficial do Google Chrome** (Manifest V3 em TypeScript):
1. **Detecção Inteligente:**
   - Detectar quando o analista da consultoria está navegando na tela oficial de "Incluir Documento Hábil" do portal `transferegov.sistema.gov.br`.
2. **Injeção Não-Invasiva em Shadow DOM Fechado:**
   - Injetar um painel flutuante encapsulado em `ShadowRoot` (`mode: 'closed'`), blindando o layout e botões da extensão contra qualquer conflito de CSS com o portal do governo.
3. **Preenchimento em 1 Clique (DOM Injector Strategy):**
   - Buscar o documento aprovado (`PRONTO_PARA_TRANSFEREGOV`) no GovFlow via API Gateway.
   - Preencher os inputs oficiais do governo (número NF, data, credor, valores, retenções) disparando sinteticamente os eventos `input` e `change`.
4. **Contingência Operacional (Clipboard Fallback):**
   - Disponibilizar botões de cópia rápida individual ao lado de cada dado caso o layout do portal do governo sofra alterações estruturais.

---

## 3. Artefatos de Referência no Repositório

- **Especificação da TASK-08 no Backlog:** [docs/clickup_tasks_backlog.md](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L141-L156)
- **Arquitetura da Extensão (Shadow DOM & Injeção):** [docs/architecture/06_arquitetura_frontend_e_extensao.md](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/06_arquitetura_frontend_e_extensao.md#L101-L202)
- **Contratos de Endpoints do Core Service:** [services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/DocumentoController.java](file:///c:/projetos/estudo%20spring/govflow/services/core-service/src/main/java/br/com/govflow/core/infrastructure/adapter/in/rest/DocumentoController.java)

---

## 4. Comandos de Inicialização e Verificação

```bash
# 1. Rodar o Frontend Angular em modo de desenvolvimento
cd frontend && npm start
# Acesso: http://localhost:4200 (Login demo: analista@govflow.com.br / govflow123)

# 2. Rodar a suíte de testes do frontend
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless

# 3. Rodar a suíte do core-service
mvn test -f services/core-service/pom.xml

# 4. Rodar a suíte do gateway
mvn test -f gateway/pom.xml
```
