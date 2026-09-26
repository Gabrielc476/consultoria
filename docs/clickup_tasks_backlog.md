# GovFlow — Backlog de Tarefas para ClickUp (MVP & Ciclo de Vida Integral)

> **Projeto:** GovFlow — Copiloto Inteligente de Gestão do Transferegov para Consultorias Municipais  
> **Origem dos Requisitos:** Documentação Técnica, ADRs e Arquitetura (`/docs` e `/docs/architecture`)  
> **Formato:** Estruturado para importação direta via ClickUp Docs, Importador Markdown ou Criação de Tasks  

---

## 📌 Guia Rápido de Importação no ClickUp

Você pode usar este documento de três maneiras no ClickUp:
1. **Via ClickUp Docs:** Crie um documento no seu Espaço, importe ou cole este conteúdo e selecione o texto de cada tarefa para converter em Tarefa com 1 clique (*"Create Task from selection"*).
2. **Via Importador CSV/Excel:** Caso prefira importar tudo em massa em uma lista de uma vez, use a tabela resumo ao final deste documento.
3. **Criação Manual Assistida:** Copie os blocos de cada task diretamente para os campos de Título, Descrição, Tags e Checklists do ClickUp.

---

# 📁 PASTA 1: 01. Fundação & Infraestrutura

## [TASK-01] Orquestração Local de Infraestrutura e Schemas PostgreSQL
* **Lista:** `01. Fundação & Infraestrutura`
* **Prioridade:** Urgente (Vermelho)
* **Dependência (Blocked by):** *Nenhuma (Pode iniciar imediatamente)*
* **Tags:** `#infra`, `#docker`, `#postgresql`, `#rabbitmq`, `#minio`

### Descrição & O Que Entrega
Subida completa do ambiente local multi-serviço via Docker Compose. Configura a instância única de PostgreSQL 16 com os 3 schemas lógicos isolados (`core_schema`, `transferegov_schema`, `whatsapp_schema`), o broker RabbitMQ com console de gestão e o MinIO (compatível com S3) criando os buckets de armazenamento de mídias e documentos. Executa os scripts Flyway de migração V1 a V13.

### Critérios de Aceite
- [x] O comando `docker-compose up -d` inicializa Postgres, RabbitMQ e MinIO sem falhas.
- [x] Execução das migrações Flyway V1 a V13 cria todas as tabelas e índices em seus respectivos schemas.
- [x] Console de gerenciamento do RabbitMQ acessível em `http://localhost:15672`.
- [x] Bucket `govflow-documents` criado e funcional no MinIO (`http://localhost:9000`).

---

## [TASK-02] API Gateway Reativo com Autenticação JWT e Multi-Tenant Injection
* **Lista:** `01. Fundação & Infraestrutura`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-01]`
* **Tags:** `#gateway`, `#spring-cloud`, `#java21`, `#clean-architecture`, `#jwt`

### Descrição & O Que Entrega
Serviço de borda reativo (Spring Cloud Gateway na porta 8080) estruturado em Clean Architecture. Centraliza o roteamento reativo para todos os microsserviços internos, valida tokens JWT, extrai a claim `tenant_id` da consultoria e propaga os cabeçalhos imutáveis `X-Tenant-Id`, `X-User-Id` e `X-Correlation-Id`. Trata requisições não autenticadas no padrão RFC 7807 (Problem Details).

### Critérios de Aceite
- [x] Rotas configuradas para `/api/v1/core/**`, `/api/v1/whatsapp/**`, `/api/v1/ai/**` e `/api/v1/transferegov/**`.
- [x] Rotas públicas (`/api/v1/auth/**`, webhook do WhatsApp) liberadas sem exigência de Bearer token.
- [x] Requisições não autorizadas respondem com HTTP 401 padronizado em JSON RFC 7807.
- [x] Headers `X-Tenant-Id`, `X-User-Id` e `X-Correlation-Id` sanitizados e propagados para os serviços downstream.

---

# 📁 PASTA 2: 02. Pipeline Operacional MVP (Tracer Bullet Principal)

## [TASK-03] Core Service: Gestão de Consultorias, Prefeituras e Multi-Tenancy
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Urgente (Vermelho)
* **Dependência (Blocked by):** `[TASK-02]`
* **Tags:** `#core-service`, `#spring-boot-3`, `#ddd`, `#hexagonal`, `#hibernate6`

