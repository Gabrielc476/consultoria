# Especificação Arquitetural: WhatsApp Service (Event-Driven & Strategy Pattern)

**Serviço:** `whatsapp-service`  
**Tecnologia:** Java 21 / Spring Boot 3.x / Spring AMQP (RabbitMQ) / Evolution API Gateway / PostgreSQL 16 (`whatsapp_schema`)  
**Padrão Arquitetural:** **Event-Driven Architecture (EDA) com Strategy Pattern para Provedores de WhatsApp**  
**Porta Padrão:** `8083`  

---

## 1. Por Que Event-Driven com Strategy Pattern no WhatsApp Service?

O canal do WhatsApp possui duas particularidades críticas:
1. **Natureza Totalmente Assíncrona e Baseada em Eventos (Event-Driven):**  
   Mensagens, áudios e arquivos chegam como *Webhooks HTTP* em picos imprevisíveis. O serviço deve apenas receber o payload, validar a assinatura, salvar a mídia no MinIO/R2 e disparar um evento assíncrono para o RabbitMQ em menos de 100ms, liberando a conexão sem travar o gateway.
2. **Volatilidade de Provedores de API (Strategy Pattern):**  
   Hoje usamos a **Evolution API (Self-Hosted)** via QR Code para desenvolvimento rápido com custo zero. No futuro, a consultoria pode migrar para a **API Oficial da Meta (Cloud API)** ou **Z-API**. O Strategy Pattern isola o formato dos payloads externos da regra de negócio do sistema.

---

## 2. Diagrama de Fluxo de Mensageria e Estratégia

```
                                  FLUXO DE ENTRADA (INBOUND)
[ Secretário no WhatsApp ]
           │
           ▼
[ Evolution API Gateway ] ──► Webhook HTTP ──► WebhookController
                                                      │
                                                      ▼
                                           EvolutionApiStrategy (Normaliza DTO)
                                                      │
                                                      ▼
                                           MediaDownloadService (Baixa mídias -> MinIO)
                                                      │
                                                      ▼
                                           RabbitMQ (Publica 'MidiaRecebidaEvent')
                                                      │
                                    ┌─────────────────┴─────────────────┐
                                    ▼                                   ▼
                        fila.audios.transcrever             fila.documentos.extrair
                          (Consumido pela IA)                 (Consumido pela IA)


                                  FLUXO DE SAÍDA (OUTBOUND)
[ Analista no Angular ] ──► Clica em "Aprovar Resposta Sugerida"
                                       │
                                       ▼
                              OutboundMessageService
                                       │
                                       ▼
                       WhatsAppSenderStrategy (Interface)
                         ├── EvolutionApiSenderStrategy (Ativo no MVP)
                         └── MetaCloudApiSenderStrategy (Futuro)
                                       │
                                       ▼
                              [ Envia no WhatsApp ]
```

---

## 3. Estrutura de Pacotes

```
services/whatsapp-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/br/com/govflow/whatsapp/
    │   │   ├── inbound/                              # PROCESSAMENTO DE MENSAGENS RECEBIDAS
    │   │   │   ├── controller/
    │   │   │   │   └── WhatsAppWebhookController.java
    │   │   │   ├── parser/                           # Estratégias de Leitura de Webhook
    │   │   │   │   ├── WebhookParserStrategy.java    # Interface Comum
    │   │   │   │   ├── EvolutionWebhookParser.java   # Parser para Evolution API (v2)
    │   │   │   │   └── MetaCloudWebhookParser.java   # Parser para Meta Cloud API
    │   │   │   ├── service/
    │   │   │   │   ├── InboundMessageProcessor.java
    │   │   │   │   └── MediaStorageHandler.java      # Faz stream do áudio/PDF para o MinIO
    │   │   │   └── event/                            # Eventos AMQP de Entrada
    │   │   │       ├── AudioRecebidoEvent.java
    │   │   │       └── DocumentoRecebidoEvent.java
    │   │   │
    │   │   ├── outbound/                             # ENVIO DE MENSAGENS E COBRANÇAS
    │   │   │   ├── controller/
    │   │   │   │   └── MessageDispatchController.java # Chamado pelo Frontend Angular
    │   │   │   ├── service/
    │   │   │   │   └── MessageDispatchService.java
    │   │   │   └── strategy/                         # Estratégias de Disparo
    │   │   │       ├── WhatsAppSenderStrategy.java   # Interface Comum
    │   │   │       ├── EvolutionSenderStrategy.java  # HTTP Client para Evolution
    │   │   │       └── MetaCloudSenderStrategy.java  # HTTP Client para Meta Cloud API
    │   │   │
    │   │   ├── routing/                              # ROTEAMENTO POR MUNICÍPIO / CONVÊNIO
    │   │   │   ├── service/
    │   │   │   │   └── ContactResolutionService.java # Mapeia número do WhatsApp -> Prefeitura
    │   │   │   └── model/
    │   │   │       └── ContatoPrefeituraEntity.java
    │   │   │
    │   │   └── config/
    │   │       ├── RabbitMQWhatsAppConfig.java       # Declara exchanges, filas e DLQs
    │   │       ├── WhatsAppProviderConfig.java       # Injeta a Strategy ativa via application.yml
    │   │       └── S3ClientConfig.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/                         # Flyway Migrations (whatsapp_schema)
    │           └── V1__init_whatsapp_schema.sql
```

---

## 4. O Padrão Strategy na Prática

### 4.1 Interface Agnóstica de Envio (`WhatsAppSenderStrategy`):
```java
public interface WhatsAppSenderStrategy {
    String getProviderName(); // "EVOLUTION" ou "META"
    
    Mono<MessageSentResult> sendTextMessage(String toPhoneNumber, String messageText);
    
    Mono<MessageSentResult> sendMediaDocument(String toPhoneNumber, String s3FileUrl, String caption);
}
```

### 4.2 Configuração Dinâmica via Properties (`application.yml`):
```yaml
govflow:
  whatsapp:
    active-provider: EVOLUTION # Alterne para META sem mudar código
    evolution:
      base-url: http://evolution-api:8084
      api-key: ${EVOLUTION_API_KEY}
      instance-name: govflow-consultoria
```

---

## 5. Resiliência e Prevenção de Perdas de Mídias

1. **Upload em Streaming Imediato:** Quando o secretário manda um áudio ou PDF, o `MediaStorageHandler` grava diretamente no MinIO gerando uma `s3_key` segura (ex: `temp/whatsapp/massaranduba/audio_123.ogg`).
2. **Desacoplamento por Filas no RabbitMQ:**
   - O Webhook responde HTTP 200 para a Evolution API em menos de **80 milissegundos**.
   - A tarefa pesada de ler o documento é enfileirada:
     - `fila.whatsapp.audios` -> Dead Letter Exchange em caso de falha.
     - `fila.whatsapp.documentos` -> Retry com *exponential backoff* até 3 tentativas.
