from google import genai
from google.genai import types

from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from infrastructure.config.settings import GeminiApiKeyMissingError
from infrastructure.llm.base_provider import BaseLLMProvider

EXTRACTION_PROMPT = """
Extraia os campos fiscais oficiais do Documento Hábil / Nota Fiscal brasileira.
Retorne JSON tipado conforme o schema.
Use coordenadas de bounding box normalizadas [ymin, xmin, ymax, xmax] na escala 0-1000.
Inclua page_number (1-based) em cada bounding box.
CNPJ apenas com dígitos. Datas em YYYY-MM-DD. Valores monetários com ponto decimal (ex: 85400.00).
tipo_documento deve ser um de: NOTA_FISCAL_SERVICOS, NOTA_FISCAL_MERCADORIAS, RECIBO_LEGAL.
{context}
""".strip()


class GeminiProvider(BaseLLMProvider):
    def __init__(self, api_key: str, model_name: str) -> None:
        if not api_key or not api_key.strip():
            raise GeminiApiKeyMissingError(
                "GEMINI_API_KEY não configurada. Defina a variável de ambiente "
                "antes de executar a extração multimodal."
            )
        self._api_key = api_key.strip()
        self._model_name = model_name
        self._client = genai.Client(api_key=self._api_key)

    @property
    def provider_name(self) -> str:
        return "GEMINI"

    @property
    def model_name(self) -> str:
        return self._model_name

    async def extract_document(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        prompt = EXTRACTION_PROMPT.format(
            context=prompt_context.strip() or "Sem regras few-shot adicionais."
        )
        response = self._client.models.generate_content(
            model=self._model_name,
            contents=[
                types.Part.from_bytes(data=file_bytes, mime_type=mime_type),
                prompt,
            ],
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=DocumentoHabilExtraction,
                temperature=0.0,
            ),
        )
        if not response.text:
            raise RuntimeError("Gemini retornou resposta vazia na extração do documento.")
        return DocumentoHabilExtraction.model_validate_json(response.text)
