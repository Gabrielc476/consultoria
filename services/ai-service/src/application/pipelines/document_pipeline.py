import logging
import time
from decimal import Decimal
from typing import Callable

from domain.rules.financial_rules import average_field_confidence, validate_documento_fiscal
from domain.schemas.events import (
    DocumentoExtraidoEvent,
    DocumentoExtraidoPayload,
    DocumentoRecebidoEvent,
    ProcessamentoMeta,
    ValidacaoMatematicaResult,
)
from application.pipelines.bbox_collector import collect_bounding_boxes
from infrastructure.config.settings import GeminiApiKeyMissingError, Settings
from infrastructure.llm.fallback_provider import LLMProviderStrategy
from infrastructure.storage.s3_client import S3DocumentClient

logger = logging.getLogger(__name__)

StrategyFactory = Callable[[], LLMProviderStrategy]


class DocumentPipeline:
    def __init__(
        self,
        settings: Settings,
        storage: S3DocumentClient,
        llm_strategy_factory: StrategyFactory,
    ) -> None:
        self._settings = settings
        self._storage = storage
        self._llm_strategy_factory = llm_strategy_factory
        self._last_strategy: LLMProviderStrategy | None = None

    async def run(self, event: DocumentoRecebidoEvent) -> DocumentoExtraidoEvent:
        started = time.perf_counter()
        payload = event.payload
        try:
            file_bytes = self._storage.download_bytes(payload.s3_bucket, payload.s3_key)
            strategy = self._llm_strategy_factory()
            self._last_strategy = strategy
            extraction = await strategy.extract_document(
                file_bytes=file_bytes,
                mime_type=payload.content_type,
                prompt_context="",
            )
            validacao = validate_documento_fiscal(extraction)
            confidence = average_field_confidence(extraction)
            status = "SUCESSO_COM_ALERTAS" if not validacao.consistente else "SUCESSO"
            latency_ms = int((time.perf_counter() - started) * 1000)
            return self._build_success_event(
                event=event,
                status=status,
                latency_ms=latency_ms,
                extraction=extraction,
                validacao=validacao,
                confidence=confidence,
                strategy=strategy,
            )
        except GeminiApiKeyMissingError as exc:
            return self._build_failure_event(
                event=event,
                status="FALHA_LLM",
                codigo="GEMINI_API_KEY_MISSING",
                mensagem=str(exc),
                started=started,
            )
        except FileNotFoundError as exc:
            return self._build_failure_event(
                event=event,
                status="FALHA_DOWNLOAD",
                codigo="S3_OBJECT_NOT_FOUND",
                mensagem=str(exc),
                started=started,
            )
        except Exception as exc:
            logger.exception("Falha no pipeline de extração documentoId=%s", payload.documento_id)
            return self._build_failure_event(
                event=event,
                status="FALHA_LLM",
                codigo="EXTRACTION_FAILED",
                mensagem=str(exc),
                started=started,
            )

    def _build_success_event(
        self,
        event: DocumentoRecebidoEvent,
        status: str,
        latency_ms: int,
        extraction,
        validacao: ValidacaoMatematicaResult,
        confidence: float,
        strategy: LLMProviderStrategy,
    ) -> DocumentoExtraidoEvent:
        return DocumentoExtraidoEvent(
            tenantId=event.tenant_id,
            correlationId=event.correlation_id,
            payload=DocumentoExtraidoPayload(
                documentoId=event.payload.documento_id,
                s3Bucket=event.payload.s3_bucket,
                s3Key=event.payload.s3_key,
                processamento=ProcessamentoMeta(
                    status=status,
                    provider=strategy.last_provider_name,
                    modelo=strategy.last_model_name,
                    fallbackUsado=strategy.fallback_used,
                    latenciaMs=latency_ms,
                    paginasProcessadas=1,
                ),
                extracao=extraction.model_dump(mode="json"),
                validacaoMatematica=validacao,
                confidenceScoreGeral=confidence,
                boundingBoxes=collect_bounding_boxes(extraction),
            ),
        )

    def _build_failure_event(
        self,
        event: DocumentoRecebidoEvent,
        status: str,
        codigo: str,
        mensagem: str,
        started: float,
    ) -> DocumentoExtraidoEvent:
        latency_ms = int((time.perf_counter() - started) * 1000)
        strategy = self._last_strategy
        provider = strategy.last_provider_name if strategy is not None else "GEMINI"
        modelo = (
            strategy.last_model_name
            if strategy is not None
            else self._settings.gemini_primary_model
        )
        fallback_usado = strategy.fallback_used if strategy is not None else False
        empty_validation = ValidacaoMatematicaResult(
            valorBruto=None,
            totalRetencoes=Decimal("0.00"),
            valorLiquidoInformado=None,
            valorLiquidoCalculado=None,
            diferenca=None,
            consistente=False,
            tolerancia=Decimal("0.02"),
        )
        return DocumentoExtraidoEvent(
            tenantId=event.tenant_id,
            correlationId=event.correlation_id,
            payload=DocumentoExtraidoPayload(
                documentoId=event.payload.documento_id,
                s3Bucket=event.payload.s3_bucket,
                s3Key=event.payload.s3_key,
                processamento=ProcessamentoMeta(
                    status=status,
                    provider=provider,
                    modelo=modelo,
                    fallbackUsado=fallback_usado,
                    latenciaMs=latency_ms,
                    paginasProcessadas=0,
                    erro={"codigo": codigo, "mensagem": mensagem},
                ),
                extracao={},
                validacaoMatematica=empty_validation,
                confidenceScoreGeral=0.0,
                boundingBoxes={},
            ),
        )
