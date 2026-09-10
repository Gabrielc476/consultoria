# Especificação Arquitetural: AI Service (Clean Pipeline & Fallback Strategy)

**Serviço:** `ai-service`  
**Tecnologia:** Python 3.12+ / FastAPI / Pydantic v2 / Google GenAI SDK (`google-genai`) / aio-pika (RabbitMQ) / Boto3 (MinIO)  
**Padrão Arquitetural:** **Clean Pipeline Architecture (Arquitetura Limpa em Camadas + Strategy Pattern para Provedores LLM)**  
**Porta Padrão:** `8000`  

---

## 1. Por Que Clean Pipeline com Strategy Pattern no AI Service?

Modelos e APIs de IA evoluem em ritmo alucinante. Hoje o modelo campeão em custo e visão é o **Gemini 3.x Flash-Lite**. No entanto, um serviço de missão crítica em GovTech precisa garantir:
1. **Zero Acoplamento com um Único Fornecedor:** O código de negócio não pode ter chamadas espalhadas do tipo `google.generativeai` ou `openai`. Tudo deve passar por uma abstração limpa (`LLMProviderStrategy`).
2. **Fallback Automático e Transparente:** Se a API do Google sofrer rate limit (HTTP 429) ou um documento escaneado tiver baixa confiança (< 75%), o pipeline automaticamente aciona um modelo de fallback superior (Gemini 3.7 Flash ou GPT-4o-mini).
3. **Padrão Pipeline de Processamento:** Cada documento passa por etapas claras e testáveis: *Download da Mídia -> Injeção de Regras Aprendidas (Few-Shot) -> Inferência VLM -> Validação Matemática do Schema -> Cálculo de Confiança*.

---

## 2. Diagrama do Pipeline de Processamento de Documentos

```
+----------------------------------------------------------------------------------------------------+
|                                    DOCUMENT EXTRACTION PIPELINE                                    |
|                                                                                                    |
|  1. RECEPÇÃO DA TAREFA (RabbitMQ / HTTP)                                                           |
|     Payload: { "documentoId": "...", "s3Key": "...", "cnpjPrefeitura": "..." }                    |
|                │                                                                                   |
|                ▼                                                                                   |
|  2. DOWNLOAD DA MÍDIA & RECUPERAÇÃO DE REGRAS APRENDIDAS                                           |
|     - Boto3 baixa o PDF/Imagem do MinIO em memória (BytesIO)                                       |
|     - SQLAlchemy busca regras Few-Shot daquele CNPJ/Fornecedor (Active Learning)                   |
|                │                                                                                   |
|                ▼                                                                                   |
|  3. EXECUÇÃO DA ESTRATÉGIA PRIMÁRIA (Gemini 3.x Flash-Lite)                                        |
|     - Envia PDF cru + Prompt com Pydantic Schema estrito do Transferegov                           |
|     - Retorna: Campos estruturados + Coordenadas das Bounding Boxes [ymin, xmin, ymax, xmax]       |
|                │                                                                                   |
|                ├───► Confiança Geral >= 75%? ──────────► [ OK: Segue para Validação ]              |
|                │                                                                                   |
|                └───► Confiança < 75% ou Erro API ──────► [ ATIVAÇÃO DE FALLBACK ]                  |
|                                                                 │                                  |
|                                                                 ▼                                  |
|                                                     Estratégia Fallback:                           |
|                                                     Gemini 3.7 Flash ou GPT-4o-mini                |
|                                                                 │                                  |
|                ┌────────────────────────────────────────────────┘                                  |
|                ▼                                                                                   |
|  4. AUDITORIA MATEMÁTICA & REGRAS DE NEGÓCIO                                                       |
|     - Valida se Soma dos Itens da Medição == Valor Bruto da NF                                     |
|     - Valida Alíquotas de INSS e ISS                                                               |
|                │                                                                                   |
|                ▼                                                                                   |
|  5. DISPARO DO RESULTADO                                                                           |
|     - Publica evento 'DocumentoProcessadoEvent' no RabbitMQ                                        |
|     - Notifica o Core Service para atualizar a tela de conferência do Analista                     |
+----------------------------------------------------------------------------------------------------+
```

---

## 3. Estrutura de Diretórios do Microsserviço Python

