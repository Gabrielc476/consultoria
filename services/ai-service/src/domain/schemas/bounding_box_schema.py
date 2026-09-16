from typing import Optional

from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    ymin: int = Field(description="Coordenada Y mínima normalizada (0 a 1000)", ge=0, le=1000)
    xmin: int = Field(description="Coordenada X mínima normalizada (0 a 1000)", ge=0, le=1000)
    ymax: int = Field(description="Coordenada Y máxima normalizada (0 a 1000)", ge=0, le=1000)
    xmax: int = Field(description="Coordenada X máxima normalizada (0 a 1000)", ge=0, le=1000)
    page_number: int = Field(default=1, description="Número da página (1-based)", ge=1)