### Descrição & O Que Entrega
Núcleo transacional do GovFlow responsável pelo cadastro de Consultorias (Tenants) e Prefeituras convenentes. Utiliza Arquitetura Hexagonal com domínio puro e persistência JPA com anotação `@TenantId` nativa do Hibernate 6, garantindo isolamento total de dados entre consultorias clientes.

### Critérios de Aceite
- [x] Endpoints REST protegidos para cadastro e consulta de Prefeituras (`/api/v1/prefeituras`).
- [x] TenantInterceptor captura o header `X-Tenant-Id` e configura o contexto da thread.
- [x] Teste automatizado comprova que consultas do Tenant A jamais retornam dados do Tenant B.
- [x] Validações de CNPJ municipal e código IBGE implementadas no modelo de domínio.

---

## [TASK-04] WhatsApp Service: Webhook Inbound e Download de Mídias para MinIO
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-01]`, `[TASK-03]`
* **Tags:** `#whatsapp-service`, `#evolution-api`, `#rabbitmq`, `#minio`, `#event-driven`

### Descrição & O Que Entrega
Módulo event-driven com Strategy Pattern para integração com provedores de WhatsApp (iniciando com Evolution API self-hosted). Recebe o webhook de mensagens, áudios e anexos (PDFs/fotos de notas e medições), faz upload em streaming diretamente para o MinIO gerando chave segura e publica o evento assíncrono `DocumentoRecebidoEvent` no RabbitMQ em menos de 80ms.

### Critérios de Aceite
- [x] Endpoint `POST /api/v1/whatsapp/webhook` processa mensagens com arquivos em anexo.
- [x] Arquivo binário transferido via streaming para o MinIO sem acúmulo em disco local.
- [x] Evento `DocumentoRecebidoEvent` publicado na fila `fila.documentos.extrair` do RabbitMQ.
- [x] Número do remetente resolvido com sucesso para a prefeitura cadastrada no banco.

---

## [TASK-05] AI Service: Extração Multimodal de Documento Hábil com Gemini 3.x e Pydantic
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Urgente (Vermelho)
* **Dependência (Blocked by):** `[TASK-01]`
* **Tags:** `#ai-service`, `#fastapi`, `#gemini`, `#pydantic-v2`, `#ocr-multimodal`

### Descrição & O Que Entrega
Serviço Python FastAPI com Clean Pipeline e Strategy Pattern de LLMs. Consome a fila do RabbitMQ, baixa o PDF do MinIO, invoca o Gemini 3.x Flash-Lite com schema estrito (`DocumentoHabilExtraction`), extrai campos fiscais oficiais do Transferegov com coordenadas de bounding box e roda validação matemática rígida (`Valor Bruto - Retenções == Valor Líquido`). Publica o resultado no RabbitMQ.

### Critérios de Aceite
- [x] Extração estruturada dos dados: Tipo de Documento, Número da NF, Data de Emissão, CNPJ/Razão Social do Credor, Valor Bruto e Retenções (INSS, ISS, IRRF).
- [x] Retorno de coordenadas normalizadas `[ymin, xmin, ymax, xmax]` para destaque visual no frontend.
- [x] Regra matemática determinística sinaliza alerta se as deduções não baterem com o valor total.
- [x] Resultado publicado no evento `DocumentoExtraidoEvent` para consumo do Core Service.

---

## [TASK-06] Core Service: Agregado Documento e Ciclo de Aprovação Auditada
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, `[TASK-05]`
* **Tags:** `#core-service`, `#hexagonal`, `#auditoria`, `#revisao-humana`

### Descrição & O Que Entrega
Recebe o evento de extração da IA e transiciona o Documento para o estado `EM_CONFERENCIA`. Disponibiliza endpoints para consulta dos dados extraídos com índices de confiança e para a aprovação/rejeição pelo analista da consultoria, persistindo a trilha de auditoria completa com comparativo (*diff*) de campos alterados manualmente.

### Critérios de Aceite
- [x] Listener RabbitMQ atualiza a entidade `Documento` com os campos sugeridos pela IA.
- [x] Endpoint `GET /api/v1/documentos/{id}` retorna o payload consolidado para a interface.
- [x] Endpoint `PUT /api/v1/documentos/{id}/aprovar` valida obrigatoriedades e altera status para `PRONTO_PARA_TRANSFEREGOV`.
- [x] Registro detalhado de auditoria salvo na tabela `tb_auditorias_revisao`.

---

## [TASK-07] Frontend Angular: Tela de Conferência Lado a Lado (Side-by-Side Review)
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-06]`
* **Tags:** `#frontend`, `#angular`, `#signals`, `#pdf-viewer`, `#fsd`

