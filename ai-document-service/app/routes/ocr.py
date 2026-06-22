from typing import Annotated

from fastapi import APIRouter, Depends, File, Form, Query, UploadFile
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.repositories.document_repository import DocumentRepository
from app.schemas.ocr_schema import (
    OcrAnalysisResponse,
    ErrorResponse,
    OcrHistorySuccessResponse,
    OcrSuccessResponse,
)
from app.services.ocr_service import OcrService
from app.utils.response import success_response


router = APIRouter()


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
    client_id: Annotated[
        int | None,
        Form(description="Identifiant du client associe au document"),
    ] = None,
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analysis = await service.extract(file, client_id=client_id)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="OCR effectué avec succès",
        data=data,
    )


@router.get(
    "/history",
    response_model=OcrHistorySuccessResponse,
    summary="Lister l'historique OCR",
)
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
) -> dict:
    analysis = service.get_history_item(analysis_id)
    data = OcrAnalysisResponse.model_validate(analysis).model_dump(mode="json")
    return success_response(
        message="Analyse OCR récupérée avec succès",
        data=data,
    )


@router.get(
    "/analysis/client/{client_id}",
    response_model=OcrHistorySuccessResponse,
    summary="Consulter les analyses OCR d'un client",
    responses={
        404: {"model": ErrorResponse, "description": "Aucune analyse trouvee"},
    },
)
def get_client_analyses(
    client_id: int,
    service: OcrService = Depends(get_ocr_service),
) -> dict:
    analyses = service.get_by_client(client_id)
    data = [
        OcrAnalysisResponse.model_validate(item).model_dump(mode="json")
        for item in analyses
    ]
    return success_response(
        message=f"Analyses OCR du client {client_id} récupérées avec succès",
        data=data,
    )
