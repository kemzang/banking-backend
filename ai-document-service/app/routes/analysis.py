from fastapi import APIRouter

from app.schemas.analysis_schema import AnalysisRequest, AnalysisResult
from app.services.analysis_service import AnalysisService
from app.utils.response import success_response

router = APIRouter()
analysis_service = AnalysisService()


@router.post("/")
def analyze_data(payload: AnalysisRequest) -> dict:
    result = analysis_service.analyze_values(payload.values)
    return success_response(
        message="Analyse effectuée avec succès",
        data=AnalysisResult(**result).model_dump(),
    )
