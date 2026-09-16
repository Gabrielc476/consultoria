from decimal import Decimal

from domain.rules.financial_rules import (
    average_field_confidence,
    validate_documento_fiscal,
)
from domain.schemas.bounding_box_schema import BoundingBox
from domain.schemas.confidence_report import ExtractedField
from domain.schemas.documento_habil_schema import DocumentoHabilExtraction, RetencaoTributaria


def _field(valor: str, confianca: float = 0.9) -> ExtractedField:
    return ExtractedField(
        valor=valor,
        confianca=confianca,
        coordenadas=BoundingBox(ymin=10, xmin=20, ymax=30, xmax=40, page_number=1),
    )


def _extraction(
    bruto: str = "85400.00",
    liquido: str = "71736.00",
    retencoes: list[tuple[str, float, float]] | None = None,
) -> DocumentoHabilExtraction:
    if retencoes is None:
        retencoes = [("INSS", 11.0, 9394.00), ("ISS", 5.0, 4270.00)]
    return DocumentoHabilExtraction(
        tipo_documento=_field("NOTA_FISCAL_SERVICOS"),
        numero_documento=_field("0001542"),
        data_emissao=_field("2026-08-20"),
        valor_bruto=_field(bruto, 0.91),
        valor_liquido=_field(liquido, 0.88),
        cnpj_credor=_field("08123456000190"),
        razao_social_credor=_field("CONSTRUTORA EXEMPLO LTDA"),
        descricao_servico=_field("Medição 02"),
        retencoes=[
            RetencaoTributaria(
                tipo=tipo,
                aliquota=aliq,
                valor=valor,
                confianca=0.9,
                coordenadas=BoundingBox(ymin=1, xmin=2, ymax=3, xmax=4, page_number=1),
            )
            for tipo, aliq, valor in retencoes
        ],
    )


def test_validate_documento_fiscal_consistente():
    extraction = _extraction()
    result = validate_documento_fiscal(extraction)
    assert result.consistente is True
    assert result.diferenca == Decimal("0.00")
    assert result.total_retencoes == Decimal("13664.00")
    assert extraction.alertas_inconsistencia == []


def test_validate_documento_fiscal_tolerancia_centavos():
    # Diferença de R$ 0,02 (arredondamento fiscal) deve ser consistente
    extraction_02 = _extraction(liquido="71736.02")
    result_02 = validate_documento_fiscal(extraction_02)
    assert result_02.consistente is True
    assert result_02.diferenca == Decimal("0.02")

    # Diferença de R$ 0,03 (acima de R$ 0,02) deve gerar alerta
    extraction_03 = _extraction(liquido="71736.03")
    result_03 = validate_documento_fiscal(extraction_03)
    assert result_03.consistente is False
    assert result_03.diferenca == Decimal("0.03")
    assert any("INCONSISTENCIA_MATEMATICA" in a for a in extraction_03.alertas_inconsistencia)


def test_validate_documento_fiscal_inconsistente_gera_alerta():
    extraction = _extraction(liquido="72000.00")
    result = validate_documento_fiscal(extraction)
    assert result.consistente is False
    assert result.diferenca == Decimal("264.00")
    assert any("INCONSISTENCIA_MATEMATICA" in a for a in extraction.alertas_inconsistencia)
    assert extraction.valor_bruto.confianca <= 0.59
    assert extraction.valor_liquido.confianca <= 0.59


def test_average_field_confidence():
    extraction = _extraction()
    score = average_field_confidence(extraction)
    assert 0.0 <= score <= 1.0
    assert score > 0.8


def test_schema_accepts_official_tipo():
    extraction = _extraction()
    dumped = extraction.model_dump(mode="json")
    assert dumped["tipo_documento"]["valor"] == "NOTA_FISCAL_SERVICOS"
    assert dumped["valor_bruto"]["coordenadas"]["ymin"] == 10