### Descrição & O Que Entrega
Interface web SPA em Angular 19/20 utilizando Feature-Sliced Design e Signals. Implementa a tela crítica de conferência lado a lado: 50% da tela para o visualizador de PDF (`ngx-extended-pdf-viewer`) com marcação retangular das bounding boxes onde a IA leu cada dado, e 50% para o formulário estruturado de campos com semáforo de confiança (verde, amarelo e vermelho). Permite aprovar ou editar campos em menos de 30 segundos.

### Critérios de Aceite
- [x] Visualização do documento original em PDF/imagem com zoom e navegação de páginas.
- [x] Coordenadas da IA destacam visualmente as caixas dos campos sobre o PDF.
- [x] Campos de formulário conectados com Signals e recálculo automático de retenções.
- [x] Botão de aprovação chama a API do Core Service e avança para o próximo documento pendente.

---

## [TASK-08] Extensão Chrome: Detecção do Portal Transferegov e Injeção no DOM
* **Lista:** `02. Pipeline Operacional MVP`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-06]`
* **Tags:** `#extension`, `#chrome-manifest-v3`, `#shadow-dom`, `#dom-injection`

### Descrição & O Que Entrega
Extensão oficial do Chrome (Manifest V3 em TypeScript). Detecta automaticamente quando o analista está na tela oficial de "Incluir Documento Hábil" do `transferegov.sistema.gov.br`, busca o documento aprovado no Core Service e injeta um painel flutuante em Shadow DOM fechado. Com 1 clique no botão "⚡ Preencher Formulário Oficial", preenche os inputs e dispara eventos sintéticos (`input`, `change`). Fornece botões de cópia rápida individual como salvaguarda de contingência.

### Critérios de Aceite
- [ ] Detecção precisa da URL e do formulário oficial do Transferegov.br.
- [ ] Painel flutuante isolado em Shadow DOM, prevenindo conflitos visuais de CSS com o portal do governo.
- [ ] Preenchimento automático de todos os campos (NF, data, credor, valores e retenções).
- [ ] Botão de cópia rápida em 1 clique ao lado de cada campo para contingência operacional.

---

# 📁 PASTA 2.1: Frontend Cockpit, Seleção de Convênios & Central WhatsApp (FSD & Signals)

## [TASK-FE-01] AppShell, Design System Obsidian & Dark Matte Tokens (Tailwind + Google Fonts)
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** *Nenhuma*
* **Tags:** `#frontend`, `#angular`, `#tailwind`, `#design-system`, `#obsidian-dark`

### Descrição & O Que Entrega
Arquitetura de casca visual unificada (`AppShellComponent`, `SidebarComponent`, `HeaderComponent`) com paleta Obsidian Dark (`#0A0E17`, `#111827`, `#1E293B`, bordas `border-white/10`) e fontes oficiais `Inter` (leitura densa) e `JetBrains Mono` (valores financeiros e numerais SICONV). Elimina fragmentação de layouts e unifica rotas autenticadas sob uma barra de navegação com status da Evolution API e badge do usuário.

### Critérios de Aceite
- [x] Shell responsivo com Sidebar expansível/colapsável e Header global contextual.
- [x] Fontes `Inter` e `JetBrains Mono` pré-carregadas via Google Fonts no `index.html`.
- [x] Modo Preview Imediato no login para testes sem bloqueio de gateway offline.
- [x] Roteamento estruturado em SPA preservando o layout base.

---

## [TASK-FE-02] Context Switcher Reativo (Municípios da Paraíba & Persistência em Signals)
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-FE-01]`
* **Tags:** `#frontend`, `#signals`, `#multi-tenant`, `#municipio-context`

### Descrição & O Que Entrega
Serviço transversal com Angular Signals (`MunicipioContextService`) que gerencia a prefeitura ativa da consultoria (ex: Patos, Sousa, Pombal, Monteiro, Cajazeiras). Integração com `GET /api/v1/prefeituras` e fallback resiliente em memória, persistindo o ID no `localStorage` e reagindo instantaneamente em todos os componentes filhos.

### Critérios de Aceite
- [x] Menu dropdown flutuante no topo permitindo alternar prefeituras em 1 clique.
- [x] Signals reativos `municipioAtivo()` e `nomeMunicipioAtivoFormatado()`.
- [x] Persistência transparente no navegador sem reload de página.
- [x] Suporte à lista de prefeituras da Paraíba com contadores de convênios.

