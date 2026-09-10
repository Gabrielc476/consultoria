# Especificação Arquitetural: Transferegov Service (Event-Driven & CQRS-Light)

**Serviço:** `transferegov-service`  
**Tecnologia:** Java 21 / Spring Boot 3.x / Spring RestClient / Spring Data JPA / PostgreSQL 16 (`transferegov_schema`)  
**Padrão Arquitetural:** **Event-Driven com CQRS-Light (Segregação de Ingestão e Consulta)**  
**Porta Padrão:** `8082`  

---

## 1. Por Que CQRS-Light com Ingestão Dupla (CSV Streaming + REST APIs)?

O `transferegov-service` possui duas responsabilidades com naturezas operacionais completamente distintas:

1. **Lado de Escrita / Ingestão Dupla (Command/ETL Batch):**
   - **Módulo Discricionárias e Legais (SICONV):** Conforme comprovado na auditoria técnica de dados abertos, **não existe API REST para consulta livre de convênios discricionários**. O Governo Federal publica dumps diários relacionais em **65 arquivos ZIP/CSV compactados** no Azure Blob Storage (`api-publica.transferegov.gestao.gov.br/downloads/dadosgov/`) gerados todo dia às 06h36 BRT. O serviço executa um job matinal agendado (`@Scheduled`) que faz o **streaming e descompactação em memória** desses arquivos, filtrando exclusivamente os registros da Paraíba (`UF = 'PB'`) ou dos CNPJs clientes, gravando no banco em menos de 1 minuto!
   - **Módulo Especiais (Emendas Pix):** Utiliza a **API REST pública** (`/especiais`) para sincronizar planos de trabalho e relatórios de gestão auditados pelo STF.
   - Detecta mudanças de estado (prorrogações de vigência, novos termos aditivos, prazos de cláusula suspensiva vencendo) e dispara eventos no RabbitMQ.
2. **Lado de Leitura / Servidor de Payloads (Query):**
   - Consumido em tempo real pelo **Frontend Angular** e pela **Extensão do Chrome**;
   - Precisa de latência ultrabaixa (< 50ms) para entregar o JSON pronto para injeção no DOM do formulário de *Documento Hábil*.

---

## 2. Diagrama de Fluxo CQRS-Light com Ingestão Dupla

```
+--------------------------------------------------------------------------------------------------+
|                                    TRANSFEREGOV SERVICE                                          |
|                                                                                                  |
| [ LADO DE ESCRITA & SINCRONIZAÇÃO DUPLA (ETL) ]                                                  |
|                                                                                                  |
|   1. Azure Blob Storage (SICONV)                                                                 |
|      (siconv_convenio.zip, siconv_proponentes.zip, siconv_empenho.zip)                           |
|            │                                                                                     |
|            ▼                                                                                     |
|      SiconvCsvStreamingPipeline (Filtro por UF='PB' em Memória) ──► transferegov_schema          |
|                                                                         (Tabelas Locais)         |
|   2. API REST Federal (/especiais - Emendas Pix) ───────────────►               ▲                |
|                                                                                 │                |
|                                                                                 ▼ Dispara Evento |
|                                                                           RabbitMQ (AlertaPrazo) |
|                                                                                                  |
| [ LADO DE LEITURA & EXTENSÃO CHROME (QUERY) ]                                                    |
|   Extensão Chrome ──────► Gateway ───────────► ExtensionPayloadQueryService                      |
|                                                        ▲                                         |
|                                                        │ Leitura Direta de Projeção Otimizada     |
|                                                 transferegov_schema (View / Materialized DTO)    |
+--------------------------------------------------------------------------------------------------+
```

---

## 3. Estrutura de Pacotes

