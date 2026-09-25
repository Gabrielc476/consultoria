# Handoff: Frontend Cockpit, Seleção de Convênios & Central WhatsApp

> **Destino:** Próximo Agente Antigravity / Desenvolvedor Fullstack  
> **Foco:** Consolidação do Frontend Angular (AppShell, Cockpit 10 Fases, Seletor de Convênios, Central WhatsApp e Handoff com Backend)  
> **Data:** 2026-09-25  
> **Status:** 51/51 testes unitários passando (100% verde) + Build de produção OK + Container Docker ativo em `http://localhost:4200`.

---

## 1. O que foi Entregue no Frontend

O frontend do GovFlow foi estruturado no padrão **Feature-Sliced Design (FSD)** com **Angular Signals**, eliminando a fragmentação de telas e entregando uma experiência de missão crítica para consultorias municipais:

### 1.1 Design System Obsidian Dark-Matte
- **Paleta de Alta Densidade**: Fundo `#0A0E17` (Gov Obsidian), Cards `#111827`, Superfícies Elevadas `#1E293B` e bordas refinadas `border-white/10`.
- **Tipografia Oficial**: Importação das famílias Google Fonts **`Inter`** (leitura nítida) e **`JetBrains Mono`** (valores monetários, números SICONV, relatórios RAE e certidões).
- **Mission Control AppShell**: Casca unificada com `SidebarComponent` (navegação e status online da Evolution API) e `HeaderComponent` (context switchers, atalho do convênio ativo e busca global `⌘K`).
- **Modo Preview Imediato**: Botão *"Acessar Imediatamente (Modo Preview Cockpit)"* no `/login` para permitir exploração mesmo se o API Gateway (:8080) estiver temporariamente desligado.

### 1.2 Gestão & Seleção de Convênios (Solução da Dor Operacional)
1. **Quick-Switcher no Topo do Cockpit (`/convenios` ou `/convenios/:id`)**:
   - Botão interativo no cabeçalho: `Convênio Transferegov #914250/2023 ▾`.
   - Abre um dropdown com busca em tempo real listando todos os convênios da prefeitura ativa.
   - Ao trocar de convênio, o Cockpit reavalia e atualiza reativamente o Stepper de 10 Fases, os KPIs financeiros, os documentos hábeis e a linha de tempo de prazos.
2. **Catálogo & Explorer Geral de Convênios (`/convenios/lista`)**:
   - Cards com Concedente (FNDE, MCID, Saúde, MDR), objeto da obra, % RAE, saldo da conta vinculada Op 006 e prazo restante.
   - Filtros rápidos por ciclo de vida: *Todos*, *Fases 0 a 3 (Planejamento)*, *Fases 4 e 5 (Obras e OBTV)*, *Fases 6 a 9 (Prestação e Proteção)* e *Prazos Críticos*.

### 1.3 Cockpit Operacional do Convênio (Fases 0 a 9)
- **Stepper Linear**: 10 fases cronológicas (00 CAUC até 09 Proteção Jurídica SELIC / Súmula 230 TCU).
- **Dossiê de Fase Contextual**: Clique em qualquer círculo da fase abre modal com status operacional, condicionantes legais e responsáveis.
- **4 KPIs Financeiros**: Repasse Federal, Execução Física RAE Caixa, Saldo em Conta Vinculada Op 006 e Prazo Fatal de Vigência.
- **Integração Cruzada**: Lista de documentos recebidos com score do OCR da IA e régua de vencimentos.

### 1.4 Central WhatsApp & Mensageria de Fiscais (`/whatsapp`)
- **Painel em 3 Colunas** (estilo WhatsApp Web + Linear dark):
  - **Coluna 1 (Contatos & Canais)**: Fiscais de obra, secretários e fornecedores com status online (`Evolution API 🟢 Conectado`) e filtros.
  - **Coluna 2 (Thread de Mensagens)**: Conversas com anexos fiscais (NF-e, Boletim de Medição) exibindo score de confiança do OCR e botão **"Revisar Lado a Lado"** (abre diretamente a bancada de conferência). Inclui barra de disparos rápidos (*Cobrar Prazo 18 dias*, *Solicitar ART*, *Confirmar OBTV*).
  - **Coluna 3 (Dossiê do Contato)**: Ficha cadastral e card do convênio com link direto para o Cockpit.

