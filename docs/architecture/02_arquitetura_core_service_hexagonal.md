# Especificação Arquitetural: Core Document Service (Hexagonal Architecture / DDD)

**Serviço:** `core-service`  
**Tecnologia:** Java 21 / Spring Boot 3.x / Spring Data JPA / PostgreSQL 16 (`core_schema`) / MinIO/R2  
**Padrão Arquitetural:** **Hexagonal Architecture (Ports & Adapters) com Domain-Driven Design (DDD)**  
**Porta Padrão:** `8081`  

---

## 1. Por Que Arquitetura Hexagonal com DDD no Core Service?

O `core-service` é o **coração transacional do negócio**. Ele custodia as regras de convênios, a hierarquia de municípios, os documentos auditados e a trilha de responsabilidade do analista.

### Benefícios Diretos:
1. **Domínio 100% Puro:** As regras de negócio (ex: cálculo de retenções, validação de vigência de convênio, bloqueio de medição sem nota atestada) residem em entidades Java puras, sem nenhuma anotação do Spring ou do Hibernate.
2. **Testabilidade Extrema:** É possível testar 100% da lógica de negócio com testes unitários em milissegundos, sem precisar carregar o contexto pesado do Spring Boot ou banco em memória.
3. **Desacoplamento de Storage e Mensageria:** Se amanhã o armazenamento mudar de MinIO para Cloudflare R2 ou AWS S3, ou a mensageria mudar de RabbitMQ para Kafka, **zero linhas de código de negócio são alteradas**, apenas os adaptadores externos.

---

## 2. Diagrama Hexagonal do Core Service

```
                     +-------------------------------------------------------------+
                     |                     CORE SERVICE HEXAGON                    |
                     |                                                             |
   [ REST Clients ]  |    +------------------- APPLICATION -------------------+    |  [ PostgreSQL ]
(Angular / Gateway)  |    |                                                   |    | (core_schema)
         │           |    |  Inbound Ports (Use Cases):                       |    |        ▲
         ▼           |    |  - CriarPrefeituraUseCase                         |    |        │
+-----------------+  |    |  - UploadDocumentoUseCase                         |    |  +-----------------+
| Inbound Adapter |──┼───►|  - AprovarRevisaoDocumentoUseCase                 |    |  | Outbound Adapter|
| REST Controller |  |    |                                                   |    |  |  JPA Repository |
+-----------------+  |    |  Outbound Ports (SPI):                            |    |  +-----------------+
                     |    |  - DocumentoRepositoryPort ───────────────────────┼───►|
   [ RabbitMQ ]      |    |  - ObjectStoragePort (S3)  ───────────────────────┼───►|  [ MinIO / R2 ]
(DocumentoExtraido)  |    |  - EventPublisherPort (AMQP) ─────────────────────┼───►| (PDFs & Mídias)
         │           |    |                                                   |    |        ▲
         ▼           |    |         +------------ DOMAIN ------------+        |    |        │
+-----------------+  |    |         | Entities, Aggregates, VO:      |        |    |  +-----------------+
| Inbound Adapter |──┼───►|         | Prefeitura, Convenio,          |        |    |  | Outbound Adapter|
| Rabbit Listener |  |    |         | Documento, ExtracaoRevisao,    |        |    |  |  S3 Client / AMQP|
+-----------------+  |    |         | ChecklistItem, Regras de Negóc |        |    |  +-----------------+
                     |    |         +--------------------------------+        |    |
                     |    +---------------------------------------------------+    |
                     +-------------------------------------------------------------+
```

---

## 3. Estrutura de Pacotes

