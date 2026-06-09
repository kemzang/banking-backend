import os
from pathlib import Path

from dotenv import load_dotenv
from pydantic import BaseModel, ConfigDict


BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


def _as_bool(value: str) -> bool:
    return value.strip().lower() in {"1", "true", "yes", "on"}


class Settings(BaseModel):
    model_config = ConfigDict(frozen=True)

    app_name: str = os.getenv("APP_NAME", "Microservice Python - TP INF462")
    app_description: str = os.getenv(
        "APP_DESCRIPTION",
        "Microservice Python pour le traitement et l'analyse de documents",
    )
    app_version: str = os.getenv("APP_VERSION", "1.0.0")
    debug: bool = _as_bool(os.getenv("DEBUG", "false"))
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./storage/app.db")
    tesseract_cmd: str | None = os.getenv("TESSERACT_CMD") or None
    upload_dir: Path = BASE_DIR / "app" / "storage" / "uploads"


settings = Settings()
