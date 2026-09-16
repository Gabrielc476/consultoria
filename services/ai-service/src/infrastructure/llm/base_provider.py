from abc import ABC, abstractmethod

from domain.schemas.documento_habil_schema import DocumentoHabilExtraction


class BaseLLMProvider(ABC):
    @abstractmethod
    async def extract_document(
        self,
        file_bytes: bytes,
        mime_type: str,
        prompt_context: str,
    ) -> DocumentoHabilExtraction:
        raise NotImplementedError

    @property
    @abstractmethod
    def provider_name(self) -> str:
        raise NotImplementedError

    @property
    @abstractmethod
    def model_name(self) -> str:
        raise NotImplementedError
