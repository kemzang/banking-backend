from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.document_analysis import DocumentAnalysis


class DocumentRepository:
    """Persistence operations for document analysis history."""

    def __init__(self, db: Session) -> None:
        self.db = db

    def create(
        self,
        original_filename: str,
        stored_filename: str,
        extracted_text: str | None = None,
        confidence_score: float = 0.0,
        status: str = "completed",
    ) -> DocumentAnalysis:
        analysis = DocumentAnalysis(
            original_filename=original_filename,
            stored_filename=stored_filename,
            extracted_text=extracted_text,
            confidence_score=confidence_score,
            status=status,
        )
        try:
            self.db.add(analysis)
            self.db.commit()
            self.db.refresh(analysis)
        except Exception:
            self.db.rollback()
            raise
        return analysis

    def find_all(self) -> list[DocumentAnalysis]:
        statement = select(DocumentAnalysis).order_by(
            DocumentAnalysis.created_at.desc(),
            DocumentAnalysis.id.desc(),
        )
        return list(self.db.scalars(statement).all())

    def find_by_id(self, analysis_id: int) -> DocumentAnalysis | None:
        return self.db.get(DocumentAnalysis, analysis_id)
