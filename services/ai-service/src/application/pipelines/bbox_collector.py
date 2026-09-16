from typing import Any, Dict, Optional

from domain.schemas.bounding_box_schema import BoundingBox
from domain.schemas.documento_habil_schema import DocumentoHabilExtraction


def collect_bounding_boxes(extraction: DocumentoHabilExtraction) -> Dict[str, Any]:
    boxes: Dict[str, Any] = {}
    field_map = {
        "tipo_documento": extraction.tipo_documento,
        "numero_documento": extraction.numero_documento,
        "data_emissao": extraction.data_emissao,
        "valor_bruto": extraction.valor_bruto,
        "valor_liquido": extraction.valor_liquido,
        "cnpj_credor": extraction.cnpj_credor,
        "razao_social_credor": extraction.razao_social_credor,
        "descricao_servico": extraction.descricao_servico,
    }
    if extraction.numero_empenho is not None:
        field_map["numero_empenho"] = extraction.numero_empenho
    if extraction.chave_acesso_nfe is not None:
        field_map["chave_acesso_nfe"] = extraction.chave_acesso_nfe

    for name, field in field_map.items():
        serialized = _serialize_box(field.coordenadas)
        if serialized is not None:
            boxes[name] = serialized

    for index, retencao in enumerate(extraction.retencoes):
        serialized = _serialize_box(retencao.coordenadas)
        if serialized is not None:
            boxes[f"retencoes[{index}]"] = serialized
    return boxes


def _serialize_box(box: Optional[BoundingBox]) -> Optional[Dict[str, Any]]:
    if box is None:
        return None
    return box.model_dump()
