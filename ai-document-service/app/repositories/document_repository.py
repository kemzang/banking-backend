from sqlalchemy.orm import Session

from app.models.document import Document


class DocumentRepository:
    """Small persistence boundary ready for the future OCR service."""

    def __init__(self, db: Session) -> None:
        self.db = db

    def create(self, filename: str, extracted_text: str | None = None) -> Document:
        document = Document(
            filename=filename,
            extracted_text=extracted_text,
        )
        self.db.add(document)
        self.db.commit()
        self.db.refresh(document)
        return document

    def get_by_id(self, document_id: int) -> Document | None:
        return self.db.get(Document, document_id)