---

## [TASK-FE-03] Cockpit do Convênio — Esteira Linear de 10 Fases (Fases 0 a 9) e Saldo Op 006
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Urgente (Vermelho)
* **Dependência (Blocked by):** `[TASK-FE-02]`
* **Tags:** `#frontend`, `#cockpit`, `#10-fases`, `#stepper`, `#kpis-financeiros`

### Descrição & O Que Entrega
Painel central do convênio federal (`/convenios` ou `/convenios/:id`). Exibe o ciclo de vida completo em 10 Fases cronológicas (Fase 00 CAUC até Fase 09 Blindagem TCU/SELIC) com círculos numerados, linhas conectoras dinâmicas e modal de dossiê de fase ao clicar. Inclui 4 KPI cards financeiros de alta precisão: Repasse Federal, Execução Física RAE Caixa, Saldo em Conta Vinculada Op 006 e Prazo Fatal de Vigência.

### Critérios de Aceite
- [x] Stepper horizontal interativo das 10 fases com semáforo de status (Concluída, Em Andamento, Pendente).
- [x] Modal contextual ao clicar em qualquer fase exibindo condicionantes legais e responsáveis.
- [x] Cards financeiros com tipografia mono e barras de progresso percentuais.
- [x] Painel dividido integrando documentos recebidos e cronograma de prazos.

---

## [TASK-FE-04] Gestão e Seleção de Convênios (Quick-Switcher no Cockpit & Explorer Geral)
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-FE-03]`
* **Tags:** `#frontend`, `#convenio-selector`, `#convenios-list`, `#signals`

### Descrição & O Que Entrega
Soluciona o fluxo de trabalho do funcionário para alternar e explorar múltiplos convênios. Implementa o `ConvenioContextService` com base reativa de convênios por município, o botão Quick-Switcher no topo do Cockpit com dropdown de busca instantânea, e a página completa de catálogo `/convenios/lista` com filtros por estágio de obras, busca por número SICONV/objeto e atalho para o Cockpit correspondente.

### Critérios de Aceite
- [x] Quick-Switcher no cabeçalho do Cockpit para troca instantânea de convênio sem sair da tela.
- [x] Página de Catálogo `/convenios/lista` com resumo executivo de saldos e convênios ativos.
- [x] Filtros por grupos de fase: Planejamento (0 a 3), Obras & OBTV (4 e 5), Prestação (6 a 9) e Críticos.
- [x] Sincronização automática entre o convênio selecionado e a prefeitura proprietária.

---

## [TASK-FE-05] Central WhatsApp & Mensageria de Fiscais de Obra (/whatsapp) com IA OCR
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-04]`, `[TASK-FE-01]`
* **Tags:** `#frontend`, `#whatsapp-hub`, `#mensageria`, `#evolution-api`, `#ocr-preview`

### Descrição & O Que Entrega
Interface dedicada de comunicação em 3 colunas inspirada no WhatsApp Web e Linear. Permite ao analista interagir com fiscais de obra, secretários e construtoras, visualizar arquivos fiscais recebidos com score do OCR da IA, disparar macros de cobrança de prazo e ART em 1 clique, e abrir diretamente a bancada de conferência lado a lado pelo anexo da mensagem.

### Critérios de Aceite
- [x] Lista de conversas com filtro por categoria (Fiscais, Finanças, Jurídico) e status online.
- [x] Thread de mensagens com balões de remetente, mensagens do bot IA e cards de anexo PDF.
- [x] Botão no anexo "Revisar Lado a Lado" que navega para `/documentos/:id/revisar`.
- [x] Barra de disparos rápidos (Macro de Prazo 18 dias, Solicitação de ART, Confirmação de OBTV).
- [x] Painel lateral direito com dossiê do fiscal e link direto para o Cockpit do convênio vinculado.

---

## [TASK-FE-06] Radar CAUC e Matriz de Saúde Fiscal (16 Exigências LRF 25)
* **Lista:** `02.1. Frontend Cockpit & WhatsApp`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-12]`, `[TASK-FE-01]`
* **Tags:** `#frontend`, `#radar-cauc`, `#lrf-25`, `#matriz-fiscal`

### Descrição & O Que Entrega
Painel analítico `/radar-cauc` com a matriz das 16 certidões oficiais do CAUC/SIAFI agrupadas em 4 grupos legais: Obrigações Tributárias/Previdenciárias, Adimplência Financeira, Prestação de Contas de Convênios e Cumprimento de Limites Constitucionais (Saúde/Educação/LRF). Exibe semáforo de regularidade e alertas preventivos de vencimento.

