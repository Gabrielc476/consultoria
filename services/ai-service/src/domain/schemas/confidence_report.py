from typing import Optional

from pydantic import BaseModel, Field

from domain.schemas.bounding_box_schema import BoundingBox


class ExtractedField(BaseModel):
    valor: Optional[str] = None
    confianca: float = Field(description="Score de confiança de 0.0 a 1.0", ge=0.0, le=1.0)
    coordenadas: Optional[BoundingBox] = None
