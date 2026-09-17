from typing import List, Optional

from pydantic import BaseModel, Field, field_validator

from domain.schemas.bounding_box_schema import BoundingBox
from domain.schemas.confidence_report import ExtractedField


class RetencaoTributaria(BaseModel):
    tipo: str = Field(description="INSS, ISS, IRRF, PIS, COFINS, CSLL")
    aliquota: float
    valor: float
    confianca: float = Field(default=0.99, ge=0.0, le=1.0)
    coordenadas: Optional[BoundingBox] = None

    @field_validator("aliquota", "valor", mode="before")
    @classmethod
    def _coerce_float(cls, v):
        if v is None:
            return 0.0
        if isinstance(v, (int, float)):
            return float(v)
        if isinstance(v, str):
            cleaned = (
                v.strip()
                .replace("%", "")
                .replace("R$", "")
                .replace(" ", "")
            )
            if "," in cleaned and "." in cleaned:
                cleaned = cleaned.replace(".", "").replace(",", ".")
            elif "," in cleaned:
                cleaned = cleaned.replace(",", ".")
            try:
                return float(cleaned)
            except ValueError:
                return 0.0
        return float(v)

    @field_validator("confianca", mode="before")
    @classmethod
    def _coerce_confianca(cls, v):
        if v is None:
            return 0.99
        try:
            val = float(v)
            return max(0.0, min(1.0, val))
        except (ValueError, TypeError):
            return 0.99


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

    @field_validator("retencoes", mode="before")
    @classmethod
    def _coerce_retencoes(cls, v):
        if v is None:
            return []
        if isinstance(v, list):
            return v
        return []

    @field_validator(
        "tipo_documento",
        "numero_documento",
        "data_emissao",
        "valor_bruto",
        "valor_liquido",
        "cnpj_credor",
        "razao_social_credor",
        "descricao_servico",
        mode="before",
    )
    @classmethod
    def _coerce_required_extracted_field(cls, v):
        if v is None:
            return {"valor": None, "confianca": 0.0, "coordenadas": None}
        if isinstance(v, (str, int, float)):
            val = str(v).strip()
            if not val or val.lower() in ("null", "none"):
                return {"valor": None, "confianca": 0.0, "coordenadas": None}
            return {"valor": val, "confianca": 0.99, "coordenadas": None}
        if isinstance(v, dict):
            if "confianca" not in v or v["confianca"] is None:
                v_copy = dict(v)
                v_copy["confianca"] = 0.99
                return v_copy
        return v

    @field_validator(
        "numero_empenho",
        "chave_acesso_nfe",
        mode="before",
    )
    @classmethod
    def _coerce_optional_extracted_field(cls, v):
        if v is None:
            return None
        if isinstance(v, (str, int, float)):
            val = str(v).strip()
            if not val or val.lower() in ("null", "none"):
                return None
            return {"valor": val, "confianca": 0.99, "coordenadas": None}
        if isinstance(v, dict):
            if "confianca" not in v or v["confianca"] is None:
                v_copy = dict(v)
                v_copy["confianca"] = 0.99
                return v_copy
        return v