### Critérios de Aceite
- [x] Grid estruturado com os 4 grupos e as 16 certidões da IN STN nº 1/2021.
- [x] Indicador de validade em dias com cores de semáforo (Regular, Alerta, Irregular).
- [x] Painel de ações corretivas rápidas para renovação de certidões.

---

# 📁 PASTA 3: 03. Sincronização & Inteligência Transferegov

## [TASK-09] Transferegov Service: Pipeline de Streaming e Ingestão de Dumps CSV/ZIP
* **Lista:** `03. Sincronização & Inteligência Transferegov`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-01]`
* **Tags:** `#transferegov-service`, `#etl`, `#streaming-csv`, `#spring-boot-3`

### Descrição & O Que Entrega
Job agendado matinal (`@Scheduled`) no `transferegov-service` que consome em streaming os 65 arquivos ZIP/CSV públicos disponibilizados diariamente no Azure Blob Storage do governo. Descompacta em memória e filtra exclusivamente os convênios da Paraíba (`UF = 'PB'`) ou dos CNPJs convenentes cadastrados, atualizando o `transferegov_schema` em menos de 1 minuto sem gargalos de memória.

### Critérios de Aceite
- [x] Download e descompactação em streaming em memória sem gravação de arquivos intermediários no disco.
- [x] Filtragem em voo por UF/CNPJ durante o processamento do CSV.
- [x] Persistência de convênios, vigências e valores na tabela `tb_sincronizacao_convenio`.

---

## [TASK-10] Radar Proativo de Prazos Críticos e Alertas de Vigência
* **Lista:** `03. Sincronização & Inteligência Transferegov`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-09]`, `[TASK-07]`
* **Tags:** `#transferegov-service`, `#alertas`, `#radar-prazos`, `#frontend`

### Descrição & O Que Entrega
Motor de monitoramento de risco e prazos de transferências federais. Avalia diariamente as datas de vencimento de vigência, cláusulas suspensivas e limites de prestação de contas, classificando convênios em réguas de criticidade (60, 30, 15 dias e vencidos) e exibindo um dashboard de alerta com semáforo visual para a consultoria.

### Critérios de Aceite
- [x] Rotina diária que calcula os dias restantes para expiração de cada convênio monitorado.
- [x] Classificação em níveis de risco: Crítico (vermelho), Atenção (amarelo) e Regular (verde).
- [x] Endpoint `GET /api/v1/transferegov/radar-prazos` entregando o panorama consolidado por município.
- [x] Componente de Dashboard no Angular com filtros por gravidade de prazo.

---

## [TASK-11] Ingestão da API REST de Emendas Especiais (Emendas Pix)
* **Lista:** `03. Sincronização & Inteligência Transferegov`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-09]`
* **Tags:** `#transferegov-service`, `#emendas-pix`, `#rest-client`, `#stf-adpf854`

### Descrição & O Que Entrega
Integração com o endpoint REST `/especiais` da API aberta do Transferegov para captura e acompanhamento de planos de trabalho e relatórios de gestão de Emendas Parlamentares Especiais (Emendas Pix), garantindo conformidade com as regras de transparência e auditoria do STF (ADPF 854).

### Critérios de Aceite
- [x] Consumo paginado da API de transferências especiais do governo federal (`/especiais`).
- [x] Mapeamento e persistência dos dados de planos de trabalho, destinações declaradas e contas correntes.
- [x] Identificação de inconsistências ou ausência de relatórios de gestão nos prazos regulamentares (ADPF 854 / Portaria MGI nº 33/2023) com disparo de alertas via RabbitMQ.
- [x] Camada de consulta CQRS-Light com endpoints REST `/api/v1/transferegov/emendas-especiais/**` e trigger de sincronização sob demanda.

---

# 📁 PASTA 4: 04. Ciclo de Vida Completo dos Convênios (Fases 0 a 9)

## [TASK-12] Radar CAUC: Monitoramento das 16 Exigências Fiscais (Fase 0)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, Flyway `V5`
* **Tags:** `#core-service`, `#cauc`, `#fase-0`, `#lrf`

### Descrição & O Que Entrega
Módulo de acompanhamento da regularidade fiscal do município conforme art. 25 da Lei de Responsabilidade Fiscal (LRF). Monitora individualmente a validade das 16 certidões fiscais e orçamentárias (Receita/PGFN, FGTS, CNDT, RREO, RGF, SICONFI, limites constitucionais de saúde e educação) e emite alertas antes do vencimento para evitar bloqueio de celebração de convênios.

