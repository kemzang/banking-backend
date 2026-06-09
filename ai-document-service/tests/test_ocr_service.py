import asyncio
from io import BytesIO

import cv2
import numpy as np
import pytest
from fastapi import UploadFile
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.core.database import Base
from app.core.exceptions import AppException
from app.repositories.document_repository import DocumentRepository
from app.services.ocr_service import OcrService


def build_png() -> bytes:
    image = np.full((80, 240, 3), 255, dtype=np.uint8)
    cv2.putText(
        image,
        "Invoice 123",
        (10, 50),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (0, 0, 0),
        2,
    )
    success, encoded = cv2.imencode(".png", image)
    assert success
    return encoded.tobytes()


def test_extract_saves_file_and_analysis(tmp_path, monkeypatch) -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)

    monkeypatch.setattr(
        "app.services.ocr_service.pytesseract.image_to_string",
        lambda image: "Invoice 123",
    )

    with Session(engine) as session:
        repository = DocumentRepository(session)
        service = OcrService(repository, upload_dir=tmp_path)
        upload = UploadFile(
            filename="invoice.png",
            file=BytesIO(build_png()),
        )

        analysis = asyncio.run(service.extract(upload))

        assert analysis.original_filename == "invoice.png"
        assert analysis.extracted_text == "Invoice 123"
        assert analysis.confidence_score == 0.0
        assert analysis.status == "completed"
        assert (tmp_path / analysis.stored_filename).is_file()
        assert service.get_history() == [analysis]
        assert service.get_history_item(analysis.id) == analysis


def test_extract_rejects_unsupported_extension(tmp_path) -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)

    with Session(engine) as session:
        service = OcrService(
            DocumentRepository(session),
            upload_dir=tmp_path,
        )
        upload = UploadFile(
            filename="document.pdf",
            file=BytesIO(b"not an image"),
        )

        with pytest.raises(AppException) as exception:
            asyncio.run(service.extract(upload))

        assert exception.value.status_code == 400
        assert exception.value.message == "Extension de fichier non supportée"
        assert not tmp_path.exists() or list(tmp_path.iterdir()) == []


def test_get_history_item_raises_when_analysis_is_missing(tmp_path) -> None:
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(bind=engine)

    with Session(engine) as session:
        service = OcrService(
            DocumentRepository(session),
            upload_dir=tmp_path,
        )

        with pytest.raises(AppException) as exception:
            service.get_history_item(999)

        assert exception.value.status_code == 404
        assert exception.value.message == "Analyse OCR introuvable"
