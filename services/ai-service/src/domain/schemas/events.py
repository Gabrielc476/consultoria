from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from uuid import UUID, uuid4

from pydantic import BaseModel, Field


class DocumentoOrigem(BaseModel):
    canal: str = "WHATSAPP"
    provedor: Optional[str] = None
    external_message_id: Optional[str] = Field(default=None, alias="externalMessageId")
    remetente_phone: Optional[str] = Field(default=None, alias="remetentePhone")
    prefeitura_id: Optional[UUID] = Field(default=None, alias="prefeituraId")
    cnpj_prefeitura: str = Field(alias="cnpjPrefeitura")

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
    content_type: str = Field(alias="contentType")
    tamanho_bytes: Optional[int] = Field(default=None, alias="tamanhoBytes")
    nome_arquivo_original: Optional[str] = Field(default=None, alias="nomeArquivoOriginal")
    origem: DocumentoOrigem
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


class ValidacaoMatematicaResult(BaseModel):
    valor_bruto: Optional[float] = Field(default=None, alias="valorBruto")
    total_retencoes: float = Field(alias="totalRetencoes")
    valor_liquido_informado: Optional[float] = Field(
        default=None,
        alias="valorLiquidoInformado",
    )
    valor_liquido_calculado: Optional[float] = Field(
        default=None,
        alias="valorLiquidoCalculado",
    )
    diferenca: Optional[float] = None
    consistente: bool
    tolerancia: float = 0.0

    model_config = {"populate_by_name": True}


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