### Critérios de Aceite
- [x] Estrutura `tb_certidoes_cauc` vinculada à Prefeitura contendo as 16 certidões oficiais.
- [x] Alertas visuais e disparos programados de proximidade de expiração (10 e 5 dias de antecedência).
- [x] Badge de status de regularidade fiscal visível na listagem de municípios.

---

## [TASK-13] Gestão de Cláusula Suspensiva: Três Pilares da Caixa (Fase 2)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, Flyway `V7`
* **Tags:** `#core-service`, `#clausula-suspensiva`, `#caixa-gigov`, `#fase-2`

### Descrição & O Que Entrega
Painel dedicado ao controle e superação da Cláusula Suspensiva em contratos de repasse com a Caixa Econômica Federal. Rastreia o prazo fatal e o status de aprovação técnica dos três pilares obrigatórios: Projetos de Engenharia e Orçamento SINAPI (SPA/LAE), Licenciamento Ambiental e Comprovação de Titularidade do Imóvel.

### Critérios de Aceite
- [x] Cadastro das condicionantes suspensivas com controle rigoroso de data-limite improrrogável.
- [x] Upload e custódia segura de laudos de titularidade e licenças ambientais no MinIO.
- [x] Checklist visual de pendências impedindo a perda de prazos que provocam rescisão de repasse.

---

## [TASK-14] Relação 1:N de Licitações, Homologação VRPL e Emissão de AIO (Fase 3)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-03]`, Flyway `V4`, `V8`
* **Tags:** `#core-service`, `#licitacoes`, `#vrpl`, `#aio`, `#fase-3`

### Descrição & O Que Entrega
Modelagem e endpoints para gerenciar múltiplos certames licitatórios municipais vinculados a um único convênio federal (relação 1:N). Gerencia o envio de documentação do processo para Verificação do Resultado do Processo Licitatório (VRPL) pela Caixa e registra a Autorização de Início de Objeto (AIO), bloqueando qualquer medição física antes da liberação oficial.

### Critérios de Aceite
- [ ] Suporte à criação de múltiplas licitações vinculadas ao mesmo convênio federal.
- [ ] Registro do protocolo e do parecer técnico de VRPL emitido pelo Concedente/Mandatária.
- [ ] Bloqueio formal de criação de boletins de medição para obras sem registro de AIO deferida.

---

## [TASK-15] Boletins de Medição de Obras e Relatório RAE da Caixa (Fase 4)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-14]`, Flyway `V9`
* **Tags:** `#core-service`, `#medicoes-obras`, `#rae-caixa`, `#fase-4`

### Descrição & O Que Entrega
Controle de execução física através de boletins periódicos e acumulados de medição vinculados ao Contrato de Execução e à Empreiteira. Registra o ateste formal do fiscal de obras municipal, o percentual de avanço de metas e confronta com o Relatório de Acompanhamento de Engenharia (RAE) da Caixa Econômica para autorização de desembolso.

### Critérios de Aceite
- [ ] Cálculo automático de percentuais físicos acumulados e saldo contratual remanescente.
- [ ] Associação obrigatória da Nota Fiscal emitida ao respectivo boletim de medição.
- [ ] Campo para conciliação do RAE da Caixa com anotação de valores glosados ou liberados.

---

## [TASK-16] Liquidação Financeira via OBTV em Duplo Comando (Fase 5)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Alta (Laranja)
* **Dependência (Blocked by):** `[TASK-06]`, `[TASK-15]`, Flyway `V10`
* **Tags:** `#core-service`, `#obtv`, `#liquidacao-financeira`, `#fase-5`

### Descrição & O Que Entrega
Módulo de liquidação de despesas e emissão de Ordens Bancárias de Transferência Voluntária (OBTV) a partir da conta corrente vinculada (Op 006). Garante que os impostos retidos (INSS, ISS, IRRF) gerem comandos para guias específicas e que o valor líquido seja transferido exclusivamente para a conta da empresa contratada, com suporte a duplo comando de autorização.

### Critérios de Aceite
- [ ] Validação algorítmica: soma das OBTVs deve ser rigorosamente idêntica ao valor bruto da nota atestada.
- [ ] Trava de segurança impedindo liquidação financeira para credor ou conta divergentes do contrato.
- [ ] Suporte na Extensão Chrome para preenchimento ágil da tela de autorização de OBTV no Transferegov.