### 1.5 Telas Complementares Integradas
- **Bancada de Revisão Lado a Lado (`/documentos/:id/revisar`)**: Modo Foco / Zen Mode 100% Viewport com visualizador de mídia, retângulos de Bounding Box, formulário estruturado e checklist de consistência.
- **Radar CAUC & Saúde Fiscal (`/radar-cauc`)**: Matriz das 16 certidões oficiais da LRF 25 agrupadas em 4 quadrantes legais com dias restantes e semáforo de regularidade.

---

## 2. Mapa de Rotas do Frontend

| Rota | Componente | Modo de Exibição | Função Principal |
| :--- | :--- | :--- | :--- |
| `/login` | `LoginPageComponent` | Standalone | Login e 1-clique Modo Demo Preview |
| `/convenios` | `ConvenioCockpitPageComponent` | AppShell | Cockpit do Convênio Ativo |
| `/convenios/lista` | `ConveniosListPageComponent` | AppShell | Catálogo e Explorer Geral de Convênios |
| `/convenios/:id` | `ConvenioCockpitPageComponent` | AppShell | Cockpit de Convênio Específico |
| `/whatsapp` | `WhatsAppHubPageComponent` | AppShell | Central de Mensageria e Fiscais de Obra |
| `/documentos` | `DocumentosListPageComponent` | AppShell | Esteira Geral de Documentos Recebidos |
| `/documentos/:id/revisar` | `RevisaoDetalhePageComponent` | Full Viewport (Zen) | Conferência OCR Lado a Lado com Bounding Boxes |
| `/radar-cauc` | `RadarCaucPageComponent` | AppShell | Matriz de Regularidade Fiscal (16 certidões) |

---

## 3. Mapeamento de Placeholders para Integração com o Backend

Todas as telas foram construídas de forma desacoplada usando Services e Signals. A tabela abaixo orienta o desenvolvedor backend sobre onde conectar cada endpoint futuro:

| Task Backend | Funcionalidade do Backend | Service Frontend | Arquivo a Atualizar |
| :--- | :--- | :--- | :--- |
| **TASK-03** | Cadastro e listagem de Prefeituras | `MunicipioContextService` | `src/app/core/context/municipio-context.service.ts` |
| **TASK-04** | Webhook WhatsApp & Evolution API real | `WhatsAppService` | `src/app/features/whatsapp/services/whatsapp.service.ts` |
| **TASK-06** | Ciclo de aprovação do documento e auditoria | `RevisaoApiService` | `src/app/features/revisao-documento/services/revisao-api.service.ts` |
| **TASK-09** | Sincronização diária SICONV de convênios | `ConvenioContextService` | `src/app/features/convenios/services/convenio-context.service.ts` |
| **TASK-10** | Radar de Prazos Críticos | `RadarPrazosApiService` | `src/app/features/radar-prazos/services/radar-prazos-api.service.ts` |
| **TASK-12** | Monitoramento das 16 certidões do CAUC | `CaucHealthMatrixComponent` | `src/app/features/radar-cauc/components/cauc-health-matrix/` |
| **TASK-13** | Gestão de Cláusula Suspensiva (Caixa) | `ConvenioCockpitPageComponent` | Fase 02 no `convenio-fase.model.ts` |
| **TASK-14** | Licitações 1:N, VRPL e AIO | `ConvenioCockpitPageComponent` | Fase 03 no `convenio-fase.model.ts` |
| **TASK-15** | Boletins de Medição e Relatório RAE | `ConvenioKpisComponent` | Fase 04 no `convenio-fase.model.ts` |
| **TASK-16** | Liquidação Financeira via OBTV | `ConvenioDocumentsListComponent` | Fase 05 no `convenio-fase.model.ts` |
| **TASK-17** | Termos Aditivos e Reequilíbrio | `ConvenioTimelineComponent` | Fase 06 no `convenio-fase.model.ts` |
| **TASK-18** | Prestação de Contas Final (RCO) | `ConvenioCockpitPageComponent` | Fases 07 e 08 no `convenio-fase.model.ts` |
| **TASK-19** | Notificações SELIC (45 dias) e Súmula 230 | `ConvenioTimelineComponent` | Fase 09 no `convenio-fase.model.ts` |

---

## 4. Como Executar e Validar

### Executar Testes Unitários
```bash
cd frontend
npm test -- --watch=false
# Resultado: 51 testes passando (100% de sucesso)
```

### Build de Produção
```bash
cd frontend
npm run build -- --configuration production
# Saída: dist/frontend/browser gerado em ~13.5s
```

### Subir no Docker
```bash
docker compose build frontend
docker compose up -d --no-deps frontend
# Acesso: http://localhost:4200
```
