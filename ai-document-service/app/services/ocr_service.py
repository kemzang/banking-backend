from pathlib import Path

import pytesseract
from fastapi import UploadFile
from starlette.concurrency import run_in_threadpool

from app.config import settings
from app.core.exceptions import AppException
from app.models.document_analysis import DocumentAnalysis
from app.repositories.document_repository import DocumentRepository
from app.services.image_preprocessing_service import ImagePreprocessingService
from app.utils.file_utils import delete_file, save_upload_file


class OcrService:
    """Coordinate file storage, image processing, OCR and persistence."""

    def __init__(
        self,
        repository: DocumentRepository,
        preprocessing_service: ImagePreprocessingService | None = None,
        upload_dir: Path | None = None,
    ) -> None:
        self.repository = repository
        self.preprocessing_service = (
            preprocessing_service or ImagePreprocessingService()
        )
        self.upload_dir = upload_dir or settings.upload_dir

        if settings.tesseract_cmd:
            pytesseract.pytesseract.tesseract_cmd = settings.tesseract_cmd

    async def extract(self, file: UploadFile | None) -> DocumentAnalysis:
        saved_path: Path | None = None

        try:
            original_filename, stored_filename, saved_path = (
                await save_upload_file(file, self.upload_dir)
            )
            processed_image = await run_in_threadpool(
                self.preprocessing_service.preprocess,
                saved_path,
            )
            extracted_text = await run_in_threadpool(
                pytesseract.image_to_string,
                processed_image,
            )

            return self.repository.create(
                original_filename=original_filename,
                stored_filename=stored_filename,
                extracted_text=extracted_text.strip(),
                confidence_score=0.0,
                status="completed",
            )
        except AppException:
            if saved_path:
                delete_file(saved_path)
            raise
        except (
            pytesseract.TesseractError,
            pytesseract.TesseractNotFoundError,
        ) as exc:
            if saved_path:
                delete_file(saved_path)
            raise AppException(
                message="Une erreur est survenue pendant l'extraction OCR",
                status_code=500,
                errors={"detail": str(exc)},
            ) from exc
        except Exception as exc:
            if saved_path:
                delete_file(saved_path)
            raise AppException(
                message="Impossible de traiter le document",
                status_code=500,
                errors={"detail": str(exc)},
            ) from exc

    def get_history(self) -> list[DocumentAnalysis]:
        return self.repository.find_all()

    def get_history_item(self, analysis_id: int) -> DocumentAnalysis:
        analysis = self.repository.find_by_id(analysis_id)
        if analysis is None:
            raise AppException(
                message="Analyse OCR introuvable",
                status_code=404,
                errors={"id": analysis_id},
            )
        return analysis
