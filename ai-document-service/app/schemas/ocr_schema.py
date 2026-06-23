from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class OcrAnalysisResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    original_filename: str
    extracted_text: str | None
    confidence_score: float
    status: str
    created_at: datetime
    document_type: str = "UNKNOWN"
    extracted_fields: dict[str, str | None] = Field(default_factory=dict)
    missing_fields: list[str] = Field(default_factory=list)
    reliability_level: str = "LOW"
    recommendation: str = "REQUEST_NEW_DOCUMENT"
    message: str | None = None

    @field_validator("document_type", mode="before")
    @classmethod
    def default_document_type(cls, value: str | None) -> str:
        return value or "UNKNOWN"

    @field_validator("recommendation", mode="before")
    @classmethod
    def default_recommendation(cls, value: str | None) -> str:
        return value or "REQUEST_NEW_DOCUMENT"


class DocumentAnalysisResponse(BaseModel):
    analysisId: int
    status: str
    documentType: str
    extractedText: str
    extractedFields: dict[str, str | None]
    missingFields: list[str]
    confidenceScore: int
    reliabilityLevel: str
    decisionRecommendation: str
    message: str


class OcrSuccessResponse(BaseModel):
    status: Literal["success"]
    message: str
    data: OcrAnalysisResponse


class OcrHistorySuccessResponse(BaseModel):
    status: Literal["success"]
    message: str
    data: list[OcrAnalysisResponse]


class ErrorResponse(BaseModel):
    status: Literal["error"]
    message: str
    errors: Any
