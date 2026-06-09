from fastapi import APIRouter, Depends, File, UploadFile
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.repositories.document_repository import DocumentRepository
from app.schemas.ocr_schema import (
    OcrAnalysisResponse,
    OcrHistorySuccessResponse,
    OcrSuccessResponse,
)
from app.services.ocr_service import OcrService
from app.utils.response import success_response


router = APIRouter()


def get_ocr_service(db: Session = Depends(get_db)) -> OcrService:
    return OcrService(DocumentRepository(db))


@router.post("/extract", response_model=OcrSuccessResponse)
async def extract_text(
    file: UploadFile | None = File(default=None),
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analysis = await service.extract(file)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="OCR effectué avec succès",
        data=data,
    )


@router.get("/history", response_model=OcrHistorySuccessResponse)
def get_history(
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analyses = service.get_history()
    data = [
        OcrAnalysisResponse.model_validate(item).model_dump(mode="json")
        for item in analyses
    ]
    return success_response(
        message="Historique OCR récupéré avec succès",
        data=data,
    )


@router.get("/history/{analysis_id}", response_model=OcrSuccessResponse)
def get_history_item(
    analysis_id: int,
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analysis = service.get_history_item(analysis_id)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="Analyse OCR récupérée avec succès",
        data=data,
    )
