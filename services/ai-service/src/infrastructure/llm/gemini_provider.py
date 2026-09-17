import asyncio
import io
import logging
import re
from typing import List

from google import genai
from google.genai import types
from PIL import Image

from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from infrastructure.config.settings import GeminiApiKeyMissingError
from infrastructure.llm.base_provider import BaseLLMProvider

logger = logging.getLogger(__name__)

GEMINI_EXTRACTION_PROMPT = """
Extraia os campos fiscais oficiais do Documento Hábil / Nota Fiscal brasileira.
Retorne JSON tipado conforme o schema.
Use coordenadas de bounding box normalizadas [ymin, xmin, ymax, xmax] na escala 0-1000.
Inclua page_number (1-based) em cada bounding box.
CNPJ apenas com dígitos. Datas em YYYY-MM-DD. Valores monetários com ponto decimal (ex: 85400.00).
tipo_documento deve ser um de: NOTA_FISCAL_SERVICOS, NOTA_FISCAL_MERCADORIAS, RECIBO_LEGAL.
{context}
""".strip()

GEMMA_EXTRACTION_PROMPT = """
Voce e um especialista em OCR e extracao fiscal de Documentos Habeis / Notas Fiscais brasileiras.
Analise a imagem da nota fiscal e extraia os dados estritamente conforme as regras abaixo.

DIRETRIZ ESTRITA DE SAIDA:
- Retorne UNICAMENTE um objeto JSON valido.
- NUNCA inclua saudacoes, explicacoes, comentarios ou texto introdutorio antes ou depois do JSON.

FORMATO ESTRITO DO JSON:
{
  "tipo_documento": {"valor": "NOTA_FISCAL_SERVICOS", "confianca": 0.99, "coordenadas": null},
  "numero_documento": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "data_emissao": {"valor": "YYYY-MM-DD", "confianca": 0.99, "coordenadas": null},
  "valor_bruto": {"valor": "0.00", "confianca": 0.99, "coordenadas": null},
  "valor_liquido": {"valor": "0.00", "confianca": 0.99, "coordenadas": null},
  "cnpj_credor": {"valor": "apenas digitos sem pontos", "confianca": 0.99, "coordenadas": null},
  "razao_social_credor": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "numero_empenho": null,
  "descricao_servico": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "chave_acesso_nfe": null,
  "retencoes": [
    {
      "tipo": "INSS",
      "aliquota": 11.0,
      "valor": 110.00,
      "confianca": 0.99,
      "coordenadas": null
    }
  ],
  "alertas_inconsistencia": []
}

REGRAS OBRIGATORIAS CAMPO A CAMPO:
1. tipo_documento: Obrigatorio. Deve ser exatamente um de: "NOTA_FISCAL_SERVICOS", "NOTA_FISCAL_MERCADORIAS", ou "RECIBO_LEGAL".
2. data_emissao: Formato estrito YYYY-MM-DD (ex: "2026-08-20").
3. valor_bruto e valor_liquido: String numerica com ponto decimal (ex: "1500.00"). NUNCA use virgula e NUNCA inclua "R$".
4. cnpj_credor: Apenas digitos numericos, sem pontuacao (ex: "08123456000190").
5. retencoes:
   - Se NAO houver retencoes no documento, retorne lista vazia []. NUNCA retorne null para "retencoes".
   - tipo: Exatamente um de: "INSS", "ISS", "IRRF", "PIS", "COFINS", "CSLL".
   - aliquota: Numero float decimal puro (ex: 11.0, 5.0). NUNCA inclua o simbolo "%".
   - valor: Numero float decimal puro com ponto (ex: 110.00). NUNCA inclua "R$" nem virgula.
   - confianca: Numero float de 0.0 a 1.0 (ex: 0.99).
6. numero_empenho e chave_acesso_nfe:
   - Se presente no documento, informe {"valor": "...", "confianca": 0.99, "coordenadas": null}. Se ausente, preencha null.
7. descricao_servico: Texto conciso do servico. Nao use aspas duplas nao escapadas no texto.
8. coordenadas: Preencha null se nao houver coordenadas precisas.

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
        return "GEMMA" if "gemma" in self._model_name.lower() else "GEMINI"

    @property
    def model_name(self) -> str:
        return self._model_name

    def _prepare_parts(self, file_bytes: bytes, mime_type: str) -> List[types.Part]:
        if mime_type.lower() == "application/pdf":
            try:
                return self._rasterize_pdf_to_images(file_bytes)
            except Exception as exc:
                logger.warning("Falha na rasterizacao do PDF, enviando raw bytes: %s", exc)
        return [types.Part.from_bytes(data=file_bytes, mime_type=mime_type)]

    @staticmethod
    def _rasterize_pdf_to_images(file_bytes: bytes) -> List[types.Part]:
        import pypdfium2 as pdfium

        pdf = pdfium.PdfDocument(file_bytes)
        parts: List[types.Part] = []
        for page in pdf:
            # scale=2.5 com clamp para manter a imagem em ~1600px max, ideal para OCR sem estourar limites de memória do Gemma
            pil_image = page.render(scale=2.5).to_pil().convert("RGB")
            if max(pil_image.size) > 1600:
                pil_image.thumbnail((1600, 1600), Image.Resampling.LANCZOS)
            buf = io.BytesIO()
            pil_image.save(buf, format="JPEG", quality=92)
            parts.append(
                types.Part.from_bytes(data=buf.getvalue(), mime_type="image/jpeg")
            )
        return parts

    @staticmethod
    def _clean_json_text(text: str) -> str:
        s = text.strip()
        # Se contiver bloco delimitado ```json ... ``` ou ``` ... ```
        if "```" in s:
            match_block = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", s, re.DOTALL)
            if match_block:
                return match_block.group(1).strip()

        # Procura o primeiro { e o último } para isolar o JSON puro ignorando conversa anterior ou posterior
        match_obj = re.search(r"\{.*\}", s, re.DOTALL)
        if match_obj:
            return match_obj.group(0).strip()

        if s.startswith("```json"):
            s = s[7:]
        elif s.startswith("```"):
            s = s[3:]
        if s.endswith("```"):
            s = s[:-3]
        return s.strip()

    async def extract_document(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        is_gemma = self.provider_name == "GEMMA"
        parts = self._prepare_parts(file_bytes, mime_type)

        if is_gemma:
            prompt = GEMMA_EXTRACTION_PROMPT.replace(
                "{context}",
                prompt_context.strip() or "Sem regras few-shot adicionais.",
            )
            config = types.GenerateContentConfig(
                response_mime_type="application/json",
                temperature=0.0,
            )
        else:
            prompt = GEMINI_EXTRACTION_PROMPT.replace(
                "{context}",
                prompt_context.strip() or "Sem regras few-shot adicionais.",
            )
            config = types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=DocumentoHabilExtraction,
                temperature=0.0,
            )

        max_retries = 3
        base_delay = 3.0
        response = None
        for attempt in range(max_retries + 1):
            try:
                response = await self._client.aio.models.generate_content(
                    model=self._model_name,
                    contents=[*parts, prompt],
                    config=config,
                )
                break
            except Exception as exc:
                msg = str(exc).lower()
                is_transient = (
                    "503" in msg
                    or "unavailable" in msg
                    or "429" in msg
                    or "resource_exhausted" in msg
                    or "high demand" in msg
                    or "overloaded" in msg
                    or "500" in msg
                    or "internal" in msg
                )
                if is_transient and attempt < max_retries:
                    delay = base_delay * (2 ** attempt)
                    logger.warning(
                        "[%s - %s] Tentativa %d/%d falhou com erro transiente: %s. Aguardando %.1fs...",
                        self.provider_name,
                        self._model_name,
                        attempt + 1,
                        max_retries,
                        exc,
                        delay,
                    )
                    await asyncio.sleep(delay)
                else:
                    raise

        if not response or not response.text:
            raise RuntimeError(
                f"{self.provider_name} ({self._model_name}) retornou resposta vazia na extração do documento."
            )
        logger.info(
            "[%s - %s] Resposta bruta do modelo:\n%s",
            self.provider_name,
            self._model_name,
            response.text,
        )
        cleaned_json = self._clean_json_text(response.text)
        try:
            return DocumentoHabilExtraction.model_validate_json(cleaned_json)
        except Exception as exc:
            logger.error(
                "[%s - %s] Falha na validacao Pydantic do JSON retornado: %s.\nJSON que falhou:\n%s",
                self.provider_name,
                self._model_name,
                exc,
                cleaned_json,
            )
            raise

