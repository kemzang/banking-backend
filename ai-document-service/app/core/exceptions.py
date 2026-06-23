import logging
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.utils.response import error_response


LOGGER = logging.getLogger(__name__)


class AppException(Exception):
    def __init__(
        self,
        message: str,
        status_code: int = 400,
        errors: Any | None = None,
    ) -> None:
        self.message = message
        self.status_code = status_code
        self.errors = errors or {}
        super().__init__(message)


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppException)
    async def app_exception_handler(
        request: Request,
        exc: AppException,
    ) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=error_response(exc.message, exc.errors),
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(
        request: Request,
        exc: RequestValidationError,
    ) -> JSONResponse:
        errors = [
            {
                "field": ".".join(str(part) for part in error["loc"]),
                "message": error["msg"],
                "type": error["type"],
            }
            for error in exc.errors()
        ]
        return JSONResponse(
            status_code=422,
            content=error_response("Données de requête invalides", errors),
        )

    @app.exception_handler(HTTPException)
    async def http_exception_handler(
        request: Request,
        exc: HTTPException,
    ) -> JSONResponse:
        message = exc.detail if isinstance(exc.detail, str) else "Erreur HTTP"
        errors = {} if isinstance(exc.detail, str) else exc.detail
        return JSONResponse(
            status_code=exc.status_code,
            content=error_response(message, errors),
            headers=exc.headers,
        )

    @app.exception_handler(Exception)
    async def unexpected_exception_handler(
        request: Request,
        exc: Exception,
    ) -> JSONResponse:
        LOGGER.exception("Erreur non geree sur %s", request.url.path, exc_info=exc)
        return JSONResponse(
            status_code=500,
            content=error_response("Une erreur interne est survenue"),
        )
