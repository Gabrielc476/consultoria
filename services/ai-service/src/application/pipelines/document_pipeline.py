import logging
import time
from decimal import Decimal
from typing import Any, Callable, Dict, List, Optional
from uuid import UUID

from domain.rules.financial_rules import average_field_confidence, validate_documento_fiscal
from domain.schemas.events import (
    DocumentoClassificadoEvent,
    DocumentoClassificadoPayload,
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
        prompt_context = self._build_prompt_context(event)
        try:
            file_bytes = self._storage.download_bytes(payload.s3_bucket, payload.s3_key)
            strategy = self._llm_strategy_factory()
            self._last_strategy = strategy
            extraction = await strategy.extract_document(
                file_bytes=file_bytes,
                mime_type=payload.content_type,
                prompt_context=prompt_context,
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

    def _build_prompt_context(self, event: DocumentoRecebidoEvent) -> str:
        parts: List[str] = []
        origem = event.payload.origem
        phone = event.payload.sender_phone or origem.remetente_phone
        name = event.payload.sender_name
        if phone or name:
            parts.append(f"Remetente: {name or 'Desconhecido'} (Telefone: {phone or 'N/A'})")

        candidatos = event.payload.convenios_candidatos
        if candidatos:
            parts.append("Convênios candidatos associados ao remetente:")
            for c in candidatos:
                cid = c.get("convenioId")
                pid = c.get("prefeituraId")
                papel = c.get("papelEspecifico", "N/A")
                is_p = c.get("principal", False)
                parts.append(f"- Convênio: {cid}, Prefeitura: {pid}, Papel: {papel}, Principal: {is_p}")
        else:
            parts.append("Remetente novo ou sem convênios pré-vinculados.")

        historico = event.payload.historico_recente_conversa
        if historico:
            parts.append("Janela de contexto recente (mensagens anteriores da conversa / áudios transcritos):")
            for msg in historico:
                tipo = msg.get("tipo", "TEXT")
                txt = msg.get("texto") or msg.get("audioTranscription") or ""
                if txt:
                    parts.append(f"- [{tipo}]: {txt}")

        return "\n".join(parts)

    def classify(
        self,
        event: DocumentoRecebidoEvent,
        extraido: DocumentoExtraidoEvent,
    ) -> DocumentoClassificadoEvent:
        payload = event.payload
        candidatos = payload.convenios_candidatos or []
        remetente_novo = payload.remetente_novo
        historico = payload.historico_recente_conversa or []

        corpus_parts = []
        if payload.nome_arquivo_original:
            corpus_parts.append(str(payload.nome_arquivo_original))
        for m in historico:
            t = m.get("texto") or m.get("audioTranscription") or ""
            if t:
                corpus_parts.append(str(t))
        raw_corpus = " ".join(corpus_parts).lower()
        import unicodedata
        normalized_corpus = unicodedata.normalize('NFKD', raw_corpus).encode('ASCII', 'ignore').decode('utf-8')
        context_corpus = f"{raw_corpus} {normalized_corpus}"

        # Determina a fase e categoria do documento em todas as 10 fases oficiais
        fase = "FASE_05_EXECUCAO_FINANCEIRA"
        categoria = "DOCUMENTO_HABIL"

        if "proposta" in context_corpus or "plano de trabalho" in context_corpus or "cauc" in context_corpus:
            fase = "FASE_00_PROPOSTA"
            categoria = "PROPOSTA_PLANO_TRABALHO" if "cauc" not in context_corpus else "CERTIDAO_CAUC"
        elif "termo de convenio" in context_corpus or "termo_convenio" in context_corpus or "celebracao" in context_corpus or "dou" in context_corpus:
            fase = "FASE_01_CELEBRACAO"
            categoria = "TERMO_CONVENIO"
        elif "spa" in context_corpus or "lae" in context_corpus or "projeto" in context_corpus or "sinapi" in context_corpus or "licenc" in context_corpus or "suspensiva" in context_corpus:
            fase = "FASE_02_CLAUSULA_SUSPENSIVA"
            categoria = "SPA_LAE_CAIXA" if ("spa" in context_corpus or "lae" in context_corpus) else "PROJETO_ENGENHARIA"
        elif "contrato administrativo" in context_corpus or "licitacao" in context_corpus or "edital" in context_corpus or "aio" in context_corpus or "homologacao" in context_corpus:
            fase = "FASE_03_LICITACAO"
            categoria = "CONTRATO_ADMINISTRATIVO" if "contrato" in context_corpus else "LICITACAO"
        elif "medic" in context_corpus or "boletim" in context_corpus or "bm" in context_corpus or "alvenaria" in context_corpus or "fotografico" in context_corpus:
            fase = "FASE_04_EXECUCAO_FISICA"
            categoria = "BOLETIM_MEDICAO"
        elif "nota fiscal" in context_corpus or "danfe" in context_corpus or "nf" in context_corpus or "obtv" in context_corpus or "habil" in context_corpus:
            fase = "FASE_05_EXECUCAO_FINANCEIRA"
            categoria = "DOCUMENTO_HABIL" if "obtv" not in context_corpus else "ORDEM_BANCARIA_OBTV"
        elif "aditivo" in context_corpus or "apostilamento" in context_corpus or "reprogramacao" in context_corpus:
            fase = "FASE_06_ALTERACOES_CONTRATUAIS"
            categoria = "TERMO_ADITIVO"
        elif "recebimento" in context_corpus or "prestacao" in context_corpus or "rco" in context_corpus or "cumprimento" in context_corpus:
            fase = "FASE_07_PRESTACAO_CONTAS"
            categoria = "TERMO_RECEBIMENTO"
        elif "gru" in context_corpus or "recolhimento" in context_corpus or "saldo zero" in context_corpus or "encerramento" in context_corpus:
            fase = "FASE_08_ENCERRAMENTO_FINANCEIRO"
            categoria = "GUIA_RECOLHIMENTO_UNIAO"
        elif "diligencia" in context_corpus or "glosa" in context_corpus or "defesa" in context_corpus or "notificacao" in context_corpus or "tce" in context_corpus or "passivo" in context_corpus:
            fase = "FASE_09_PASSIVO_JURIDICO"
            categoria = "NOTIFICACAO_DILIGENCIA"

        resumo = (
            f"Recebido via WhatsApp de {payload.sender_name or payload.sender_phone or 'contato'}. "
            f"Arquivo: {payload.nome_arquivo_original or 'anexo'}."
        )

        # Regras de Inferência e Confiança
        convenio_id: Optional[UUID] = None
        prefeitura_id: Optional[UUID] = payload.origem.prefeitura_id
        motivo_ambiguidade: Optional[str] = None
        confidence_score: float = 0.0
        direcionar_triagem: bool = False

        if not candidatos or remetente_novo:
            # Caso 1: Remetente sem convênio vinculado — não deve ser arquivado nem triado
            direcionar_triagem = False
            confidence_score = 0.0
            motivo_ambiguidade = "Remetente não possui convênios vinculados no sistema."
        elif len(candidatos) == 1:
            # Caso 2: Contato com exatamente 1 convênio associado
            cand = candidatos[0]
            cid_raw = cand.get("convenioId")
            pid_raw = cand.get("prefeituraId")
            convenio_id = UUID(str(cid_raw)) if cid_raw else None
            prefeitura_id = UUID(str(pid_raw)) if pid_raw else prefeitura_id
            confidence_score = 0.95
            direcionar_triagem = False
            motivo_ambiguidade = None
        else:
            # Caso 3: Contato com múltiplos convênios (1:N) -> Verificação de desambiguação
            principal = next((c for c in candidatos if c.get("principal")), candidatos[0])
            cid_raw = principal.get("convenioId")
            pid_raw = principal.get("prefeituraId")
            convenio_id = UUID(str(cid_raw)) if cid_raw else None
            prefeitura_id = UUID(str(pid_raw)) if pid_raw else prefeitura_id

            # Verifica se o texto da conversa resolve o convênio específico
            resolved_specifically = False
            for c in candidatos:
                papel = str(c.get("papelEspecifico", "")).lower()
                if papel and papel in context_corpus:
                    convenio_id = UUID(str(c.get("convenioId")))
                    pid_spec = c.get("prefeituraId")
                    if pid_spec:
                        prefeitura_id = UUID(str(pid_spec))
                    resolved_specifically = True
                    break

            if resolved_specifically:
                confidence_score = 0.92
                direcionar_triagem = False
                motivo_ambiguidade = None
            else:
                confidence_score = 0.75
                direcionar_triagem = True
                motivo_ambiguidade = (
                    f"Remetente associado a {len(candidatos)} convênios ativos; "
                    "histórico da conversa não especificou a obra única com certeza absoluta."
                )

        # Se o score geral for <= 0.90, encaminha para triagem
        if confidence_score <= 0.90:
            direcionar_triagem = True

        return DocumentoClassificadoEvent(
            tenantId=event.tenant_id,
            correlationId=event.correlation_id,
            payload=DocumentoClassificadoPayload(
                documentoId=payload.documento_id,
                mensagemInboundId=payload.documento_id,
                tenantId=event.tenant_id,
                prefeituraId=prefeitura_id,
                convenioId=convenio_id,
                faseCicloVida=fase,
                categoriaDocumento=categoria,
                confidenceScore=confidence_score,
                motivoAmbiguidade=motivo_ambiguidade,
                direcionarTriagem=direcionar_triagem,
                remetentePhone=payload.sender_phone or payload.origem.remetente_phone,
                remetenteName=payload.sender_name,
                remetenteNovo=remetente_novo,
                conteudoResumo=resumo,
                s3Bucket=payload.s3_bucket,
                s3Key=payload.s3_key,
                nomeArquivoOriginal=payload.nome_arquivo_original,
                extracao=extraido.payload.extracao,
            ),
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
                nomeArquivoOriginal=event.payload.nome_arquivo_original,
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