```
services/core-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/govflow/core/
    │   │   ├── domain/                               # NÚCLEO DO HEXÁGONO (Zero Dependências)
    │   │   │   ├── model/                            # Agregados, Entidades e Value Objects
    │   │   │   │   ├── Prefeitura.java
    │   │   │   │   ├── Convenio.java
    │   │   │   │   ├── Documento.java                # Agregado com status, metadados e revisões
    │   │   │   │   ├── ExtracaoCampos.java           # Value Object com campos do Transferegov
    │   │   │   │   ├── StatusDocumento.java          # ENUM (RECEBIDO, EM_ANALISE, REVISADO, SUBMETIDO)
    │   │   │   │   └── AuditoriaRevisao.java         # Value Object (analistaId, dataAprovacao, diff)
    │   │   │   ├── event/                            # Eventos de Domínio Puros
    │   │   │   │   ├── DocumentoCriadoEvent.java
    │   │   │   │   └── DocumentoRevisadoEvent.java
    │   │   │   └── exception/
    │   │   │       ├── ConvenioExpiradoException.java
    │   │   │       └── DocumentoInvalidoException.java
    │   │   │
    │   │   ├── application/                          # CASOS DE USO E PORTAS
    │   │   │   ├── port/
    │   │   │   │   ├── in/                           # Portas de Entrada (Use Cases)
    │   │   │   │   │   ├── UploadDocumentoUseCase.java
    │   │   │   │   │   ├── SalvarExtracaoPreliminarUseCase.java
    │   │   │   │   │   ├── AprovarRevisaoDocumentoUseCase.java
    │   │   │   │   │   └── ConsultarConveniosUseCase.java
    │   │   │   │   └── out/                          # Portas de Saída
    │   │   │   │       ├── DocumentoRepositoryPort.java
    │   │   │   │       ├── ConvenioRepositoryPort.java
    │   │   │   │       ├── ObjectStoragePort.java
    │   │   │   │       └── EventPublisherPort.java
    │   │   │   └── service/                          # Implementação dos Use Cases
    │   │   │       ├── UploadDocumentoService.java
    │   │   │       ├── ProcessarRevisaoService.java
    │   │   │       └── ConvenioQueryService.java
    │   │   │
    │   │   └── infrastructure/                       # ADAPTADORES EXTERNOS
    │   │       ├── adapter/
    │   │       │   ├── in/
    │   │       │   │   ├── rest/                     # Adaptador REST (Controllers)
    │   │       │   │   │   ├── DocumentoController.java
    │   │       │   │   │   ├── ConvenioController.java
    │   │       │   │   │   └── dto/                  # Request / Response DTOs
    │   │       │   │   └── amqp/                     # Adaptador Mensageria Entrada
    │   │       │   │       └── DocumentoExtraidoListener.java
    │   │       │   └── out/
    │   │       │       ├── persistence/              # Adaptador de Banco (PostgreSQL)
    │   │       │       │   ├── entity/               # Entidades JPA (com anotações Hibernate)
    │   │       │       │   │   ├── DocumentoJpaEntity.java
    │   │       │       │   │   └── ConvenioJpaEntity.java
    │   │       │       │   ├── repository/           # Spring Data JPA
    │   │       │       │   │   └── SpringDataDocumentoRepository.java
    │   │       │       │   ├── mapper/               # Mapeamento JPA Entity <-> Domain Model
    │   │       │       │   │   └── DocumentoPersistenceMapper.java
    │   │       │       │   └── DocumentoRepositoryAdapter.java (implements DocumentoRepositoryPort)
    │   │       │       ├── storage/                  # Adaptador MinIO / R2 (S3)
    │   │       │       │   └── S3ObjectStorageAdapter.java (implements ObjectStoragePort)
    │   │       │       └── amqp/                     # Adaptador Mensageria Saída
    │   │       │           └── RabbitMQEventPublisherAdapter.java (implements EventPublisherPort)
    │   │       └── config/
    │   │           ├── S3Config.java
    │   │           ├── RabbitMQConfig.java
    │   │           └── MultiTenantJpaConfig.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/                         # Flyway Migrations (core_schema)
    │           ├── V1__init_core_schema.sql
    │           └── V2__create_documentos_tables.sql
```

---

## 4. O Agregado Principal: `Documento` (Regra de Negócio Pura)

```java
public class Documento {
    private final UUID id;
    private final UUID tenantId;
    private final UUID convenioId;
    private final String nomeArquivoOriginal;
    private final String s3Key;
    private StatusDocumento status;
    private ExtracaoCampos camposExtraidos;
    private AuditoriaRevisao auditoria;

    // Construtor e comportamentos de domínio
    public void registrarExtracaoIA(ExtracaoCampos extracao) {
        if (this.status == StatusDocumento.REVISADO) {
            throw new IllegalStateException("Documento já revisado não pode ser sobrescrito por extração preliminar.");
        }
        this.camposExtraidos = extracao;
        this.status = StatusDocumento.EM_CONFERENCIA;
    }

    public void aprovarRevisao(UUID analistaId, ExtracaoCampos camposAprovados, String observacao) {
        camposAprovados.validarConsistencia(); // Valida se os campos obrigatórios do Transferegov estão preenchidos
        this.camposExtraidos = camposAprovados;
        this.auditoria = new AuditoriaRevisao(analistaId, LocalDateTime.now(), observacao);
        this.status = StatusDocumento.PRONTO_PARA_TRANSFEREGOV;
    }
}
```

---

## 5. Multi-Tenancy e Isolamento de Dados

Para cumprir o **ADR-008**:
1. Todas as entidades JPA herdam de uma classe base `@MappedSuperclass`:
   ```java
   @MappedSuperclass
   public abstract class BaseTenantEntity {
       @TenantId // Anotação nativa do Hibernate 6
       @Column(name = "tenant_id", nullable = false, updatable = false)
       private UUID tenantId;
   }
   ```
2. Um `TenantInterceptor` lê o cabeçalho `X-Tenant-Id` injetado pelo API Gateway e registra no contexto da thread (`CurrentTenantContext`), garantindo que **nenhuma query SQL possa vazar dados entre consultorias concorrentes**.
