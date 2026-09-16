# GovFlow AI Service

Microsserviço Python (FastAPI) de **extração multimodal de Documento Hábil** com Gemini 3.x, schema Pydantic estrito, validação matemática determinística e integração RabbitMQ/MinIO.

Arquitetura: **Clean Pipeline + Strategy Pattern** ([docs/architecture/05_arquitetura_ai_service_clean_pipeline.md](../../docs/architecture/05_arquitetura_ai_service_clean_pipeline.md)).

## Endpoints

| Método | Path | Descrição |
| --- | --- | --- |
| `GET` | `/health` | Liveness (não exige `GEMINI_API_KEY`) |
| `POST` | `/api/v1/ai/extract` | Extração síncrona (mesmo pipeline do consumer) |

## Filas

| Fila | Evento | Direção |
| --- | --- | --- |
| `fila.documentos.extrair` | `DocumentoRecebidoEvent` | entrada |
| `fila.documentos.processados` | `DocumentoExtraidoEvent` | saída |

## Variáveis

Copie [`.env.example`](.env.example). A única credencial externa pendente é:

```env
GEMINI_API_KEY=sua-chave-do-ai-studio
```

Obtenha em: https://aistudio.google.com/app/apikey

## Desenvolvimento local

```powershell
cd services/ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
pytest
uvicorn main:app --app-dir src --reload --port 8000
```

## Critérios de aceite (TASK-05)

- [x] Extração tipada: tipo, NF, data, CNPJ/razão, bruto, retenções (INSS/ISS/IRRF)
- [x] Bounding boxes normalizadas `[ymin,xmin,ymax,xmax]` (0–1000)
- [x] Alerta determinístico se bruto − retenções ≠ líquido
- [x] Publicação de `DocumentoExtraidoEvent` no RabbitMQ

Evidência de aceite e regressão (fallback 503): [`docs/audits/test_results.json`](../../docs/audits/test_results.json).
