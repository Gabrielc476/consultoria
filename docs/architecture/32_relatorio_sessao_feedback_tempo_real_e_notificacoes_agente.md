# Relatório de Sessão Técnica: Feedback em Tempo Real e Sistema de Notificações para o Agente

**Data**: 01/10/2026  
**Status**: Concluído  
**Módulos Impactados**: `frontend` (SPA Angular), `services/ai-service`, `services/core-service`, `services/whatsapp-service`

---

## 1. Contexto & Diagnóstico da Trilha do Documento WhatsApp

### O Incidente
O usuário enviou um documento PDF via WhatsApp através de um contato vinculado (`Lais Souza • Psicóloga`, fone `558381579286`) e questionou por que o documento não aparentou ir para a esteira (`/documentos`) nem para a triagem (`/triagem`), questionando se chegou a passar pelo RabbitMQ, MinIO e se o documento possuía ID.

### Auditoria da Trilha Ponta a Ponta:
1. **MinIO (S3)**: **Confirmado!** O arquivo foi transmitido, armazenado e protegido com hash no bucket `govflow-documents`:
   - Chave: `raw/whatsapp/545b7585-d234-436d-b3ed-a06c44f024f8/2026/10/2f26d7df-3a08-4541-9f30-47566123f70e_Boletim_de_Medi__o_BM-03_-_Conv_nio_914250_2023.pdf`
   - Tamanho: 290.858 bytes.
2. **RabbitMQ**: **Confirmado!** A mensagem `whatsapp.documento.recebido` foi enviada à exchange `govflow.events` e consumida pela fila `fila.documentos.extrair`.
3. **AI Service**: **Confirmado!** A IA processou o documento e extraiu:
   - Tipo / Categoria: `BOLETIM_MEDICAO` (Boletim de Medição BM-03)
   - Razão Social / Credor: `Construtora Alvorada Ltda`
   - CNPJ: `14285912000144`
   - Valor: `R$ 145.200,00`
   - Fase sugerida: `FASE_04_EXECUCAO_FISICA`
   - Score de Confiança: `0.985` (98.5%)
4. **PostgreSQL / Core Service**: **Confirmado!**
   - O documento existe na tabela `core_schema.tb_documentos` com o ID UUID `2f26d7df-3a08-4541-9f30-47566123f70e`.
   - **Por que não foi para a Triagem?** Porque o contato já estava vinculado unicamente ao Convênio 914250/2023 e a confiança da IA foi altíssima (98.5%), então ele foi direto para `EM_CONFERENCIA` na esteira, dentro da pasta virtual `/04_Execucao_Fisica_e_Medicoes` do convênio.
   - **Por que o agente não viu na tela?**
     a) O frontend Angular era passivo (não realizava auto-refresh periódico em background da esteira).
     b) Não havia canal de notificação ativo (toasts/som/badging) informando o auditor da chegada de novos documentos.
     c) O nome original veio mascarado como `documento.pdf` no evento do `ai-service` por ausência da propagação do campo `nomeArquivoOriginal`.

---

## 2. Implementação do Sistema de Feedback e Notificações em Tempo Real

### 2.1 Serviço Centralizado `NotificacaoDocumentoService`
- Criado em `frontend/src/app/core/services/notificacao-documento.service.ts`:
  - Polling assíncrono inteligente a cada 5 segundos contra as APIs de esteira e triagem.
  - Rastreamento dos IDs já conhecidos para identificar imediatamente novos ingressos.
  - Alerta Sonoro de Alta Atenção usando a Web Audio API nativa do navegador (chime suave de dois tons, sem necessidade de carregar arquivos MP3 externos).
  - Toasts descritivos com o nome do arquivo, categoria e ID.
  - Lista reativa de notificações recentes (`signal<NotificacaoItem[]>`).
  - Badge numérico de notificações não lidas.

### 2.2 Sinos e Dropdown de Alertas no Header
- Em `frontend/src/app/core/layout/header/header.component.ts`:
  - Sino de notificações interativo com contagem dinâmica de pendências/novidades.
  - Painel Dropdown detalhado com lista dos últimos eventos recebidos (esteira ou triagem).
  - Botão de ação rápida com 1 clique para abrir o documento diretamente no Auditor Lado a Lado (`/documentos/{id}/revisar`).

### 2.3 Badges Vivos na Barra Lateral (Sidebar)
- Em `frontend/src/app/core/layout/sidebar/sidebar.component.ts`:
  - Badges com contadores vivos em tempo real para os módulos:
    - **Esteira de Documentos**: contagem de itens em conferência.
    - **Caixa de Triagem**: contagem de itens aguardando resolução de convênio.

### 2.4 Auto-Refresh em Background nas Telas
- Atualizados `DocumentosListPageComponent` e `TriagemInboxPageComponent`:
  - Implementado polling a cada 5 segundos via `setInterval` e descarte com `ngOnDestroy`.
  - Novos documentos que chegam via WhatsApp aparecem na tela do agente instantaneamente, sem necessidade de F5 ou clique manual em "Atualizar".

---

## 3. Correções de Idempotência e Metadados no Backend

1. **AI Service (`services/ai-service`)**:
   - Adicionado `nome_arquivo_original` aos schemas de eventos `DocumentoClassificadoPayload` e `DocumentoExtraidoPayload`.
   - Propagado o nome original durante a extração e classificação para preservar o nome real do PDF.
2. **Core Service (`services/core-service`)**:
   - Ajustado `DocumentoClassificadoListener` para verificar se o documento já existe (`buscarPorId(docId)`) antes de inserir, evitando corridas de chave duplicada com `DocumentoProcessadoListener`.
   - Criados métodos de domínio `atualizarClassificacao(...)` e `atualizarArmazenamento(...)` na entidade `Documento`.
