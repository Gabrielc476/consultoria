import asyncio
from functools import wraps
from uuid import uuid4

import pytest

from application.pipelines.document_pipeline import DocumentPipeline
from domain.schemas.bounding_box_schema import BoundingBox
from domain.schemas.confidence_report import ExtractedField
from domain.schemas.documento_habil_schema import DocumentoHabilExtraction, RetencaoTributaria
from domain.schemas.events import DocumentoOrigem, DocumentoRecebidoEvent, DocumentoRecebidoPayload
from infrastructure.config.settings import GeminiApiKeyMissingError, Settings
from infrastructure.llm.fallback_provider import LLMProviderStrategy
from infrastructure.llm.base_provider import BaseLLMProvider


def async_test(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        return asyncio.run(f(*args, **kwargs))

    return wrapper


class FakeStorage:
    def __init__(self, payload: bytes = b"%PDF-1.4 fake") -> None:
        self.payload = payload

    def download_bytes(self, bucket: str, key: str) -> bytes:
        if key == "missing.pdf":
            raise FileNotFoundError(f"s3://{bucket}/{key}")
        return self.payload


class FakeLLM(BaseLLMProvider):
    def __init__(
        self,
        extraction: DocumentoHabilExtraction | None = None,
        name: str = "GEMINI",
        model: str = "fake",
        error: Exception | None = None,
    ) -> None:
        self._extraction = extraction
        self._name = name
        self._model = model
        self._error = error

    @property
    def provider_name(self) -> str:
        return self._name

    @property
    def model_name(self) -> str:
        return self._model

    async def extract_document(self, file_bytes, mime_type, prompt_context):
        if self._error is not None:
            raise self._error
        assert self._extraction is not None
        return self._extraction


def _field(valor: str, conf: float = 0.95) -> ExtractedField:
    return ExtractedField(
        valor=valor,
        confianca=conf,
        coordenadas=BoundingBox(ymin=1, xmin=2, ymax=3, xmax=4, page_number=1),
    )


def _good_extraction() -> DocumentoHabilExtraction:
    return DocumentoHabilExtraction(
        tipo_documento=_field("NOTA_FISCAL_SERVICOS"),
        numero_documento=_field("0001542"),
        data_emissao=_field("2026-08-20"),
        valor_bruto=_field("100.00"),
        valor_liquido=_field("90.00"),
        cnpj_credor=_field("08123456000190"),
        razao_social_credor=_field("EMPRESA X"),
        descricao_servico=_field("Serviço"),
        retencoes=[
            RetencaoTributaria(tipo="ISS", aliquota=10.0, valor=10.0, confianca=0.9),
        ],
    )


def _event(key: str = "docs/nf.pdf") -> DocumentoRecebidoEvent:
    return DocumentoRecebidoEvent(
        tenantId=uuid4(),
        correlationId=uuid4(),
        payload=DocumentoRecebidoPayload(
            documentoId=uuid4(),
            s3Bucket="govflow-documents",
            s3Key=key,
            contentType="application/pdf",
            origem=DocumentoOrigem(cnpjPrefeitura="08765432000199"),
        ),
    )


@async_test
async def test_pipeline_sucesso_publicavel():
    settings = Settings(GEMINI_API_KEY="test-key")
    strategy = LLMProviderStrategy(
        primary=FakeLLM(_good_extraction()),
        fallback=FakeLLM(_good_extraction(), model="fallback"),
        confidence_threshold=0.75,
    )
    pipeline = DocumentPipeline(
        settings=settings,
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event())
    assert result.event_type == "DocumentoExtraidoEvent"
    assert result.payload.processamento.status == "SUCESSO"
    assert result.payload.validacao_matematica.consistente is True
    assert "valor_bruto" in result.payload.bounding_boxes
    assert result.payload.extracao["numero_documento"]["valor"] == "0001542"


@async_test
async def test_pipeline_alerta_matematico():
    bad = _good_extraction()
    bad.valor_liquido.valor = "50.00"
    strategy = LLMProviderStrategy(
        primary=FakeLLM(bad),
        fallback=FakeLLM(bad),
        confidence_threshold=0.99,
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event())
    assert result.payload.processamento.status == "SUCESSO_COM_ALERTAS"
    assert result.payload.validacao_matematica.consistente is False
    assert any(
        "INCONSISTENCIA_MATEMATICA" in a
        for a in result.payload.extracao.get("alertas_inconsistencia", [])
    )


@async_test
async def test_pipeline_sem_gemini_key():
    def boom():
        raise GeminiApiKeyMissingError("GEMINI_API_KEY não configurada")

    pipeline = DocumentPipeline(
        settings=Settings(),
        storage=FakeStorage(),
        llm_strategy_factory=boom,
    )
    result = await pipeline.run(_event())
    assert result.payload.processamento.status == "FALHA_LLM"
    assert result.payload.processamento.erro["codigo"] == "GEMINI_API_KEY_MISSING"


@async_test
async def test_pipeline_download_falha():
    strategy = LLMProviderStrategy(
        primary=FakeLLM(_good_extraction()),
        fallback=FakeLLM(_good_extraction()),
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event(key="missing.pdf"))
    assert result.payload.processamento.status == "FALHA_DOWNLOAD"


@async_test
async def test_strategy_fallback_on_gemini_503_unavailable():
    """Remainder from test_results.json notes: fallback must cover HTTP 503."""
    primary_error = RuntimeError(
        "503 UNAVAILABLE. {'error': {'code': 503, 'message': "
        "'This model is currently experiencing high demand.', 'status': 'UNAVAILABLE'}}"
    )
    strategy = LLMProviderStrategy(
        primary=FakeLLM(error=primary_error, name="GEMINI", model="gemini-3.7-flash"),
        fallback=FakeLLM(_good_extraction(), name="GEMMA", model="gemma-4-31b-it"),
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event())
    assert result.payload.processamento.status == "SUCESSO"
    assert result.payload.processamento.fallback_usado is True
    assert result.payload.processamento.modelo == "gemma-4-31b-it"


@async_test
async def test_pipeline_reports_fallback_used_when_both_models_fail_with_503():
    err = RuntimeError(
        "503 UNAVAILABLE. {'error': {'code': 503, 'status': 'UNAVAILABLE'}}"
    )
    strategy = LLMProviderStrategy(
        primary=FakeLLM(error=err, name="GEMINI", model="gemini-3.7-flash"),
        fallback=FakeLLM(error=err, name="GEMMA", model="gemma-4-31b-it"),
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event())
    assert result.payload.processamento.status == "FALHA_LLM"
    assert result.payload.processamento.fallback_usado is True
    assert result.payload.processamento.modelo == "gemma-4-31b-it"


@async_test
async def test_strategy_cascades_to_tertiary_when_primary_and_fallback_fail():
    err = RuntimeError(
        "503 UNAVAILABLE. {'error': {'code': 503, 'status': 'UNAVAILABLE'}}"
    )
    strategy = LLMProviderStrategy(
        primary=FakeLLM(error=err, name="GEMMA", model="gemma-4-31b-it"),
        fallback=FakeLLM(error=err, name="GEMINI", model="gemini-3.7-flash"),
        tertiary=FakeLLM(_good_extraction(), name="GEMINI", model="gemini-3.1-flash-lite"),
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(_event())
    assert result.payload.processamento.status == "SUCESSO"
    assert result.payload.processamento.fallback_usado is True
    assert result.payload.processamento.modelo == "gemini-3.1-flash-lite"


@async_test
async def test_pipeline_consumes_real_whatsapp_service_flat_event():
    raw_whatsapp_event = {
        "tenantId": "c0a80101-0000-0000-0000-000000000001",
        "prefeituraId": "c0a80101-0000-0000-0000-000000000002",
        "mensagemInboundId": "c0a80101-0000-0000-0000-000000000003",
        "s3Bucket": "govflow-documents",
        "s3Key": "raw/whatsapp/unidentified/2026/09/nota.pdf",
        "mediaMimetype": "application/pdf",
        "fileName": "nota_fiscal_01.pdf",
        "fileSizeBytes": 80874,
        "senderPhone": "558387514931",
        "senderName": "Secretário de Obras",
        "timestamp": "2026-09-16T15:25:37Z",
    }
    event = DocumentoRecebidoEvent.parse_from_message(raw_whatsapp_event)
    assert str(event.tenant_id) == "c0a80101-0000-0000-0000-000000000001"
    assert str(event.correlation_id) == "c0a80101-0000-0000-0000-000000000003"
    assert str(event.payload.documento_id) == "c0a80101-0000-0000-0000-000000000003"
    assert event.payload.content_type == "application/pdf"
    assert event.payload.origem.cnpj_prefeitura is None

    strategy = LLMProviderStrategy(
        primary=FakeLLM(_good_extraction(), name="GEMINI", model="gemini-3.7-flash"),
        fallback=FakeLLM(_good_extraction(), name="GEMMA", model="gemma-4-31b-it"),
    )
    pipeline = DocumentPipeline(
        settings=Settings(GEMINI_API_KEY="test-key"),
        storage=FakeStorage(),
        llm_strategy_factory=lambda: strategy,
    )
    result = await pipeline.run(event)
    assert result.payload.processamento.status == "SUCESSO"
    assert result.payload.processamento.modelo == "gemini-3.7-flash"
    assert result.payload.processamento.fallback_usado is False


def test_gemma_json_cleaner_resilient_to_conversational_text():
    from infrastructure.llm.gemini_provider import GeminiProvider

    noisy_output = """
    Olá! Aqui está a extração fiscal da nota solicitada:
    ```json
    {
      "tipo_documento": {"valor": "NOTA_FISCAL_SERVICOS", "confianca": 0.99, "coordenadas": null},
      "numero_documento": {"valor": "0001542", "confianca": 0.99, "coordenadas": null},
      "data_emissao": {"valor": "2026-08-20", "confianca": 0.99, "coordenadas": null},
      "valor_bruto": {"valor": "1000.00", "confianca": 0.99, "coordenadas": null},
      "valor_liquido": {"valor": "840.00", "confianca": 0.99, "coordenadas": null},
      "cnpj_credor": {"valor": "08123456000190", "confianca": 0.99, "coordenadas": null},
      "razao_social_credor": {"valor": "CONSTRUTORA EXEMPLO LTDA", "confianca": 0.99, "coordenadas": null},
      "numero_empenho": null,
      "descricao_servico": {"valor": "Serviços de medição", "confianca": 0.99, "coordenadas": null},
      "chave_acesso_nfe": null,
      "retencoes": [
        {
          "tipo": "INSS",
          "aliquota": "11.0%",
          "valor": "110,00",
          "confianca": 0.99,
          "coordenadas": null
        },
        {
          "tipo": "ISS",
          "aliquota": 5.0,
          "valor": 50.00,
          "confianca": 0.99,
          "coordenadas": null
        }
      ],
      "alertas_inconsistencia": []
    }
    ```
    Espero ter ajudado! Se precisar de algo mais, estou à disposição.
    """

    cleaned = GeminiProvider._clean_json_text(noisy_output)
    extraction = DocumentoHabilExtraction.model_validate_json(cleaned)

    assert extraction.tipo_documento.valor == "NOTA_FISCAL_SERVICOS"
    assert extraction.numero_documento.valor == "0001542"
    assert len(extraction.retencoes) == 2
    assert extraction.retencoes[0].aliquota == 11.0
    assert extraction.retencoes[0].valor == 110.00
    assert extraction.retencoes[1].aliquota == 5.0


def test_schema_coercion_handles_null_retencoes_and_optional_fields():
    raw_json = """
    {
      "tipo_documento": {"valor": "NOTA_FISCAL_SERVICOS", "confianca": 0.99, "coordenadas": null},
      "numero_documento": {"valor": "123", "confianca": 0.99, "coordenadas": null},
      "data_emissao": {"valor": "2026-09-01", "confianca": 0.99, "coordenadas": null},
      "valor_bruto": {"valor": "500.00", "confianca": 0.99, "coordenadas": null},
      "valor_liquido": {"valor": "500.00", "confianca": 0.99, "coordenadas": null},
      "cnpj_credor": {"valor": "12345678000199", "confianca": 0.99, "coordenadas": null},
      "razao_social_credor": {"valor": "Firma ABC", "confianca": 0.99, "coordenadas": null},
      "numero_empenho": null,
      "descricao_servico": {"valor": "Consultoria", "confianca": 0.99, "coordenadas": null},
      "chave_acesso_nfe": null,
      "retencoes": null,
      "alertas_inconsistencia": []
    }
    """
    extraction = DocumentoHabilExtraction.model_validate_json(raw_json)
    assert extraction.retencoes == []
    assert extraction.numero_empenho is None
    assert extraction.valor_bruto.valor == "500.00"

