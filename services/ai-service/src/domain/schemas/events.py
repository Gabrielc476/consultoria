from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

from pydantic import BaseModel, Field, field_serializer


class DocumentoOrigem(BaseModel):
    canal: str = "WHATSAPP"
    provedor: Optional[str] = None
    external_message_id: Optional[str] = Field(default=None, alias="externalMessageId")
    remetente_phone: Optional[str] = Field(default=None, alias="remetentePhone")
    prefeitura_id: Optional[UUID] = Field(default=None, alias="prefeituraId")
    cnpj_prefeitura: Optional[str] = Field(default=None, alias="cnpjPrefeitura")

    model_config = {"populate_by_name": True}


class DocumentoVinculos(BaseModel):
    convenio_id: Optional[UUID] = Field(default=None, alias="convenioId")
    medicao_id: Optional[UUID] = Field(default=None, alias="medicaoId")
    contrato_id: Optional[UUID] = Field(default=None, alias="contratoId")

    model_config = {"populate_by_name": True}


class DocumentoRecebidoPayload(BaseModel):
    documento_id: UUID = Field(alias="documentoId")
    s3_bucket: str = Field(alias="s3Bucket")
    s3_key: str = Field(alias="s3Key")
    content_type: str = Field(default="application/pdf", alias="contentType")
    tamanho_bytes: Optional[int] = Field(default=None, alias="tamanhoBytes")
    nome_arquivo_original: Optional[str] = Field(default=None, alias="nomeArquivoOriginal")
    origem: DocumentoOrigem = Field(default_factory=DocumentoOrigem)
    vinculos_opcionais: Optional[DocumentoVinculos] = Field(
        default=None,
        alias="vinculosOpcionais",
    )

    model_config = {"populate_by_name": True}


class EventEnvelope(BaseModel):
    event_id: UUID = Field(default_factory=uuid4, alias="eventId")
    event_type: str = Field(alias="eventType")
    event_version: str = Field(default="1.0.0", alias="eventVersion")
    occurred_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        alias="occurredAt",
    )
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: Dict[str, Any]

    model_config = {"populate_by_name": True}


class DocumentoRecebidoEvent(BaseModel):
    event_id: UUID = Field(default_factory=uuid4, alias="eventId")
    event_type: str = Field(default="DocumentoRecebidoEvent", alias="eventType")
    event_version: str = Field(default="1.0.0", alias="eventVersion")
    occurred_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        alias="occurredAt",
    )
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: DocumentoRecebidoPayload

    model_config = {"populate_by_name": True}

    @classmethod
    def parse_from_message(
        cls,
        data: Dict[str, Any],
        headers: Optional[Dict[str, Any]] = None,
    ) -> "DocumentoRecebidoEvent":
        """Parses both nested envelope format and flat payload emitted by whatsapp-service."""
        hdr = headers or {}
        tenant_raw = (
            data.get("tenantId")
            or hdr.get("X-Tenant-Id")
            or hdr.get("x-tenant-id")
            or "00000000-0000-0000-0000-000000000000"
        )
        tenant_id = UUID(str(tenant_raw))

        corr_raw = (
            data.get("correlationId")
            or hdr.get("X-Correlation-Id")
            or hdr.get("x-correlation-id")
            or data.get("mensagemInboundId")
            or uuid4()
        )
        correlation_id = UUID(str(corr_raw))

        event_id_raw = data.get("eventId") or uuid4()
        event_id = UUID(str(event_id_raw))

        if "payload" in data and isinstance(data["payload"], dict):
            # Nested envelope format
            payload_dict = dict(data["payload"])
            if "contentType" not in payload_dict and "mediaMimetype" in payload_dict:
                payload_dict["contentType"] = payload_dict["mediaMimetype"]
            if "documentoId" not in payload_dict and "mensagemInboundId" in payload_dict:
                payload_dict["documentoId"] = payload_dict["mensagemInboundId"]
            return cls(
                eventId=event_id,
                tenantId=tenant_id,
                correlationId=correlation_id,
                payload=DocumentoRecebidoPayload.model_validate(payload_dict),
            )

        # Flat payload from whatsapp-service
        doc_id_raw = data.get("documentoId") or data.get("mensagemInboundId") or uuid4()
        doc_id = UUID(str(doc_id_raw))
        pref_id_raw = data.get("prefeituraId")
        pref_id = UUID(str(pref_id_raw)) if pref_id_raw else None

        payload_obj = DocumentoRecebidoPayload(
            documentoId=doc_id,
            s3Bucket=data.get("s3Bucket", ""),
            s3Key=data.get("s3Key", ""),
            contentType=data.get("mediaMimetype") or data.get("contentType") or "application/pdf",
            tamanhoBytes=data.get("fileSizeBytes") or data.get("tamanhoBytes"),
            nomeArquivoOriginal=data.get("fileName") or data.get("nomeArquivoOriginal"),
            origem=DocumentoOrigem(
                canal="WHATSAPP",
                prefeituraId=pref_id,
                remetentePhone=data.get("senderPhone"),
                cnpjPrefeitura=data.get("cnpjPrefeitura"),
            ),
        )

        return cls(
            eventId=event_id,
            tenantId=tenant_id,
            correlationId=correlation_id,
            payload=payload_obj,
        )


class ValidacaoMatematicaResult(BaseModel):
    valor_bruto: Optional[Decimal] = Field(default=None, alias="valorBruto")
    total_retencoes: Decimal = Field(alias="totalRetencoes")
    valor_liquido_informado: Optional[Decimal] = Field(
        default=None,
        alias="valorLiquidoInformado",
    )
    valor_liquido_calculado: Optional[Decimal] = Field(
        default=None,
        alias="valorLiquidoCalculado",
    )
    diferenca: Optional[Decimal] = None
    consistente: bool
    tolerancia: Decimal = Decimal("0.02")

    model_config = {"populate_by_name": True}

    @field_serializer(
        "valor_bruto",
        "total_retencoes",
        "valor_liquido_informado",
        "valor_liquido_calculado",
        "diferenca",
        "tolerancia",
        when_used="json",
    )
    def serialize_decimal(self, value: Optional[Decimal]) -> Optional[float]:
        return float(round(value, 2)) if value is not None else None


class ProcessamentoMeta(BaseModel):
    status: str
    provider: str
    modelo: str
    fallback_usado: bool = Field(alias="fallbackUsado")
    latencia_ms: int = Field(alias="latenciaMs")
    paginas_processadas: int = Field(default=1, alias="paginasProcessadas")
    erro: Optional[Dict[str, str]] = None

    model_config = {"populate_by_name": True}


class DocumentoExtraidoPayload(BaseModel):
    documento_id: UUID = Field(alias="documentoId")
    s3_bucket: str = Field(alias="s3Bucket")
    s3_key: str = Field(alias="s3Key")
    processamento: ProcessamentoMeta
    extracao: Dict[str, Any]
    validacao_matematica: ValidacaoMatematicaResult = Field(alias="validacaoMatematica")
    confidence_score_geral: float = Field(alias="confidenceScoreGeral")
    bounding_boxes: Dict[str, Any] = Field(default_factory=dict, alias="boundingBoxes")

    model_config = {"populate_by_name": True}


class DocumentoExtraidoEvent(BaseModel):
    event_id: UUID = Field(default_factory=uuid4, alias="eventId")
    event_type: str = Field(default="DocumentoExtraidoEvent", alias="eventType")
    event_version: str = Field(default="1.0.0", alias="eventVersion")
    occurred_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        alias="occurredAt",
    )
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: DocumentoExtraidoPayload

    model_config = {"populate_by_name": True}
