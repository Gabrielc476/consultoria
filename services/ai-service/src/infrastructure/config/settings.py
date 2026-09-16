from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    gemini_api_key: str = Field(default="", alias="GEMINI_API_KEY")
    gemini_primary_model: str = Field(
        default="gemini-3.7-flash",
        alias="GEMINI_PRIMARY_MODEL",
    )
    gemini_fallback_model: str = Field(
        default="gemma-4-31b-it",
        alias="GEMINI_FALLBACK_MODEL",
    )
    llm_confidence_threshold: float = Field(
        default=0.75,
        alias="LLM_CONFIDENCE_THRESHOLD",
    )

    minio_endpoint: str = Field(default="http://minio:9000", alias="MINIO_ENDPOINT")
    minio_access_key: str = Field(default="minioadmin", alias="MINIO_ACCESS_KEY")
    minio_secret_key: str = Field(default="minioadmin123", alias="MINIO_SECRET_KEY")
    minio_documents_bucket: str = Field(
        default="govflow-documents",
        alias="MINIO_DOCUMENTS_BUCKET",
    )
    minio_region: str = Field(default="us-east-1", alias="MINIO_REGION")

    rabbitmq_host: str = Field(default="rabbitmq", alias="RABBITMQ_HOST")
    rabbitmq_port: int = Field(default=5672, alias="RABBITMQ_PORT")
    rabbitmq_user: str = Field(default="govflow", alias="RABBITMQ_USER")
    rabbitmq_password: str = Field(default="govflow123", alias="RABBITMQ_PASSWORD")
    rabbitmq_vhost: str = Field(default="/", alias="RABBITMQ_VHOST")

    exchange_documentos: str = "govflow.events"
    exchange_documentos_dlx: str = "govflow.dlx"
    queue_extrair: str = "fila.documentos.extrair"
    queue_processados: str = "fila.documentos.processados"
    queue_dlq: str = "fila.documentos.extrair.dlq"
    routing_key_recebido: str = "whatsapp.documento.recebido"
    routing_key_extraido: str = "documento.extraido"

    ai_service_port: int = Field(default=8000, alias="AI_SERVICE_PORT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    @property
    def rabbitmq_url(self) -> str:
        return (
            f"amqp://{self.rabbitmq_user}:{self.rabbitmq_password}"
            f"@{self.rabbitmq_host}:{self.rabbitmq_port}{self.rabbitmq_vhost}"
        )

    def require_gemini_api_key(self) -> str:
        key = self.gemini_api_key.strip()
        if not key:
            raise GeminiApiKeyMissingError(
                "GEMINI_API_KEY não configurada. Defina a variável de ambiente "
                "antes de executar a extração multimodal."
            )
        return key


class GeminiApiKeyMissingError(RuntimeError):
    """Raised when Gemini extraction is attempted without an API key."""


@lru_cache
def get_settings() -> Settings:
    return Settings()
