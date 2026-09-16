from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from domain.rules.financial_rules import average_field_confidence
from infrastructure.llm.base_provider import BaseLLMProvider


class LLMProviderStrategy:
    """Primary provider with automatic fallback on low confidence or transient API errors."""

    def __init__(
        self,
        primary: BaseLLMProvider,
        fallback: BaseLLMProvider,
        confidence_threshold: float = 0.75,
    ) -> None:
        self._primary = primary
        self._fallback = fallback
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
            return await self._run_fallback(file_bytes, mime_type, prompt_context)
        except Exception as exc:
            if self._is_transient_provider_error(exc):
                return await self._run_fallback(file_bytes, mime_type, prompt_context)
            raise

    async def _run_fallback(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        self.fallback_used = True
        self.last_provider_name = self._fallback.provider_name
        self.last_model_name = self._fallback.model_name
        extraction = await self._fallback.extract_document(
            file_bytes,
            mime_type,
            prompt_context,
        )
        return extraction

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
        )