---

## [TASK-17] Termos Aditivos de Prorrogação e Reequilíbrio Físico-Financeiro (Fase 6)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-14]`, Flyway `V11`
* **Tags:** `#core-service`, `#termos-aditivos`, `#reequilibrio`, `#lei14133`, `#fase-6`

### Descrição & O Que Entrega
Gestão de alterações contratuais bilaterais (Termos Aditivos) para prorrogação de vigência e alteração de valores (acréscimos e supressões no limite legal de 25% da Lei nº 14.133/2021). Inclui registro de Apostilamento de Reajuste por índices oficiais de engenharia (INCC/IPCA) sem necessidade de aditivo formal.

### Critérios de Aceite
- [ ] Cadastro de aditivos vinculados tanto ao Convênio Federal quanto ao Contrato Municipal de Execução.
- [ ] Alerta algorítmico caso o somatório de aditivos ultrapasse o teto legal de 25%.
- [ ] Atualização automática da nova data final de vigência na linha do tempo do convênio.

---

## [TASK-18] Prestação de Contas Final (RCO) e Encerramento de Conta (Fases 7 e 8)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-16]`, Flyway `V12`
* **Tags:** `#core-service`, `#rco`, `#prestacao-contas`, `#fase-7-8`

### Descrição & O Que Entrega
Compilação assistida do Relatório de Cumprimento do Objeto (RCO) a ser protocolado no Transferegov no prazo fatal de até 60 dias após o término da vigência. Consolida a conciliação bancária completa (comprovante de saldo zerado na Op 006), comprovante de devolução de saldo remanescente e rendimentos via Guia de Recolhimento da União (GRU), Termo de Recebimento Definitivo e registros fotográficos finais.

### Critérios de Aceite
- [ ] Checklist do RCO sinalizando todas as peças documentais obrigatórias.
- [ ] Validação de conciliação bancária comprovando saldo da conta bancária zerado (R$ 0,00).
- [ ] Contagem regressiva em destaque no painel para o prazo limite de 60 dias da prestação de contas.

---

## [TASK-19] Passivo, Notificações SELIC (45 dias) e Blindagem por Súmula 230/TCU (Fase 9)
* **Lista:** `04. Ciclo de Vida Completo`
* **Prioridade:** Média (Amarelo)
* **Dependência (Blocked by):** `[TASK-18]`, Flyway `V13`
* **Tags:** `#core-service`, `#passivo-convenios`, `#sumula-230-tcu`, `#tce`, `#fase-9`

### Descrição & O Que Entrega
Módulo de proteção jurídica municipal contra bloqueios federais e instauração de Tomada de Contas Especial (TCE). Controla o prazo fatal de 45 dias de notificações com juros SELIC e operacionaliza a blindagem do art. 26-A da Lei nº 10.522/2002 e Súmula 230/TCU: permite cadastrar ação judicial de ressarcimento contra ex-gestor omisso, viabilizando o desbloqueio cautelar do município no CAUC/SIAFI.

### Critérios de Aceite
- [ ] Cronômetro regressivo com alertas para notificações de 45 dias com juros SELIC.
- [ ] Cadastro do número de processo de Ação Civil Pública / Representação no MPF com upload da petição.
- [ ] Emissão de relatório de proteção institucional para instrução de ofício de desbloqueio perante o Concedente.

---

## 📊 Matriz Consolidada de Tarefas (Visão Rápida)

