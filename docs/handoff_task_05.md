# Handoff: Transição para TASK-05 — AI Service (Extração Multimodal de Documentos Hábeis com Gemini & Pydantic)

## 1. Contexto Geral & Estado Atual do Repositório

- **Branch Atual:** `main`.
- **Última Entrega (TASK-04):** Conclusão e homologação do `whatsapp-service` (Java 21 / Spring Boot 3.3.5 / Spring AMQP / MinIO S3 SDK / PostgreSQL `whatsapp_schema`).
- **Estado de Testes Automatizados:** 87 testes automatizados passando com 100% de sucesso:
  - `whatsapp-service`: 15 testes (Unitários e de Integração com MockMvc, S3 Mock e RabbitMQ Mock).
  - `core-service`: 52 testes (Domínio puro, Módulos 10/11, JPA Multi-Tenant com `@TenantId` e isolamento entre consultorias).
  - `gateway`: 20 testes (Roteamento reativo, filtros JWT e propagação de headers).
- **Infraestrutura em Execução no Docker Compose:**
  - `govflow-postgres`: PostgreSQL 16 na porta `5432`, com schemas `core_schema`, `whatsapp_schema` e `transferegov_schema` (migrations V1 a V14 aplicadas).
  - `govflow-redis`: Redis 7 na porta `6379`.
  - `govflow-minio`: MinIO S3 nas portas `9000` (API) e `9001` (Console Web), buckets provisionados: `govflow-documents` e `evolution`.
  - `govflow-rabbitmq`: RabbitMQ 3.13-management nas portas `5672` (AMQP) e `15672` (Management UI).
  - `govflow-evolution-api`: Evolution API v2.3.7 na porta `8084`, com masquerading ativo de browser (`GovFlow Desktop`/`Chrome`) e persistência direta no PostgreSQL.
  - `govflow-whatsapp-service`: Microsserviço real na porta `8083`, processando webhooks em tempo real.
  - `govflow-core-service`: Microsserviço na porta `8081`.
  - `govflow-api-gateway`: Spring Cloud Gateway na porta `8080`.
  - `govflow-stub-ai`: Nginx Echo Stub na porta 80 interna (a ser substituído pelo serviço real na TASK-05).
- **WhatsApp Pareado e Operacional em Tempo Real:**
  - Instância: `govflow-consultoria` conectada com sucesso ao WhatsApp do usuário (`wuid: 558387514931`).
  - Webhook configurado e ativo para `http://whatsapp-service:8083/api/v1/whatsapp/webhook`.
  - **Validação de Produção Real Concluída:** Mensagens reais e anexos (PDFs e imagens) já foram recebidos no WhatsApp, transferidos via streaming para o MinIO (`govflow-documents`), registrados na tabela `whatsapp_schema.tb_mensagens_inbound` e os eventos publicados na exchange `govflow.events` do RabbitMQ.

---

## 2. Como Está Funcionando o WhatsApp Service (TASK-04)

O `whatsapp-service` opera como um **Gateway de Alta Performance e Resiliência Event-Driven**:

```
[ WhatsApp Smartphone ] 
           │
           ▼
[ Evolution API v2.3.7 (:8084) ]
           │ (HTTP POST Webhook - messages.upsert)
           ▼
[ WhatsAppWebhookController (:8083) ]
           │
           ├──▶ [ EvolutionWebhookParser ] ──────────▶ Converte JSON para InboundMessageDto
           ├──▶ [ InboundMessageProcessor ] ─────────▶ Garante Idempotência (< 10ms) via external_message_id
           ├──▶ [ ContactResolutionService ] ────────▶ Normaliza E.164 (9º dígito) e busca prefeitura/tenant
           ├──▶ [ MediaStorageHandler ] ─────────────▶ Streaming direto p/ MinIO (sem buffer em disco)
           │                                            Bucket: govflow-documents
           │                                            Key: raw/whatsapp/{tenantId}/{year}/{month}/{msgId}_{file}
           └──▶ [ RabbitMQ (govflow.events) ] ───────▶ Publica DocumentoRecebidoEvent (X-Tenant-Id header)
```

### Componentes Chave Implementados:
1. **`WhatsAppWebhookController`**:
   - Endpoint `POST /api/v1/whatsapp/webhook`.
   - Responde com `202 Accepted` de forma não bloqueante para evitar timeouts do webhook da Evolution API.
