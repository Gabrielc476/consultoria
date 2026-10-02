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
    convenios_candidatos_ids: List[UUID] = Field(
        default_factory=list,
        alias="conveniosCandidatosIds",
    )
    convenios_candidatos: List[Dict[str, Any]] = Field(
        default_factory=list,
        alias="conveniosCandidatos",
    )
    historico_recente_conversa: List[Dict[str, Any]] = Field(
        default_factory=list,
        alias="historicoRecenteConversa",
    )
    remetente_novo: bool = Field(default=False, alias="remetenteNovo")
    contato_id: Optional[UUID] = Field(default=None, alias="contatoId")
    sender_phone: Optional[str] = Field(default=None, alias="senderPhone")
    sender_name: Optional[str] = Field(default=None, alias="senderName")

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
        sender_phone = data.get("senderPhone")
        sender_name = data.get("senderName")
        contato_id_raw = data.get("contatoId")
        contato_id = UUID(str(contato_id_raw)) if contato_id_raw else None

        candidatos_ids_raw = data.get("conveniosCandidatosIds") or []
        candidatos_ids = [UUID(str(cid)) for cid in candidatos_ids_raw]
        candidatos = data.get("conveniosCandidatos") or []
        historico = data.get("historicoRecenteConversa") or []
        remetente_novo = bool(data.get("remetenteNovo", False))

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
                remetentePhone=sender_phone,
                cnpjPrefeitura=data.get("cnpjPrefeitura"),
            ),
            conveniosCandidatosIds=candidatos_ids,
            conveniosCandidatos=candidatos,
            historicoRecenteConversa=historico,
            remetenteNovo=remetente_novo,
            contatoId=contato_id,
            senderPhone=sender_phone,
            senderName=sender_name,
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
    nome_arquivo_original: Optional[str] = Field(default=None, alias="nomeArquivoOriginal")
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


class DocumentoClassificadoPayload(BaseModel):
    documento_id: UUID = Field(alias="documentoId")
    mensagem_inbound_id: Optional[UUID] = Field(default=None, alias="mensagemInboundId")
    tenant_id: UUID = Field(alias="tenantId")
    prefeitura_id: Optional[UUID] = Field(default=None, alias="prefeituraId")
    convenio_id: Optional[UUID] = Field(default=None, alias="convenioId")
    fase_ciclo_vida: Optional[str] = Field(default="04_EXECUCAO_FISICA_E_MEDICOES", alias="faseCicloVida")
    categoria_documento: Optional[str] = Field(default="DOCUMENTO_HABIL", alias="categoriaDocumento")
    confidence_score: float = Field(default=0.0, alias="confidenceScore")
    motivo_ambiguidade: Optional[str] = Field(default=None, alias="motivoAmbiguidade")
    direcionar_triagem: bool = Field(default=False, alias="direcionarTriagem")
    remetente_phone: Optional[str] = Field(default=None, alias="remetentePhone")
    remetente_name: Optional[str] = Field(default=None, alias="remetenteName")
    remetente_novo: bool = Field(default=False, alias="remetenteNovo")
    conteudo_resumo: Optional[str] = Field(default=None, alias="conteudoResumo")
    s3_bucket: str = Field(default="", alias="s3Bucket")
    s3_key: str = Field(default="", alias="s3Key")
    nome_arquivo_original: Optional[str] = Field(default=None, alias="nomeArquivoOriginal")
    extracao: Dict[str, Any] = Field(default_factory=dict)

    model_config = {"populate_by_name": True}


class DocumentoClassificadoEvent(BaseModel):
    event_id: UUID = Field(default_factory=uuid4, alias="eventId")
    event_type: str = Field(default="DocumentoClassificadoEvent", alias="eventType")
    event_version: str = Field(default="1.0.0", alias="eventVersion")
    occurred_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        alias="occurredAt",
    )
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: DocumentoClassificadoPayload

    model_config = {"populate_by_name": True}


class AudioRecebidoPayload(BaseModel):
    mensagem_inbound_id: UUID = Field(alias="mensagemInboundId")
    s3_bucket: str = Field(alias="s3Bucket")
    s3_key: str = Field(alias="s3Key")
    media_mimetype: str = Field(default="audio/ogg", alias="mediaMimetype")
    sender_phone: Optional[str] = Field(default=None, alias="senderPhone")
    sender_name: Optional[str] = Field(default=None, alias="senderName")

    model_config = {"populate_by_name": True}


class AudioRecebidoEvent(BaseModel):
    event_id: UUID = Field(default_factory=uuid4, alias="eventId")
    event_type: str = Field(default="AudioRecebidoEvent", alias="eventType")
    tenant_id: UUID = Field(alias="tenantId")
    correlation_id: UUID = Field(alias="correlationId")
    payload: AudioRecebidoPayload

    model_config = {"populate_by_name": True}
