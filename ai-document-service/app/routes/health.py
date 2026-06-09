from fastapi import APIRouter

from app.utils.response import success_response

router = APIRouter()


@router.get("/")
def health_check() -> dict:
    return success_response(
        message="Microservice Python opérationnel",
        data={},
    )