```
services/ai-service/
├── pyproject.toml / requirements.txt
├── Dockerfile
└── src/
    ├── domain/                               # MODELOS DE DOMÍNIO E SCHEMAS PYDANTIC (PUROS)
    │   ├── schemas/
    │   │   ├── documento_habil_schema.py     # Schema estrito do Transferegov (campos oficiais)
    │   │   ├── audio_intent_schema.py        # Schema de intenção e resumo de voz
    │   │   ├── bounding_box_schema.py        # Coordenadas [ymin, xmin, ymax, xmax] normalizadas
    │   │   └── confidence_report.py          # Score de confiança (0.0 a 1.0) por campo
    │   └── rules/
    │       └── financial_rules.py            # Validação pura de impostos e somatórios
    │
    ├── application/                          # PIPELINES E CASOS DE USO
    │   ├── pipelines/
    │   │   ├── document_pipeline.py          # Orquestra as etapas de extração de PDFs
    │   │   └── audio_pipeline.py             # Orquestra a transcrição de voz do WhatsApp
    │   └── services/
    │       └── active_learning_service.py    # Grava e recupera correções dos analistas
    │
    ├── infrastructure/                       # ADAPTADORES EXTERNOS E CLIENTS
    │   ├── llm/                              # Strategy Pattern para Provedores de IA
    │   │   ├── base_provider.py              # Interface abstrata (BaseLLMProvider)
    │   │   ├── gemini_provider.py            # Implementação primária (SDK google-genai)
    │   │   └── fallback_provider.py          # Implementação de contingência (OpenAI / Gemini 3.7)
    │   ├── storage/
    │   │   └── s3_client.py                  # Integração com MinIO / Cloudflare R2
    │   ├── messaging/
    │   │   ├── rabbitmq_consumer.py          # Worker assíncrono aio-pika
    │   │   └── rabbitmq_publisher.py
    │   └── db/                               # PostgreSQL para memória de correções
    │       ├── models.py                     # Tabela 'template_corrections'
    │       └── session.py
    │
    └── main.py                               # Inicialização do FastAPI e rotas REST
```

---

## 4. O Schema Pydantic Estrito do Transferegov (`documento_habil_schema.py`)

O Gemini 3.x Flash-Lite recebe este schema via `response_schema` para garantir que o retorno seja **sempre um JSON válido e tipado**:

```python
from pydantic import BaseModel, Field
from typing import List, Optional

class BoundingBox(BaseModel):
    ymin: int = Field(description="Coordenada Y mínima normalizada (0 a 1000)")
    xmin: int = Field(description="Coordenada X mínima normalizada (0 a 1000)")
    ymax: int = Field(description="Coordenada Y máxima normalizada (0 a 1000)")
    xmax: int = Field(description="Coordenada X máxima normalizada (0 a 1000)")

class ExtractedField(BaseModel):
    valor: str
    confianca: float = Field(description="Score de confiança de 0.0 a 1.0", ge=0.0, le=1.0)
    coordenadas: Optional[BoundingBox] = None

class RetencaoTributaria(BaseModel):
    tipo: str = Field(description="INSS, ISS, IRRF, etc.")
    aliquota: float
    valor: float
    confianca: float

class DocumentoHabilExtraction(BaseModel):
    tipo_documento: ExtractedField = Field(description="Ex: NOTA_FISCAL, RECIBO, MEDICAO")
    numero_documento: ExtractedField
    data_emissao: ExtractedField = Field(description="Formato YYYY-MM-DD")
    valor_bruto: ExtractedField
    cnpj_credor: ExtractedField = Field(description="CNPJ sem formatação (apenas números)")
    razao_social_credor: ExtractedField
    numero_empenho: Optional[ExtractedField] = None
    descricao_servico: ExtractedField
    retencoes: List[RetencaoTributaria] = []
    alertas_inconsistencia: List[str] = Field(default=[], description="Erros matemáticos detectados")
```

---

## 5. Implementação do Provedor Primário com SDK `google-genai`

```python
from google import genai
from google.genai import types
from domain.schemas.documento_habil_schema import DocumentoHabilExtraction

class GeminiProvider(BaseLLMProvider):
    def __init__(self, api_key: str):
        self.client = genai.Client(api_key=api_key)
        self.model_name = "gemini-3.1-flash-lite"

    async def extract_document(self, pdf_bytes: bytes, prompt_context: str) -> DocumentoHabilExtraction:
        response = self.client.models.generate_content(
            model=self.model_name,
            contents=[
                types.Part.from_bytes(data=pdf_bytes, mime_type="application/pdf"),
                prompt_context
            ],
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=DocumentoHabilExtraction,
                temperature=0.0 # Temperatura zero para determinismo total de dados fiscais
            )
        )
        return DocumentoHabilExtraction.model_validate_json(response.text)
```
Com `temperature=0.0` e `response_schema`, o modelo elimina alucinações e retorna os dados perfeitamente mapeados para a tela de conferência do analista.
