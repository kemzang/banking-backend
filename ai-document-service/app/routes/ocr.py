from typing import Annotated

from fastapi import APIRouter, Depends, File, Header, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.repositories.document_repository import DocumentRepository
from app.schemas.ocr_schema import (
    OcrAnalysisResponse,
    DocumentAnalysisResponse,
    ErrorResponse,
    OcrHistorySuccessResponse,
    OcrSuccessResponse,
)
from app.services.ocr_service import OcrService
from app.utils.response import success_response


router = APIRouter()
public_router = APIRouter()


def get_ocr_service(db: Session = Depends(get_db)) -> OcrService:
    return OcrService(DocumentRepository(db))


@router.post(
    "/extract",
    response_model=OcrSuccessResponse,
    summary="Extraire le texte d'une image",
    responses={
        400: {"model": ErrorResponse, "description": "Image invalide"},
        422: {"model": ErrorResponse, "description": "Fichier absent"},
        503: {"model": ErrorResponse, "description": "Tesseract indisponible"},
    },
)
async def extract_text(
    file: Annotated[
        UploadFile,
        File(description="Image PNG, JPG ou JPEG à analyser"),
    ],
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analysis = await service.extract(file)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="OCR effectué avec succès",
        data=data,
    )


@public_router.post(
    "/analysis",
    response_model=DocumentAnalysisResponse,
    summary="Analyser un document par OCR",
    responses={
        400: {"model": ErrorResponse, "description": "Document invalide"},
        422: {"model": ErrorResponse, "description": "Fichier absent"},
        503: {"model": ErrorResponse, "description": "Tesseract indisponible"},
    },
)
async def analyze_document(
    file: Annotated[
        UploadFile,
        File(description="Image PNG ou JPEG, 10 Mo maximum"),
    ],
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analysis = await service.extract(file)
    return {
        "analysisId": analysis.id,
        "status": analysis.status,
        "documentType": analysis.document_type or "UNKNOWN",
        "extractedText": analysis.extracted_text or "",
        "extractedFields": analysis.extracted_fields,
        "missingFields": analysis.missing_fields,
        "confidenceScore": round(analysis.confidence_score),
        "reliabilityLevel": analysis.reliability_level,
        "decisionRecommendation": (
            analysis.recommendation or "REQUEST_NEW_DOCUMENT"
        ),
        "message": analysis.message or "Analyse terminée.",
    }


@router.get(
    "/history",
    response_model=OcrHistorySuccessResponse,
    summary="Lister l'historique OCR",
)
def get_history(
    service: OcrService = Depends(get_ocr_service),
    x_user_roles: str | None = Header(default=None),
) -> dict:
    if x_user_roles and "CLIENT" in {role.strip() for role in x_user_roles.split(",")}:
        return success_response(message="Historique OCR personnel", data=[])
    analyses = service.get_history()
    data = [
        OcrAnalysisResponse.model_validate(item).model_dump(mode="json")
        for item in analyses
    ]
    return success_response(
        message="Historique OCR récupéré avec succès",
        data=data,
    )


@router.get(
    "/history/{analysis_id}",
    response_model=OcrSuccessResponse,
    summary="Consulter une analyse OCR",
    responses={
        404: {"model": ErrorResponse, "description": "Analyse introuvable"},
    },
)
def get_history_item(
    analysis_id: int,
    service: OcrService = Depends(get_ocr_service),
    x_user_roles: str | None = Header(default=None),
) -> dict:
    if x_user_roles and "CLIENT" in {role.strip() for role in x_user_roles.split(",")}:
        raise HTTPException(status_code=403, detail="Acces direct a l'historique OCR interdit au client")
    analysis = service.get_history_item(analysis_id)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="Analyse OCR récupérée avec succès",
        data=data,
    )
