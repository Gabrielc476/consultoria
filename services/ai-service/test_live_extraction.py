"""
GovFlow AI Service - Script de Teste Direto de Extração Multimodal.
Executa a extração usando as credenciais do .env sem depender de Docker ou RabbitMQ.
"""

import asyncio
import os
import sys
from pathlib import Path

# Adiciona src ao sys.path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR / "src"))

from domain.rules.financial_rules import average_field_confidence, validate_documento_fiscal
from infrastructure.config.settings import get_settings
from infrastructure.llm.fallback_provider import LLMProviderStrategy
from infrastructure.llm.gemini_provider import GeminiProvider


async def main():
    print("=" * 70)
    print("  GovFlow - Teste Direto de Extração de Documentos (AI Service)")
    print("=" * 70)

    settings = get_settings()
    key = settings.gemini_api_key.strip()

    if not key:
        print("[ERRO] GEMINI_API_KEY não foi encontrada no .env!")
        sys.exit(1)

    masked_key = key[:6] + "..." + key[-4:]
    print(f"[*] Chave de API: {masked_key}")
    print(f"[*] Modelo Primário:   {settings.gemini_primary_model}")
    print(f"[*] Modelo Secundário: {settings.gemini_fallback_model}")
    print(f"[*] Threshold Mínimo:  {settings.llm_confidence_threshold}")
    print("-" * 70)

    pdf_path = BASE_DIR / "tests" / "fixtures" / "nf_smoke.pdf"
    if len(sys.argv) > 1:
        pdf_path = Path(sys.argv[1])

    if not pdf_path.exists():
        print(f"[ERRO] Arquivo PDF não encontrado: {pdf_path}")
        sys.exit(1)

    print(f"[*] Lendo documento: {pdf_path.name} ({pdf_path.stat().st_size} bytes)...")
    file_bytes = pdf_path.read_bytes()

    print("[*] Inicializando Provedor de IA com Estratégia de Fallback...")
    primary = GeminiProvider(key, settings.gemini_primary_model)
    fallback = GeminiProvider(key, settings.gemini_fallback_model)
    strategy = LLMProviderStrategy(
        primary=primary,
        fallback=fallback,
        confidence_threshold=settings.llm_confidence_threshold,
    )

    print("[*] Enviando documento para extração multimodal...")
    try:
        extraction = await strategy.extract_document(
            file_bytes=file_bytes,
            mime_type="application/pdf",
            prompt_context="",
        )
    except Exception as exc:
        print(f"\n[FALHA] Erro durante a chamada à API: {exc}")
        sys.exit(1)

    print("\n" + "=" * 70)
    print("  RESULTADO DA EXTRAÇÃO")
    print("=" * 70)
    print(f"Modelo Utilizado: {strategy.last_provider_name} ({strategy.last_model_name})")
    print(f"Fallback Ativado: {'SIM' if strategy.fallback_used else 'NÃO'}")
    print("-" * 70)
    print(f"Tipo do Documento:  {extraction.tipo_documento}")
    print(f"Número Documento:   {extraction.numero_documento.value} (confiança: {extraction.numero_documento.confidence:.2f})")
    print(f"Data Emissão:       {extraction.data_emissao.value} (confiança: {extraction.data_emissao.confidence:.2f})")
    print(f"CNPJ Emissor:       {extraction.cnpj_emissor.value} (confiança: {extraction.cnpj_emissor.confidence:.2f})")
    print(f"CNPJ Destinatário:  {extraction.cnpj_destinatario.value} (confiança: {extraction.cnpj_destinatario.confidence:.2f})")
    print(f"Valor Total:        R$ {extraction.valor_total.value} (confiança: {extraction.valor_total.confidence:.2f})")
    print(f"Itens Extraídos:    {len(extraction.itens)}")

    for i, item in enumerate(extraction.itens, 1):
        print(f"  [{i}] {item.descricao[:40]} | Qtd: {item.quantidade} | Unit: R$ {item.valor_unitario} | Total: R$ {item.valor_total}")

    print("-" * 70)
    validacao = validate_documento_fiscal(extraction)
    confidence = average_field_confidence(extraction)
    print(f"Consistência Matemática: {'VÁLIDA' if validacao.consistente else 'DIVERGENTE'}")
    print(f"Diferença Calculada:     R$ {validacao.diferenca_calculada}")
    if validacao.alertas:
        print(f"Alertas:                 {validacao.alertas}")
    print(f"Score Médio de Confiança: {confidence:.2%}")
    print("=" * 70)
    print("Extração concluída com sucesso!")


if __name__ == "__main__":
    asyncio.run(main())