2. **`EvolutionWebhookParser` (Strategy Pattern)**:
   - Desacopla o domínio da Evolution API, permitindo plugar futuros provedores (Meta Cloud API oficial, Z-API) sem alterar a regra de negócio.
   - Extrai texto, tipo de mensagem (`TEXT`, `DOCUMENT`, `IMAGE`, `AUDIO`), dados do remetente e mídias em base64 ou URL pública.
3. **`InboundMessageProcessor`**:
   - Chave de idempotência baseada no `external_message_id` original do WhatsApp. Mensagens repetidas são ignoradas imediatamente com log de auditoria.
   - Salva o registro completo na tabela `whatsapp_schema.tb_mensagens_inbound`.
4. **`ContactResolutionService`**:
   - Normaliza o telefone com algoritmo resiliente de 9º dígito brasileiro (`55 + DDD + 9? + 8 dígitos`).
   - Consulta `whatsapp_schema.tb_contatos_prefeitura` para vincular o remetente ao `prefeitura_id` e `tenant_id`. Se o contato ainda não estiver cadastrado, atribui `unidentified`.
5. **`MediaStorageHandler`**:
   - Download e streaming direto via AWS SDK S3 v2 (`s3Client.putObject(..., RequestBody.fromInputStream(stream, length))`).
   - Zero acúmulo de arquivos temporários em disco e sem sobrecarga na memória Heap da JVM.
6. **Topologia RabbitMQ (`RabbitMQTopologyConfig`)**:
   - Exchange: `govflow.events` (Topic Exchange).
   - Filas declaradas:
     - `fila.documentos.extrair` (binding: `whatsapp.inbound.media`) -> **Consumida pelo AI Service na TASK-05**.
     - `fila.audios.transcrever` (binding: `whatsapp.inbound.audio`).
     - `fila.documentos.dlq` (Dead Letter Queue vinculada com TTL de 7 dias).

---

## 3. O Que É Necessário para a [TASK-05] (AI Service)

### Objetivo da TASK-05
Construir o microsserviço `services/ai-service` em **Python (FastAPI)** com **Clean Pipeline**, **Pydantic v2** e integração multimodal com a API do **Google Gemini (Gemini 2.5 / Flash)**.

### Requisitos Funcionais & Pipeline da TASK-05:
1. **Consumidor AMQP (RabbitMQ Consumer)**:
   - Conectar no broker RabbitMQ (`amqp://govflow:govflow123@rabbitmq:5672/`).
   - Escutar a fila `fila.documentos.extrair`.
   - Ler o payload canônico do evento `DocumentoRecebidoEvent`:
     ```json
     {
       "eventId": "UUID",
       "tenantId": "UUID",
       "prefeituraId": "UUID",
       "mensagemInboundId": "UUID",
       "s3Bucket": "govflow-documents",
       "s3Key": "raw/whatsapp/unidentified/2026/09/e0c044f2..._imagem.jpg",
       "mediaMimeType": "image/jpeg",
       "originalFileName": "nota_fiscal.pdf",
       "fileSizeBytes": 80874,
       "senderPhone": "558387514931",
       "timestamp": "2026-09-16T15:25:37Z"
     }
     ```
2. **Download do Documento do MinIO (S3 Client)**:
   - Baixar o binário usando as credenciais do MinIO (`http://minio:9000`, `minioadmin`, `minioadmin123`).
3. **Extração Multimodal com Gemini e Structured Outputs**:
   - Utilizar o modelo multimodal Gemini (ex: `gemini-2.5-flash` ou `gemini-1.5-flash`).
   - Forçar retorno estrito via JSON Schema / Pydantic v2 com o modelo `DocumentoHabilExtraction`:
     - `tipo_documento`: `NOTA_FISCAL`, `RECIBO`, `FOLHA_PAGAMENTO`, `MEDICAO`, `OUTRO`.
     - `numero_documento`: Número da NF ou documento hábil.
     - `data_emissao`: Data em formato `YYYY-MM-DD`.
     - `credor_cnpj_cpf`: CNPJ ou CPF do fornecedor contratado.
     - `credor_razao_social`: Razão social ou nome do favorecido.
     - `valor_bruto`: `Decimal` (ex: `150000.00`).
     - `retencao_inss`: `Decimal` (ou 0.00).
     - `retencao_iss`: `Decimal` (ou 0.00).
     - `retencao_irrf`: `Decimal` (ou 0.00).
     - `outras_retencoes`: `Decimal` (ou 0.00).
     - `valor_liquido`: `Decimal` (ex: `135000.00`).
     - `bounding_boxes`: Dicionário mapeando os campos com suas coordenadas normalizadas no documento: `[ymin, xmin, ymax, xmax]` (na escala de 0 a 1000) para suporte ao visualizador de PDF no Angular (TASK-07).
