from typing import List, Optional

from pydantic import BaseModel, Field

from domain.schemas.bounding_box_schema import BoundingBox
from domain.schemas.confidence_report import ExtractedField


class RetencaoTributaria(BaseModel):
    tipo: str = Field(description="INSS, ISS, IRRF, PIS, COFINS, CSLL")
    aliquota: float
    valor: float
    confianca: float = Field(ge=0.0, le=1.0)
    coordenadas: Optional[BoundingBox] = None


class DocumentoHabilExtraction(BaseModel):
    tipo_documento: ExtractedField = Field(
        description="NOTA_FISCAL_SERVICOS, NOTA_FISCAL_MERCADORIAS ou RECIBO_LEGAL"
    )
    numero_documento: ExtractedField
    data_emissao: ExtractedField = Field(description="Formato YYYY-MM-DD")
    valor_bruto: ExtractedField
    valor_liquido: ExtractedField
    cnpj_credor: ExtractedField = Field(description="CNPJ sem formatação (apenas números)")
    razao_social_credor: ExtractedField
    numero_empenho: Optional[ExtractedField] = None
    descricao_servico: ExtractedField
    chave_acesso_nfe: Optional[ExtractedField] = None
    retencoes: List[RetencaoTributaria] = Field(default_factory=list)
    alertas_inconsistencia: List[str] = Field(
        default_factory=list,
        description="Erros matemáticos detectados após validação determinística",
    )
