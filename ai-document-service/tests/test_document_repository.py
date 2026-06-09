from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.core.database import Base
from app.models.document_analysis import DocumentAnalysis
from app.repositories.document_repository import DocumentRepository


def test_document_repository_crud_operations() -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)

    with Session(engine) as session:
        repository = DocumentRepository(session)

        created = repository.create(
            original_filename="invoice.pdf",
            stored_filename="550e8400-invoice.pdf",
            extracted_text="Invoice content",
            confidence_score=0.94,
        )

        assert created.id is not None
        assert created.status == "completed"
        assert created.created_at is not None
        assert repository.find_by_id(created.id) == created
        assert repository.find_all() == [created]


def test_document_analysis_defaults() -> None:
    analysis = DocumentAnalysis(
        original_filename="document.png",
        stored_filename="stored-document.png",
    )

    assert analysis.extracted_text is None