```
services/transferegov-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/govflow/transferegov/
    │   │   ├── sync/                                 # LADO DE COMANDO / INGESTÃO (Write Side)
    │   │   │   ├── client/                           # Cliente HTTP das APIs Abertas do Governo
    │   │   │   │   ├── TransferegovPublicApiClient.java (Spring RestClient)
    │   │   │   │   └── dto/                          # DTOs retornados pela API Federal
    │   │   │   │       ├── PropostaFederalDTO.java
    │   │   │   │       ├── ParceriaFederalDTO.java
    │   │   │   │       └── AnaliseFederalDTO.java
    │   │   │   ├── scheduler/                        # Jobs Agendados
    │   │   │   │   └── DailySyncPropostasScheduler.java
    │   │   │   ├── pipeline/                         # Pipeline de Processamento e Detecção de Diff
    │   │   │   │   ├── SyncConveniosPipeline.java
    │   │   │   │   └── DeadlineDetectorService.java  # Calcula alertas de prazos (15, 7, 2 dias)
    │   │   │   └── persistence/                      # Gravação dos Dados Sincronizados
    │   │   │       ├── entity/
    │   │   │       │   ├── SincronizacaoConvenioEntity.java
    │   │   │       │   └── AlertaPrazoEntity.java
    │   │   │       └── repository/
    │   │   │           └── SincronizacaoConvenioRepository.java
    │   │   │
    │   │   ├── query/                                # LADO DE CONSULTA (Read Side / Extensão Chrome)
    │   │   │   ├── controller/                       # Endpoints para Frontend e Extensão
    │   │   │   │   ├── ExtensionPayloadController.java
    │   │   │   │   └── TransferegovDashboardController.java
    │   │   │   ├── service/                          # Serviços de Projeção de Dados
    │   │   │   │   └── DocumentoHabilPayloadQueryService.java
    │   │   │   └── dto/                              # DTOs formatados exatamente para o DOM da Extensão
    │   │   │       ├── DocumentoHabilExtensionPayload.java
    │   │   │       └── TransferegovTimelineDTO.java
    │   │   │
    │   │   ├── event/                                # EVENTOS DE DOMÍNIO E MENSAGERIA
    │   │   │   ├── producer/                         # Dispara eventos para o RabbitMQ
    │   │   │   │   └── TransferegovEventPublisher.java
    │   │   │   └── model/
    │   │   │       ├── AlertaPrazoExpirandoEvent.java
    │   │   │       └── NovoParecerCaixaEvent.java
    │   │   │
    │   │   └── config/
    │   │       ├── RestClientConfig.java             # Timeouts, pooling e headers da API Federal
    │   │       ├── RabbitMQTransfereConfig.java
    │   │       └── DatabaseConfig.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/                         # Flyway Migrations (transferegov_schema)
    │           └── V1__init_transferegov_schema.sql
```

---

## 4. O Contrato de Dados para a Extensão do Chrome (`DocumentoHabilExtensionPayload`)

Este é o payload JSON que o endpoint `GET /api/v1/transferegov/payload-extensao/{documentoId}` retorna diretamente para a extensão preencher a tela oficial:

```json
{
  "documentoId": "c4719491-1f4e-411f-9e16-c84b3ed52031",
  "convenioNumero": "912345/2024",
  "municipio": "Massaranduba-PB",
  "camposFormulario": {
    "tp_documento_habil": "NOTA_FISCAL",
    "nr_documento_habil": "0001542",
    "dt_emissao": "2026-08-20",
    "vl_documento_habil": 85400.00,
    "cd_credor_devedor": "08123456000190",
    "nm_credor_devedor": "CONSTRUTORA EXEMPLO LTDA",
    "nr_empenho_dh": "2024NE00034",
    "tx_observacao": "Medição 02 referente à pavimentação da Rua Principal - Convênio 912345."
  },
  "retencoes": [
    { "tipo": "INSS", "aliquota": 11.0, "valor": 9394.00 },
    { "tipo": "ISS", "aliquota": 5.0, "valor": 4270.00 }
  ],
  "metaEtapaSugerida": {
    "meta": "01",
    "etapa": "02 - Pavimentação Asfáltica",
    "valorEtapa": 85400.00
  },
  "statusRevisao": "APROVADO_POR_ANALISTA",
  "aprovadoPor": "Gabriel - Analista Pleno"
}
```
Com esse contrato imutável, o Content Script da extensão do Chrome percorre o objeto `camposFormulario` e localiza os seletores HTML correspondentes em milissegundos.
