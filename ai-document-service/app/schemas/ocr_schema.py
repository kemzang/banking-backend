from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict


class OcrAnalysisResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    original_filename: str
    extracted_text: str | None
    confidence_score: float
    status: str
    created_at: datetime


class OcrSuccessResponse(BaseModel):
    status: Literal["success"]
    message: str
    data: OcrAnalysisResponse


class OcrHistorySuccessResponse(BaseModel):
    status: Literal["success"]
    message: str
    data: list[OcrAnalysisResponse]
