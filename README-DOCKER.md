# 🐳 Guia de Infraestrutura Docker: GovFlow + Evolution API + MinIO + Flyway

Este repositório possui uma infraestrutura local completa baseada em Docker Compose, projetada para suportar a arquitetura corporativa do **GovFlow** e seu microsserviço de WhatsApp (`whatsapp-service`).

---

## 📦 Serviços Orquestrados

| Serviço | Imagem | Porta Host | Finalidade |
| :--- | :--- | :--- | :--- |
| **`postgres`** | `postgres:16-alpine` | `5432` | Banco relacional com databases `govflow` (App) e `evolution` (Prisma). |
| **`flyway`** | `flyway/flyway:10-alpine` | - | Aplicação automatizada de migrations versionadas nos schemas do GovFlow. |
| **`minio`** | `minio/minio:RELEASE...` | `9000` (API) / `9001` (Console) | Object Storage S3 compatível para mídias do WhatsApp e notas fiscais. |
| **`minio-init`** | `minio/minio:latest` | - | Provisionamento automático dos buckets (`evolution` e `govflow-documents`). |
| **`redis`** | `redis:7-alpine` | `6379` | Cache de sessão, filas e message buffering para a Evolution API. |
| **`evolution-api`** | `evoapicloud/evolution-api:v2.3.7` | `8084` | Gateway RESTful/WebSocket para conexão multi-instância com o WhatsApp. |

---

## 🚀 Como Inicializar

### 1. Pré-requisitos
* Docker Desktop instalado e em execução (WSL 2 habilitado no Windows).

### 2. Subir todos os serviços
No terminal PowerShell ou Bash, na raiz do projeto:

```powershell
docker compose up -d
```

### 3. Verificar o status dos containers
```powershell
docker compose ps
```

---

## 🗄️ Gerenciamento de Migrations com o Flyway

As migrations residem na pasta:
```
flyway/sql/
├── V1__init_whatsapp_schema.sql  # Tabelas de instâncias, contatos e mensagens
└── V2__init_core_schema.sql      # Tabelas de prefeituras, convênios e contratos
```

### Como o Flyway funciona no Compose:
* Ao rodar `docker compose up`, o container `flyway` aguarda o `postgres` ficar totalmente saudável (*healthy*), executa o comando `migrate` e finaliza com status `0` (concluído).

### Comandos úteis do Flyway:
* **Ver logs da migração:**
  ```powershell
  docker compose logs flyway
  ```

* **Consultar o status de todas as migrations (`info`):**
  ```powershell
  docker compose run --rm flyway info
  ```

* **Adicionar uma nova migração:**
  1. Crie o arquivo `flyway/sql/V3__nome_da_migration.sql`.
  2. Execute:
     ```powershell
     docker compose run --rm flyway migrate
     ```

---

## 🪣 Acesso ao MinIO (Object Storage S3)

* **Painel Web (Console):** [http://localhost:9001](http://localhost:9001)
* **Usuário:** `minioadmin`
* **Senha:** `minioadmin123`
* **Buckets pré-configurados automaticamente:**
  * `evolution`: Armazena áudios (PTT), PDFs e imagens recebidas do WhatsApp.
  * `govflow-documents`: Armazena notas fiscais e relatórios processados pela IA.

---

## 📲 Utilizando a Evolution API v2

* **Endereço da API / Swagger:** [http://localhost:8084](http://localhost:8084)
* **Chave Mestra de Autenticação (`apikey`):** `GovFlowSuperSecretApiKey2026`

### 1. Criar uma Instância para a Consultoria
Execute no terminal ou no Postman/Insomnia:

```powershell
curl -X POST "http://localhost:8084/instance/create" `
  -H "Content-Type: application/json" `
  -H "apikey: GovFlowSuperSecretApiKey2026" `
  -d '{
    "instanceName": "govflow-consultoria",
    "token": "token-seguro-instancia-123",
    "qrcode": true,
    "integration": "WHATSAPP-BAILEYS"
  }'
```

### 2. Obter o QR Code para conectar o WhatsApp
```powershell
curl -X GET "http://localhost:8084/instance/connect/govflow-consultoria" `
  -H "apikey: GovFlowSuperSecretApiKey2026"
```
*(Abra o WhatsApp no celular > Aparelhos conectados > Conectar um aparelho e aponte a câmera para o QR Code gerado).*

### 3. Testar Envio de Mensagem de Texto
```powershell
curl -X POST "http://localhost:8084/message/sendText/govflow-consultoria" `
  -H "Content-Type: application/json" `
  -H "apikey: GovFlowSuperSecretApiKey2026" `
  -d '{
    "number": "5583999999999",
    "text": "Olá! Teste de integração do GovFlow via Evolution API v2."
  }'
```

---

## 🛑 Como Parar ou Reiniciar

* **Parar os containers sem perder dados:**
  ```powershell
  docker compose stop
  ```

* **Destruir containers e recriar mantendo os volumes de dados:**
  ```powershell
  docker compose down
  docker compose up -d
  ```

* **Resetar completamente o ambiente (apaga bancos e arquivos de mídia):**
  ```powershell
  docker compose down -v
  docker compose up -d
  ```
