from typing import Any


def success_response(message: str, data: Any | None = None) -> dict[str, Any]:
    return {
        "status": "success",
        "message": message,
        "data": data if data is not None else {},
    }


def error_response(message: str, errors: Any | None = None) -> dict[str, Any]:
    return {
        "status": "error",
        "message": message,
        "errors": errors if errors is not None else {},
    }
