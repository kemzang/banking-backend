import json
from datetime import datetime

from sqlalchemy import DateTime, Float, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class DocumentAnalysis(Base):
    """Persistent history of a document processing operation."""

    __tablename__ = "document_analyses"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        autoincrement=True,
    )
    original_filename: Mapped[str] = mapped_column(
        String(255),
        nullable=False,
    )
    stored_filename: Mapped[str] = mapped_column(
        String(255),
        nullable=False,
    )
    extracted_text: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
    )
    confidence_score: Mapped[float] = mapped_column(
        Float,
        nullable=False,
        default=0.0,
    )
    status: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        default="completed",
    )
    content_type: Mapped[str | None] = mapped_column(String(100), nullable=True)
    document_type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    extracted_fields_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    missing_fields_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    recommendation: Mapped[str | None] = mapped_column(String(50), nullable=True)
    message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        nullable=False,
        server_default=func.now(),
    )

    @property
    def extracted_fields(self) -> dict[str, str | None]:
        return json.loads(self.extracted_fields_json or "{}")

    @property
    def missing_fields(self) -> list[str]:
        return json.loads(self.missing_fields_json or "[]")

    @property
    def reliability_level(self) -> str:
        if self.confidence_score >= 70:
            return "HIGH"
        if self.confidence_score >= 40:
            return "MEDIUM"
        return "LOW"
