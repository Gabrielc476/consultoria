import logging
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from uuid import UUID

from application.pipelines.document_pipeline import DocumentPipeline
from domain.schemas.events import DocumentoRecebidoEvent, DocumentoRecebidoPayload
from infrastructure.config.settings import Settings, get_settings
from infrastructure.llm.provider_factory import create_llm_strategy
from infrastructure.messaging.rabbitmq_consumer import RabbitMQConsumer
from infrastructure.messaging.rabbitmq_publisher import RabbitMQPublisher
from infrastructure.messaging.topology import connect_rabbitmq, declare_topology
from infrastructure.storage.s3_client import S3DocumentClient

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ExtractRequest(BaseModel):
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: DocumentoRecebidoPayload

    model_config = {"populate_by_name": True}


def build_pipeline(settings: Settings) -> DocumentPipeline:
    storage = S3DocumentClient(settings)

    def strategy_factory():
        return create_llm_strategy(settings)

    return DocumentPipeline(
        settings=settings,
        storage=storage,
        llm_strategy_factory=strategy_factory,
    )


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    app.state.settings = settings
    app.state.pipeline = build_pipeline(settings)
    app.state.rabbit_connection = None
    app.state.publisher = None

    try:
        connection = await connect_rabbitmq(settings)
        channel = await connection.channel()
        await declare_topology(channel, settings)
        publisher = RabbitMQPublisher(channel, settings)
        pipeline: DocumentPipeline = app.state.pipeline

        async def handle_recebido(event: DocumentoRecebidoEvent) -> None:
            result = await pipeline.run(event)
            await publisher.publish_documento_extraido(result)

        consumer = RabbitMQConsumer(connection, settings, handle_recebido)
        await consumer.start()
        app.state.rabbit_connection = connection
        app.state.publisher = publisher
        logger.info("AI Service conectado ao RabbitMQ")
    except Exception:
        logger.exception(
            "RabbitMQ indisponível no startup — REST /health e /extract seguem ativos"
        )

    yield

    connection = getattr(app.state, "rabbit_connection", None)
    if connection is not None:
        await connection.close()


app = FastAPI(
    title="GovFlow AI Service",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
async def health() -> dict:
    settings: Settings = app.state.settings
    return {
        "status": "ok",
        "service": "ai-service",
        "primaryModel": settings.gemini_primary_model,
        "fallbackModel": settings.gemini_fallback_model,
        "geminiConfigured": bool(settings.gemini_api_key.strip()),
    }


@app.post("/api/v1/ai/extract")
async def extract_document(request: Dict[str, Any]):
    pipeline: DocumentPipeline = app.state.pipeline
    event = DocumentoRecebidoEvent.parse_from_message(request)
    result = await pipeline.run(event)

    publisher: Optional[RabbitMQPublisher] = getattr(app.state, "publisher", None)
    if publisher is not None and result.payload.processamento.status.startswith("SUCESSO"):
        await publisher.publish_documento_extraido(result)

    if result.payload.processamento.status.startswith("FALHA"):
        erro = result.payload.processamento.erro or {}
        raise HTTPException(
            status_code=422 if erro.get("codigo") == "GEMINI_API_KEY_MISSING" else 502,
            detail=result.model_dump(by_alias=True, mode="json"),
        )

    return result.model_dump(by_alias=True, mode="json")
