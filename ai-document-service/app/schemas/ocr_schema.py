from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, computed_field


class OcrAnalysisResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    original_filename: str
    extracted_text: str | None
    confidence_score: float
    status: str
    client_id: int | None = None
    document_type: str | None = None
    structured_data: str | None = None
    created_at: datetime

    @computed_field(return_type=dict | None)
    @property
    def structured_fields(self) -> dict | None:
        if self.structured_data:
            import json
            try:
                return json.loads(self.structured_data)
            except (json.JSONDecodeError, TypeError):
                return None
        return None


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
