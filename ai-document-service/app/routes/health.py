from fastapi import APIRouter

from app.utils.response import success_response

router = APIRouter()
public_router = APIRouter()


@router.get("/")
def health_check() -> dict:
    return success_response(
        message="Microservice Python opérationnel",
        data={},
    )


@public_router.get("/health", summary="Vérifier la disponibilité du service")
def public_health_check() -> dict[str, str]:
    return {
        "status": "UP",
        "service": "ai-document-service",
    }