4. **Validação Contábil Rígida (Regra Determinística)**:
   - Validação matemática automática:
     $$\text{Valor Bruto} - (\text{INSS} + \text{ISS} + \text{IRRF} + \text{Outras Retenções}) = \text{Valor Líquido}$$
   - Se houver divergência maior que R$ 0,02 (arredondamento), sinalizar flag `divergencia_valores: true` com detalhamento da diferença para revisão humana.
5. **Publicação do Evento de Saída**:
   - Publicar o evento `DocumentoExtraidoEvent` na exchange `govflow.events` com routing key `documento.extraido` para consumo futuro do `core-service` (TASK-06).

---

## 4. Documentação de Referência e Especificações

1. **Arquitetura do AI Service:**
   - [05_arquitetura_ai_service_clean_pipeline.md](file:///c:/projetos/estudo%20spring/govflow/docs/architecture/05_arquitetura_ai_service_clean_pipeline.md)
   - Contém os contratos Pydantic, o diagrama de fluxo da extração e os prompts recomendados.
2. **Backlog Oficial das Tasks:**
   - [clickup_tasks_backlog.md](file:///c:/projetos/estudo%20spring/govflow/docs/clickup_tasks_backlog.md#L90-L105)
3. **Estrutura de Tabelas e Schema do Banco:**
   - [V1__init_whatsapp_schema.sql](file:///c:/projetos/estudo%20spring/govflow/flyway/sql/V1__init_whatsapp_schema.sql)
   - [V2__init_core_schema.sql](file:///c:/projetos/estudo%20spring/govflow/flyway/sql/V2__init_core_schema.sql)

---

## 5. Decisões Técnicas Críticas & Guia de Implementação para a TASK-05

1. **Substituição do Stub no Docker Compose**:
   - O serviço `stub-ai` no [`docker-compose.yml`](file:///c:/projetos/estudo%20spring/govflow/docker-compose.yml#L299-L308) deve ser substituído pelo build do `./services/ai-service`.
2. **Gerenciamento de Segredos e API Key**:
   - Utilizar a variável de ambiente `GEMINI_API_KEY` injetada via `.env` / Docker Compose. Nunca commitar chaves reais no repositório.
3. **Prompt Engineering para Documentos Fiscais Brasileiros**:
   - Incluir instruções claras para NF-e (DANFE - Modelo 55) e NFS-e (Nota Fiscal de Serviços Eletrônica Municipal), destacando onde ficam os campos de retenções de impostos na fonte.
4. **Idempotência no Consumo**:
   - Registrar no header do AMQP ou em base Redis/PostgreSQL o `mensagemInboundId` processado para evitar reextração desnecessária caso a mensagem sofra redelivery pelo broker.
5. **Tratamento de Exceções & Dead Lettering**:
   - Se o arquivo baixado for ilegível, corrompido ou o Gemini rejeitar o formato, enviar mensagem com o motivo do erro para a fila de DLQ (`fila.documentos.dlq`) e atualizar o status em `tb_mensagens_inbound` como `processing_error`.

---

## 6. Suggested Skills para o Próximo Agente

Para executar a **TASK-05** com excelência, o próximo agente deve utilizar:

- **`ai-engineer`**: Boas práticas de Structured Outputs com LLMs multimodais, engenharia de prompts fiscais, orquestração com Pydantic v2 e tratamento de coordenadas/bounding boxes.
- **`clean-code`**: Estrutura modular em FastAPI (separação de consumers AMQP, clients de S3/Gemini, validadores e DTOs).
- **`docker-expert`**: Criação de Dockerfile otimizado para Python/FastAPI (multi-stage ou imagens slim com gerenciamento de dependências via Poetry ou UV/Pip).
- **`commit`**: Convenções de commit semântico padronizadas do repositório ao finalizar a entrega.
