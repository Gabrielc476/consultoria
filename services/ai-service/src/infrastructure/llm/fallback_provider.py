import logging
from typing import Optional

from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from domain.rules.financial_rules import average_field_confidence
from infrastructure.llm.base_provider import BaseLLMProvider

logger = logging.getLogger(__name__)


class LLMProviderStrategy:
    """Primary provider with cascading automatic fallback (secondary and tertiary) on low confidence or API errors."""

    def __init__(
        self,
        primary: BaseLLMProvider,
        fallback: BaseLLMProvider,
        confidence_threshold: float = 0.75,
        tertiary: Optional[BaseLLMProvider] = None,
    ) -> None:
        self._primary = primary
        self._fallback = fallback
        self._tertiary = tertiary
        self._confidence_threshold = confidence_threshold
        self.last_provider_name = primary.provider_name
        self.last_model_name = primary.model_name
        self.fallback_used = False

    async def extract_document(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        self.fallback_used = False
        try:
            extraction = await self._primary.extract_document(
                file_bytes,
                mime_type,
                prompt_context,
            )
            self.last_provider_name = self._primary.provider_name
            self.last_model_name = self._primary.model_name
            if average_field_confidence(extraction) >= self._confidence_threshold:
                return extraction
            logger.info(
                "Provedor primario (%s) com baixa confianca (%.2f < %.2f). Acionando fallback...",
                self._primary.model_name,
                average_field_confidence(extraction),
                self._confidence_threshold,
            )
            return await self._run_fallback(file_bytes, mime_type, prompt_context)
        except Exception as exc:
            logger.warning(
                "Falha no provedor primario (%s: %s). Acionando fallback para %s...",
                self._primary.model_name,
                exc,
                self._fallback.model_name,
            )
            return await self._run_fallback(file_bytes, mime_type, prompt_context)

    async def _run_fallback(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        self.fallback_used = True
        self.last_provider_name = self._fallback.provider_name
        self.last_model_name = self._fallback.model_name
        try:
            extraction = await self._fallback.extract_document(
                file_bytes,
                mime_type,
                prompt_context,
            )
            if (
                average_field_confidence(extraction) >= self._confidence_threshold
                or self._tertiary is None
            ):
                return extraction
            logger.info(
                "Fallback secundario (%s) com baixa confianca (%.2f < %.2f). Acionando fallback terciario...",
                self._fallback.model_name,
                average_field_confidence(extraction),
                self._confidence_threshold,
            )
            return await self._run_tertiary(file_bytes, mime_type, prompt_context)
        except Exception as exc:
            if self._tertiary is not None:
                logger.warning(
                    "Falha no fallback secundario (%s: %s). Acionando fallback terciario para %s...",
                    self._fallback.model_name,
                    exc,
                    self._tertiary.model_name,
                )
                return await self._run_tertiary(file_bytes, mime_type, prompt_context)
            raise

    async def _run_tertiary(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        assert self._tertiary is not None
        self.fallback_used = True
        self.last_provider_name = self._tertiary.provider_name
        self.last_model_name = self._tertiary.model_name
        return await self._tertiary.extract_document(
            file_bytes,
            mime_type,
            prompt_context,
        )

    @staticmethod
    def _is_transient_provider_error(exc: Exception) -> bool:
        message = str(exc).lower()
        return (
            "429" in message
            or "rate limit" in message
            or "resource_exhausted" in message
            or "503" in message
            or "unavailable" in message
            or "high demand" in message
            or "500" in message
            or "internal" in message
        )
