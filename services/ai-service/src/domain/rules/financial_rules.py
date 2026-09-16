from decimal import Decimal, ROUND_HALF_UP
from typing import List

from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from domain.schemas.events import ValidacaoMatematicaResult


MONETARY_CONFIDENCE_ON_INCONSISTENCY = 0.59
ZERO = Decimal("0.00")
TOLERANCE = Decimal("0.02")


def parse_money(raw: str | None) -> Decimal | None:
    if raw is None or str(raw).strip() == "":
        return None
    text = str(raw).strip().replace("R$", "").replace(" ", "")
    if "," in text and "." in text:
        text = text.replace(".", "").replace(",", ".")
    elif "," in text:
        text = text.replace(",", ".")
    try:
        return Decimal(text).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except Exception:
        return None


def sum_retencoes(retencoes_valores: List[float]) -> Decimal:
    total = ZERO
    for value in retencoes_valores:
        total += Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def validate_documento_fiscal(
    extraction: DocumentoHabilExtraction,
) -> ValidacaoMatematicaResult:
    valor_bruto = parse_money(extraction.valor_bruto.valor)
    valor_liquido_informado = parse_money(extraction.valor_liquido.valor)
    total_retencoes = sum_retencoes([r.valor for r in extraction.retencoes])

    if valor_bruto is None:
        alert = "INCONSISTENCIA_MATEMATICA: valor_bruto ausente ou inválido"
        extraction.alertas_inconsistencia.append(alert)
        return ValidacaoMatematicaResult(
            valor_bruto=None,
            total_retencoes=total_retencoes,
            valor_liquido_informado=valor_liquido_informado,
            valor_liquido_calculado=None,
            diferenca=None,
            consistente=False,
            tolerancia=TOLERANCE,
        )

    valor_liquido_calculado = (valor_bruto - total_retencoes).quantize(
        Decimal("0.01"),
        rounding=ROUND_HALF_UP,
    )

    if valor_liquido_informado is None:
        diferenca = None
        consistente = False
        alert = (
            "INCONSISTENCIA_MATEMATICA: valor_liquido ausente ou inválido; "
            f"calculado={valor_liquido_calculado}"
        )
        extraction.alertas_inconsistencia.append(alert)
        _downgrade_monetary_confidence(extraction)
        return ValidacaoMatematicaResult(
            valor_bruto=valor_bruto,
            total_retencoes=total_retencoes,
            valor_liquido_informado=None,
            valor_liquido_calculado=valor_liquido_calculado,
            diferenca=None,
            consistente=False,
            tolerancia=TOLERANCE,
        )

    diferenca = (valor_liquido_informado - valor_liquido_calculado).quantize(
        Decimal("0.01"),
        rounding=ROUND_HALF_UP,
    )
    consistente = abs(diferenca) <= TOLERANCE

    if not consistente:
        alert = (
            f"INCONSISTENCIA_MATEMATICA: valor_bruto({valor_bruto}) - "
            f"total_retencoes({total_retencoes}) = {valor_liquido_calculado} "
            f"!= valor_liquido_informado({valor_liquido_informado}); "
            f"diferenca={diferenca}"
        )
        extraction.alertas_inconsistencia.append(alert)
        _downgrade_monetary_confidence(extraction)

    return ValidacaoMatematicaResult(
        valor_bruto=valor_bruto,
        total_retencoes=total_retencoes,
        valor_liquido_informado=valor_liquido_informado,
        valor_liquido_calculado=valor_liquido_calculado,
        diferenca=diferenca,
        consistente=consistente,
        tolerancia=TOLERANCE,
    )


def _downgrade_monetary_confidence(extraction: DocumentoHabilExtraction) -> None:
    for field in (extraction.valor_bruto, extraction.valor_liquido):
        if field.confianca > MONETARY_CONFIDENCE_ON_INCONSISTENCY:
            field.confianca = MONETARY_CONFIDENCE_ON_INCONSISTENCY
    for retencao in extraction.retencoes:
        if retencao.confianca > MONETARY_CONFIDENCE_ON_INCONSISTENCY:
            retencao.confianca = MONETARY_CONFIDENCE_ON_INCONSISTENCY


def average_field_confidence(extraction: DocumentoHabilExtraction) -> float:
    scores: List[float] = [
        extraction.tipo_documento.confianca,
        extraction.numero_documento.confianca,
        extraction.data_emissao.confianca,
        extraction.valor_bruto.confianca,
        extraction.valor_liquido.confianca,
        extraction.cnpj_credor.confianca,
        extraction.razao_social_credor.confianca,
        extraction.descricao_servico.confianca,
    ]
    if extraction.numero_empenho is not None:
        scores.append(extraction.numero_empenho.confianca)
    if extraction.chave_acesso_nfe is not None:
        scores.append(extraction.chave_acesso_nfe.confianca)
    scores.extend(r.confianca for r in extraction.retencoes)
    if not scores:
        return 0.0
    return round(sum(scores) / len(scores), 3)