| ID | Nome da Tarefa | Lista / Pasta | Prioridade | Status | Componente Principal |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TASK-01** | Orquestração Local de Infraestrutura e Schemas PostgreSQL | 01. Fundação | Urgente 🔴 | Concluído ✅ | Docker / Postgres / RabbitMQ / MinIO |
| **TASK-02** | API Gateway Reativo com Autenticação JWT e Multi-Tenant | 01. Fundação | Alta 🟠 | Concluído ✅ | Spring Cloud Gateway (Java 21) |
| **TASK-03** | Core Service: Gestão de Consultorias e Multi-Tenancy | 02. MVP Pipeline | Urgente 🔴 | Concluído ✅ | Core Service (Spring Boot 3) |
| **TASK-04** | WhatsApp Service: Webhook Inbound e Download MinIO | 02. MVP Pipeline | Alta 🟠 | Concluído ✅ | WhatsApp Service (Evolution API) |
| **TASK-05** | AI Service: Extração Multimodal com Gemini 3.x e Bounding Boxes | 02. MVP Pipeline | Urgente 🔴 | Concluído ✅ | AI Service (FastAPI / Gemini) |
| **TASK-06** | Core Service: Agregado Documento e Ciclo de Aprovação | 02. MVP Pipeline | Alta 🟠 | Concluído ✅ | Core Service (Hexagonal) |
| **TASK-07** | Frontend Angular: Tela de Conferência Lado a Lado | 02. MVP Pipeline | Alta 🟠 | Concluído ✅ | Frontend Angular 19/20 (Signals) |
| **TASK-08** | Extensão Chrome: Detecção do Transferegov e Injeção no DOM | 02. MVP Pipeline | Alta 🟠 | Backlog ⏳ | Extensão Chrome (Manifest V3) |
| **TASK-FE-01** | AppShell, Design System Obsidian & Dark Matte Tokens | 02.1. Frontend Cockpit | Alta 🟠 | Concluído ✅ | Angular / Tailwind / Fonts Inter & Mono |
| **TASK-FE-02** | Context Switcher Reativo (Municípios da Paraíba & Signals) | 02.1. Frontend Cockpit | Alta 🟠 | Concluído ✅ | MunicipioContextService |
| **TASK-FE-03** | Cockpit do Convênio — Esteira 10 Fases e Saldo Op 006 | 02.1. Frontend Cockpit | Urgente 🔴 | Concluído ✅ | ConvenioCockpit / PhaseStepper / KPIs |
| **TASK-FE-04** | Gestão & Seleção de Convênios (Quick-Switcher & /convenios/lista) | 02.1. Frontend Cockpit | Alta 🟠 | Concluído ✅ | ConvenioContext / ConveniosListPage |
| **TASK-FE-05** | Central WhatsApp & Mensageria de Fiscais (/whatsapp) | 02.1. Frontend Cockpit | Alta 🟠 | Concluído ✅ | WhatsAppHub / Evolution API Mock |
| **TASK-FE-06** | Radar CAUC e Matriz de Saúde Fiscal (16 Exigências LRF 25) | 02.1. Frontend Cockpit | Alta 🟠 | Concluído ✅ | RadarCauc / CaucHealthMatrix |
| **TASK-09** | Transferegov Service: Streaming e Ingestão Dumps CSV/ZIP | 03. Sincronização | Média 🟡 | Concluído ✅ | Transferegov Service (Spring Boot) |
| **TASK-10** | Radar Proativo de Prazos Críticos e Alertas de Vigência | 03. Sincronização | Média 🟡 | Concluído ✅ | Transferegov Service / Frontend |
| **TASK-11** | Ingestão da API REST de Emendas Especiais (Emendas Pix) | 03. Sincronização | Média 🟡 | Concluído ✅ | Transferegov Service (RestClient) |
| **TASK-12** | Radar CAUC: Monitoramento das 16 Exigências Fiscais (Fase 0) | 04. Ciclo de Vida | Alta 🟠 | Concluído ✅ | Core Service |
| **TASK-13** | Gestão de Cláusula Suspensiva: Três Pilares da Caixa (Fase 2) | 04. Ciclo de Vida | Alta 🟠 | Concluído ✅ | Core Service / Frontend |
| **TASK-14** | Licitações 1:N, Homologação VRPL e Emissão de AIO (Fase 3) | 04. Ciclo de Vida | Alta 🟠 | Backlog ⏳ | Core Service |
| **TASK-15** | Boletins de Medição de Obras e Relatório RAE Caixa (Fase 4) | 04. Ciclo de Vida | Alta 🟠 | Backlog ⏳ | Core Service / Frontend |
| **TASK-16** | Liquidação Financeira via OBTV em Duplo Comando (Fase 5) | 04. Ciclo de Vida | Alta 🟠 | Backlog ⏳ | Core Service / Extensão Chrome |
| **TASK-17** | Termos Aditivos de Prorrogação e Reequilíbrio (Fase 6) | 04. Ciclo de Vida | Média 🟡 | Backlog ⏳ | Core Service |
| **TASK-18** | Prestação de Contas Final (RCO) e Encerramento (Fases 7 e 8) | 04. Ciclo de Vida | Média 🟡 | Backlog ⏳ | Core Service / Frontend |
| **TASK-19** | Passivo, Notificações SELIC e Súmula 230/TCU (Fase 9) | 04. Ciclo de Vida | Média 🟡 | Backlog ⏳ | Core Service |
